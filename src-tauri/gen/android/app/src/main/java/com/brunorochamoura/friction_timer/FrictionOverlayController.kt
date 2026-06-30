package com.brunorochamoura.friction_timer

import android.content.Context
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class FrictionOverlayController(
  private val context: Context,
  private val windowManager: WindowManager,
  private val onProceed: (FrictionAppConfig) -> Unit,
  private val onCancel: (FrictionAppConfig) -> Unit,
) {
  private var overlayView: View? = null
  private var countDownTimer: CountDownTimer? = null
  private var activeConfig: FrictionAppConfig? = null

  val activePackage: String?
    get() = activeConfig?.appId

  fun isShowingFor(packageName: String): Boolean =
    activeConfig?.appId == packageName && overlayView != null

  fun show(config: FrictionAppConfig, message: String): Boolean {
    dismiss()

    val themedContext = ContextThemeWrapper(context, R.style.Theme_friction_timer)
    val view = LayoutInflater.from(themedContext)
      .inflate(R.layout.friction_overlay, null, false)
    val messageView = view.findViewById<TextView>(R.id.friction_overlay_message)
    val cancelButton = view.findViewById<Button>(R.id.friction_overlay_cancel)
    val proceedButton = view.findViewById<Button>(R.id.friction_overlay_proceed)

    messageView.text = message
    cancelButton.setOnClickListener { onCancel(config) }
    proceedButton.setOnClickListener {
      if (proceedButton.isEnabled) {
        onProceed(config)
      }
    }

    updateCountdownButton(proceedButton, config.waitSeconds)

    val params = WindowManager.LayoutParams(
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.MATCH_PARENT,
      WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
        WindowManager.LayoutParams.FLAG_LAYOUT_INSET_DECOR,
      PixelFormat.TRANSLUCENT,
    ).apply {
      gravity = Gravity.CENTER
    }

    try {
      windowManager.addView(view, params)
    } catch (ex: Exception) {
      Log.w(TAG, "Failed to show friction overlay", ex)
      return false
    }

    overlayView = view
    activeConfig = config
    if (BuildConfig.DEBUG) {
      Log.d(TAG, "Showing overlay for ${config.appId}")
    }
    startCountdown(proceedButton, config.waitSeconds)
    return true
  }

  fun dismiss() {
    if (BuildConfig.DEBUG && (overlayView != null || activeConfig != null)) {
      Log.d(TAG, "Dismissing overlay for ${activeConfig?.appId}")
    }
    countDownTimer?.cancel()
    countDownTimer = null
    activeConfig = null

    overlayView?.let { view ->
      runCatching { windowManager.removeViewImmediate(view) }
        .onFailure { ex -> Log.w(TAG, "Failed to remove friction overlay", ex) }
    }
    overlayView = null
  }

  private fun startCountdown(button: Button, waitSeconds: Long) {
    if (waitSeconds <= 0L) {
      unlockProceedButton(button)
      return
    }

    countDownTimer = object : CountDownTimer(waitSeconds * 1000L, 1000L) {
      override fun onTick(millisUntilFinished: Long) {
        updateCountdownButton(button, FrictionOverlayLogic.secondsRemaining(millisUntilFinished))
      }

      override fun onFinish() {
        unlockProceedButton(button)
      }
    }.also { timer ->
      timer.start()
    }
  }

  private fun updateCountdownButton(button: Button, remainingSeconds: Long) {
    button.isEnabled = false
    button.text = context.getString(R.string.friction_overlay_wait_seconds, remainingSeconds)
  }

  private fun unlockProceedButton(button: Button) {
    button.isEnabled = true
    button.text = context.getString(R.string.friction_overlay_proceed)
  }

  companion object {
    private const val TAG = "FrictionOverlayCtrl"
  }
}
