package com.brunorochamoura.frictiontimer

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import com.brunorochamoura.frictiontimer.ui.SettingsScreen
import com.brunorochamoura.frictiontimer.ui.theme.FrictionTimerTheme

class MainActivity : ComponentActivity() {

    companion object {
        private const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            FrictionTimerTheme {
                // Launch the SettingsScreen composable from a separate file
                SettingsScreen(context = this)
            }
        }
    }

    fun requestOverlayPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (!Settings.canDrawOverlays(this)) {
                val intent = Intent(
                    Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                    Uri.parse("package:$packageName")
                )
                startActivity(intent)
                Log.d(TAG, "Requesting overlay permission")
            } else {
                Log.d(TAG, "Overlay permission already granted")
            }
        }
    }

    fun openAccessibilitySettings() {
        startActivity(Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        Log.d(TAG, "Opening Accessibility settings")
    }
}
