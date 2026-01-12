package com.brunorochamoura.frictiontimer

import android.content.Context
import android.content.Intent
import android.graphics.PixelFormat
import android.os.CountDownTimer
import android.provider.Settings
import android.util.Log
import android.view.Gravity
import android.view.LayoutInflater
import android.view.View
import android.view.WindowManager
import android.widget.Button
import android.widget.TextView

class FrictionOverlay(
    private val context: Context,
    private val targetAppPackage: String,
    private val onProceed: () -> Unit
) {

    private val prefs = context.getSharedPreferences("friction_timer", Context.MODE_PRIVATE)
    private val WAIT_MS: Long
        get() = prefs.getLong("${targetAppPackage}_wait_time_ms", 30000L)

    companion object {
        private const val TAG = "FrictionOverlay"
        private val DEFAULT_MESSAGES = listOf("Pause. Is this intentional?")
    }

    private val wm = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: View? = null
    private var timer: CountDownTimer? = null
    private var showing = false

    fun isShowing(): Boolean = showing

    fun show() {
        if (showing) {
            Log.d(TAG, "Overlay already showing for $targetAppPackage, skipping show()")
            return
        }

        if (!Settings.canDrawOverlays(context)) {
            Log.d(TAG, "Cannot draw overlays, permission missing")
            return
        }

        Log.d(TAG, "Showing overlay for $targetAppPackage")
        view = LayoutInflater.from(context).inflate(R.layout.overlay_layout, null)

        val waitButton = view!!.findViewById<Button>(R.id.waitButton)
        val cancelButton = view!!.findViewById<Button>(R.id.cancelButton)
        val messageText = view!!.findViewById<TextView>(R.id.messageText)

        val messages = prefs.getStringSet(
            "${targetAppPackage}_motivational_messages",
            DEFAULT_MESSAGES.toSet()
        )?.toList() ?: DEFAULT_MESSAGES
        messageText.text = messages.random()

        cancelButton.setOnClickListener {
            Log.d(TAG, "Cancel button clicked for $targetAppPackage")
            remove()
            goHome()
        }

        waitButton.isEnabled = false
        timer = object : CountDownTimer(WAIT_MS, 1_000) {
            override fun onTick(ms: Long) {
                val secondsLeft = ms / 1000
                waitButton.text = "Wait ${secondsLeft}s"
                Log.d(TAG, "Countdown tick for $targetAppPackage: $secondsLeft s remaining")
            }

            override fun onFinish() {
                waitButton.text = "Proceed"
                waitButton.isEnabled = true
                Log.d(TAG, "Countdown finished for $targetAppPackage, Proceed enabled")
            }
        }.start()

        waitButton.setOnClickListener {
            Log.d(TAG, "Proceed button clicked for $targetAppPackage")
            remove()
            onProceed()
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
            WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.CENTER
        }

        wm.addView(view, params)
        showing = true
        Log.d(TAG, "Overlay added to WindowManager for $targetAppPackage")
    }

    fun remove() {
        if (!showing) {
            Log.d(TAG, "Overlay not showing for $targetAppPackage, skip remove()")
            return
        }

        Log.d(TAG, "Removing overlay for $targetAppPackage")
        timer?.cancel()
        timer = null

        view?.let {
            try {
                wm.removeView(it)
                Log.d(TAG, "Overlay view removed from WindowManager for $targetAppPackage")
            } catch (e: Exception) {
                Log.e(TAG, "Error removing overlay view for $targetAppPackage: ${e.message}")
            }
        }

        view = null
        showing = false
    }

    private fun goHome() {
        Log.d(TAG, "Returning to home screen")
        val intent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        context.startActivity(intent)
    }
}
