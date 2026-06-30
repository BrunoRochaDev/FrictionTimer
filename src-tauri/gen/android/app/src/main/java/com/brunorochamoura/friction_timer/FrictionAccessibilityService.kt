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

  private var currentForegroundPackage: String? = null
  private var launcherPackages: Set<String> = emptySet()

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
    if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) {
      return
    }

    val packageName = event.packageName?.toString()?.trim().orEmpty()
    if (packageName.isBlank()) {
      return
    }

    if (!isIgnoredPackage(packageName)) {
      currentForegroundPackage = packageName
    }

    if (!::overlayController.isInitialized) {
      return
    }

    val activeOverlayPackage = overlayController.activePackage
    if (activeOverlayPackage != null &&
      packageName != activeOverlayPackage &&
      !isIgnoredPackage(packageName)
    ) {
      overlayController.dismiss()
    }

    if (packageName == applicationContext.packageName || isIgnoredPackage(packageName)) {
      return
    }

    if (!Settings.canDrawOverlays(this)) {
      overlayController.dismiss()
      return
    }

    if (overlayController.isShowingFor(packageName)) {
      return
    }

    val config = configRepository.findByAppId(packageName) ?: return
    val nowMs = System.currentTimeMillis()
    if (runtimeState.isInCooldown(packageName, nowMs)) {
      return
    }

    val message = runtimeState.nextMessage(
      appId = packageName,
      messages = config.messages,
      fallback = getString(R.string.friction_overlay_fallback_message),
    )

    overlayController.show(config, message)
  }

  override fun onInterrupt() {
    if (::overlayController.isInitialized) {
      overlayController.dismiss()
    }
  }

  override fun onDestroy() {
    mainHandler.removeCallbacksAndMessages(null)
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
    performGlobalAction(GLOBAL_ACTION_BACK)
    verifyAppClosedOrGoHome(config.appId, HOME_FALLBACK_ATTEMPTS)
  }

  private fun verifyAppClosedOrGoHome(targetPackage: String, attemptsRemaining: Int) {
    mainHandler.postDelayed({
      val activePackage = resolveActivePackage()?.takeUnless(::isTransientSystemPackage)
      when {
        activePackage == targetPackage && attemptsRemaining <= 1 -> {
          performGlobalAction(GLOBAL_ACTION_HOME)
        }
        activePackage == targetPackage -> {
          verifyAppClosedOrGoHome(targetPackage, attemptsRemaining - 1)
        }
        activePackage == null && attemptsRemaining > 1 -> {
          verifyAppClosedOrGoHome(targetPackage, attemptsRemaining - 1)
        }
      }
    }, HOME_FALLBACK_DELAY_MS)
  }

  private fun resolveActivePackage(): String? {
    val rootPackage = rootInActiveWindow?.packageName?.toString()?.trim()
    if (!rootPackage.isNullOrBlank()) {
      return rootPackage
    }

    return currentForegroundPackage
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

  private fun isTransientSystemPackage(packageName: String): Boolean {
    return packageName == "android" || packageName == "com.android.systemui"
  }

  companion object {
    private const val HOME_FALLBACK_DELAY_MS = 250L
    private const val HOME_FALLBACK_ATTEMPTS = 3
  }
}
