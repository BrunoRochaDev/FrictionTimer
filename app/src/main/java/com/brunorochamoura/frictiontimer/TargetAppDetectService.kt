package com.brunorochamoura.frictiontimer

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.content.SharedPreferences
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.accessibility.AccessibilityEvent

class TargetAppDetectService : AccessibilityService() {

    companion object {
        private const val TAG = "TargetAppDetectService"
        private const val DEFAULT_COOLDOWN_MS = 300_000L
    }

    private val prefs: SharedPreferences by lazy {
        getSharedPreferences("friction_timer", Context.MODE_PRIVATE)
    }

    private val handler = Handler(Looper.getMainLooper())

    private var activeTargetApp: String? = null

    private var overlay: FrictionOverlay? = null

    private val cooldownRunnables = mutableMapOf<String, Runnable>()

    override fun onServiceConnected() {
        super.onServiceConnected()
        Log.d(TAG, "Accessibility service connected")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event == null) return
        if (event.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return

        val pkg = event.packageName?.toString() ?: return
        val targetApps = getTargetApps()

        if (targetApps.isEmpty()) return

        Log.d(TAG, "Window changed: $pkg")

        when {
            // Window changed to target app
            pkg in targetApps && activeTargetApp != pkg -> {
                Log.d(TAG, "Entered target app: $pkg")
                onEnterTargetApp(pkg)
            }

            // Left current target app
            activeTargetApp != null && pkg != activeTargetApp && pkg != packageName -> {
                Log.d(TAG, "Exited target app: $activeTargetApp")
                onExitTargetApp(activeTargetApp!!)
            }
        }
    }

    override fun onInterrupt() {
        Log.d(TAG, "Accessibility service interrupted")
    }

    private fun onEnterTargetApp(pkg: String) {
        // Clean up any previous app state
        activeTargetApp?.let { onExitTargetApp(it) }

        activeTargetApp = pkg

        if (!isInCooldown(pkg)) {
            showOverlay(pkg)
        } else {
            Log.d(TAG, "Cooldown active for $pkg, overlay suppressed")
        }
    }

    private fun onExitTargetApp(pkg: String) {
        removeOverlay()
        activeTargetApp = null
        // NOTE: we intentionally do NOT clear cooldown here
        // cooldown is per-app and should persist
    }

    private fun getTargetApps(): Set<String> {
        return prefs.getStringSet("target_apps", emptySet()) ?: emptySet()
    }

    private fun showOverlay(pkg: String) {
        if (overlay?.isShowing() == true) {
            Log.d(TAG, "Overlay already showing, skipping")
            return
        }

        Log.d(TAG, "Showing overlay for $pkg")

        overlay = FrictionOverlay(this, pkg) {
            Log.d(TAG, "Proceed clicked for $pkg")
            startCooldown(pkg)
        }

        overlay!!.show()
    }

    private fun removeOverlay() {
        overlay?.remove()
        overlay = null
    }
    
    private fun startCooldown(pkg: String) {
        val cooldownMs = getCooldownMs(pkg)
        val endTime = System.currentTimeMillis() + cooldownMs

        prefs.edit()
            .putLong("${pkg}_cooldown_end", endTime)
            .apply()

        Log.d(TAG, "Cooldown started for $pkg until $endTime")

        removeOverlay()

        // Cancel any existing runnable for this app
        cooldownRunnables[pkg]?.let {
            handler.removeCallbacks(it)
        }

        val runnable = Runnable {
            // Only re-show if:
            // 1) This app is still active
            // 2) Cooldown truly ended
            if (activeTargetApp == pkg && !isInCooldown(pkg)) {
                Log.d(TAG, "Cooldown finished, re-showing overlay for $pkg")
                showOverlay(pkg)
            }
        }

        cooldownRunnables[pkg] = runnable
        handler.postDelayed(runnable, cooldownMs)
    }

    private fun isInCooldown(pkg: String): Boolean {
        val endTime = prefs.getLong("${pkg}_cooldown_end", 0L)
        return System.currentTimeMillis() < endTime
    }

    private fun getCooldownMs(pkg: String): Long {
        return prefs.getLong("${pkg}_cooldown_time_ms", DEFAULT_COOLDOWN_MS)
    }
}