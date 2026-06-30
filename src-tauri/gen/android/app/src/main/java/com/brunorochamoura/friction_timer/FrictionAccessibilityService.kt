package com.brunorochamoura.friction_timer

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.WindowManager
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityWindowInfo

class FrictionAccessibilityService : AccessibilityService() {
  private val mainHandler = Handler(Looper.getMainLooper())

  private lateinit var configRepository: FrictionAppConfigRepository
  private lateinit var runtimeState: FrictionRuntimeStateStore
  private lateinit var overlayController: FrictionOverlayController
  private val foregroundSessionState = FrictionForegroundSessionStateMachine()

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
      dismissOverlay("overlay_permission_revoked")
      return
    }

    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "Accessibility event=${eventTypeName(eventType)} packageHint=${packageName.ifBlank { "<none>" }} activeOverlay=${foregroundSessionState.activeOverlayPackage}",
      )
    }
    scheduleForegroundProcessing(packageName)
  }

  override fun onInterrupt() {
    if (::overlayController.isInitialized) {
      dismissOverlay("service_interrupted")
    }
  }

  override fun onDestroy() {
    mainHandler.removeCallbacks(processForegroundRunnable)
    if (::overlayController.isInitialized) {
      dismissOverlay("service_destroyed")
    }
    super.onDestroy()
  }

  private fun handleProceed(config: FrictionAppConfig) {
    runtimeState.setCooldownUntil(
      config.appId,
      FrictionOverlayLogic.cooldownExpiry(System.currentTimeMillis(), config.durationSeconds),
    )
    dismissOverlay("user_proceeded:${config.appId}")
  }

  private fun handleCancel(config: FrictionAppConfig) {
    dismissOverlay("user_cancelled:${config.appId}")
    performGlobalAction(GLOBAL_ACTION_HOME)
  }

  private fun scheduleForegroundProcessing(packageHint: String) {
    pendingForegroundPackageHint = packageHint.ifBlank { null }
    mainHandler.removeCallbacks(processForegroundRunnable)
    mainHandler.postDelayed(processForegroundRunnable, FOREGROUND_SETTLE_DELAY_MS)
  }

  private fun processForegroundPackage(packageHint: String?) {
    val resolvedPackage = resolveForegroundPackage(packageHint)
    if (BuildConfig.DEBUG) {
      Log.d(
        TAG,
        "Processing foreground packageHint=${packageHint ?: "<none>"} resolved=${resolvedPackage ?: "<none>"} root=${normalizePackageLabel(rootInActiveWindow?.packageName?.toString())} state=${foregroundSessionState.debugState()}",
      )
    }

    val transition = foregroundSessionState.onForegroundObserved(resolvedPackage)
    if (transition.dismissActiveOverlay) {
      dismissOverlay("confirmed_foreground_exit:${resolvedPackage ?: "<none>"}")
    }

    if (transition.shouldRecheckForeground) {
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "Holding overlay until foreground exit is confirmed")
      }
      scheduleForegroundProcessing("")
    }

    maybeShowOverlayFor(transition.packageToEvaluateForOverlay)
  }

  private fun maybeShowOverlayFor(packageName: String?) {
    if (packageName == null || isIgnoredPackage(packageName)) {
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

    if (overlayController.show(config, message)) {
      foregroundSessionState.onOverlayShown(config.appId)
      if (BuildConfig.DEBUG) {
        Log.d(TAG, "Overlay shown for ${config.appId}")
      }
    }
  }

  private fun resolveForegroundPackage(packageHint: String?): String? {
    resolveForegroundPackageFromWindows()?.let { return it }

    val rootPackage = normalizePackageCandidate(rootInActiveWindow?.packageName?.toString())
    if (rootPackage != null) {
      return rootPackage
    }

    return normalizePackageCandidate(packageHint)
  }

  private fun resolveForegroundPackageFromWindows(): String? {
    var bestPackage: String? = null
    var bestScore = Int.MIN_VALUE

    for (window in windows) {
      val packageName = normalizePackageCandidate(window.root?.packageName?.toString()) ?: continue
      val score = foregroundWindowScore(window)
      if (score > bestScore) {
        bestScore = score
        bestPackage = packageName
      }
    }

    return bestPackage
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

  private fun dismissOverlay(reason: String) {
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Dismissing overlay reason=$reason activeOverlay=${foregroundSessionState.activeOverlayPackage}")
    }
    overlayController.dismiss()
    foregroundSessionState.onOverlayDismissed()
  }

  private fun foregroundWindowScore(window: AccessibilityWindowInfo): Int {
    var score = when (window.type) {
      AccessibilityWindowInfo.TYPE_APPLICATION -> 100
      AccessibilityWindowInfo.TYPE_INPUT_METHOD -> 50
      AccessibilityWindowInfo.TYPE_SYSTEM -> 25
      AccessibilityWindowInfo.TYPE_ACCESSIBILITY_OVERLAY -> 0
      else -> 10
    }

    if (window.isActive) {
      score += 10
    }
    if (window.isFocused) {
      score += 5
    }

    return score
  }

  private fun normalizePackageLabel(packageName: String?): String =
    packageName?.trim().orEmpty().ifBlank { "<none>" }

  private fun eventTypeName(eventType: Int): String = when (eventType) {
    AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED -> "TYPE_WINDOW_STATE_CHANGED"
    AccessibilityEvent.TYPE_WINDOWS_CHANGED -> "TYPE_WINDOWS_CHANGED"
    else -> eventType.toString()
  }

  companion object {
    private const val FOREGROUND_SETTLE_DELAY_MS = 180L
    private const val TAG = "FrictionAccessSvc"
  }
}
