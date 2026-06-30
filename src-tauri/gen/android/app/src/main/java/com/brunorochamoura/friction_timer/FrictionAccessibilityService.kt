package com.brunorochamoura.friction_timer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent

class FrictionAccessibilityService : AccessibilityService() {
  private val mainHandler = Handler(Looper.getMainLooper())

  private lateinit var configRepository: FrictionAppConfigRepository
  private lateinit var runtimeState: FrictionRuntimeStateStore
  private lateinit var overlayController: FrictionOverlayController

  private var launcherPackages: Set<String> = emptySet()
  private var pendingForegroundPackageHint: String? = null
  private val processForegroundRunnable = Runnable {
    processForegroundPackage(pendingForegroundPackageHint)
  }

  override fun onServiceConnected() {
    super.onServiceConnected()

    configRepository = FrictionAppConfigRepository(applicationContext)
    runtimeState = FrictionRuntimeStateStore(applicationContext)
    overlayController = FrictionOverlayController(
      context = applicationContext,
      windowManager = getSystemService(WINDOW_SERVICE) as WindowManager,
      onProceed = ::handleProceed,
      onCancel = ::handleCancel,
    )
    launcherPackages = resolveLauncherPackages()
  }

  override fun onAccessibilityEvent(event: AccessibilityEvent?) {
    val eventType = event?.eventType ?: return
    if (eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED &&
      eventType != AccessibilityEvent.TYPE_WINDOWS_CHANGED
    ) {
      return
    }

    val packageName = event.packageName?.toString()?.trim().orEmpty()
    if (!::overlayController.isInitialized) {
      return
    }

    if (!Settings.canDrawOverlays(this)) {
      overlayController.dismiss()
      return
    }

    scheduleForegroundProcessing(packageName)
  }

  override fun onInterrupt() {
    if (::overlayController.isInitialized) {
      overlayController.dismiss()
    }
  }

  override fun onDestroy() {
    mainHandler.removeCallbacks(processForegroundRunnable)
    if (::overlayController.isInitialized) {
      overlayController.dismiss()
    }
    super.onDestroy()
  }

  private fun handleProceed(config: FrictionAppConfig) {
    runtimeState.setCooldownUntil(
      config.appId,
      FrictionOverlayLogic.cooldownExpiry(System.currentTimeMillis(), config.durationSeconds),
    )
    overlayController.dismiss()
  }

  private fun handleCancel(config: FrictionAppConfig) {
    overlayController.dismiss()
    performGlobalAction(GLOBAL_ACTION_HOME)
  }

  private fun scheduleForegroundProcessing(packageHint: String) {
    pendingForegroundPackageHint = packageHint.ifBlank { null }
    mainHandler.removeCallbacks(processForegroundRunnable)
    mainHandler.postDelayed(processForegroundRunnable, FOREGROUND_SETTLE_DELAY_MS)
  }

  private fun processForegroundPackage(packageHint: String?) {
    val activeOverlayPackage = overlayController.activePackage
    val resolvedPackage = resolveForegroundPackage(packageHint)
    if (activeOverlayPackage != null &&
      resolvedPackage != null &&
      resolvedPackage != activeOverlayPackage
    ) {
      overlayController.dismiss()
    }

    if (resolvedPackage == null || isIgnoredPackage(resolvedPackage)) {
      return
    }

    if (overlayController.isShowingFor(resolvedPackage)) {
      return
    }

    val config = configRepository.findByAppId(resolvedPackage) ?: return
    val nowMs = System.currentTimeMillis()
    if (runtimeState.isInCooldown(resolvedPackage, nowMs)) {
      return
    }

    val message = runtimeState.nextMessage(
      appId = resolvedPackage,
      messages = config.messages,
      fallback = getString(R.string.friction_overlay_fallback_message),
    )

    overlayController.show(config, message)
  }

  private fun resolveForegroundPackage(packageHint: String?): String? {
    val rootPackage = normalizePackageCandidate(rootInActiveWindow?.packageName?.toString())
    if (rootPackage != null) {
      return rootPackage
    }

    return normalizePackageCandidate(packageHint)
  }

  private fun resolveLauncherPackages(): Set<String> {
    val homeIntent = Intent(Intent.ACTION_MAIN).apply {
      addCategory(Intent.CATEGORY_HOME)
    }

    return packageManager
      .queryIntentActivities(homeIntent, 0)
      .mapNotNull { it.activityInfo?.packageName }
      .toSet()
  }

  private fun isIgnoredPackage(packageName: String): Boolean {
    return packageName == applicationContext.packageName ||
      isTransientSystemPackage(packageName) ||
      launcherPackages.contains(packageName)
  }

  private fun normalizePackageCandidate(packageName: String?): String? {
    val normalized = packageName?.trim().orEmpty()
    return normalized
      .ifBlank { null }
      ?.takeUnless { it == applicationContext.packageName }
      ?.takeUnless(::isTransientSystemPackage)
  }

  private fun isTransientSystemPackage(packageName: String): Boolean {
    return packageName == "android" || packageName == "com.android.systemui"
  }

  companion object {
    private const val FOREGROUND_SETTLE_DELAY_MS = 180L
  }
}
