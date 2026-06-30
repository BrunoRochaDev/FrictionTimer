package com.brunorochamoura.frictiontimer.permissions

import android.app.Activity
import android.content.ComponentName
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import app.tauri.annotation.Command
import app.tauri.annotation.InvokeArg
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.Plugin
import com.brunorochamoura.frictiontimer.FrictionAccessibilityService

@InvokeArg
class OpenSettingsArgs {
  lateinit var kind: String
}

data class ServiceStatusResponse(
  val overlay: Boolean,
  val accessibility: Boolean,
)

@TauriPlugin
class PermissionsPlugin(private val activity: Activity) : Plugin(activity) {
  @Command
  fun getStatus(invoke: Invoke) {
    try {
      invoke.resolveObject(
        ServiceStatusResponse(
          overlay = Settings.canDrawOverlays(activity),
          accessibility = isAccessibilityServiceEnabled(),
        ),
      )
    } catch (ex: Exception) {
      invoke.reject(ex.message, ex)
    }
  }

  @Command
  fun openSettings(invoke: Invoke) {
    try {
      val args = invoke.parseArgs(OpenSettingsArgs::class.java)
      val intent = when (args.kind) {
        "overlay" -> Intent(
          Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
          Uri.parse("package:${activity.packageName}"),
        )
        "accessibility" -> Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        else -> throw IllegalArgumentException("Unknown settings target: ${args.kind}")
      }

      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
      activity.startActivity(intent)
      invoke.resolve()
    } catch (ex: Exception) {
      invoke.reject(ex.message, ex)
    }
  }

  private fun isAccessibilityServiceEnabled(): Boolean {
    val accessibilityEnabled = Settings.Secure.getInt(
      activity.contentResolver,
      Settings.Secure.ACCESSIBILITY_ENABLED,
      0,
    ) == 1

    if (!accessibilityEnabled) {
      return false
    }

    val enabledServices = Settings.Secure.getString(
      activity.contentResolver,
      Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES,
    ) ?: return false

    val expectedService = ComponentName(
      activity,
      FrictionAccessibilityService::class.java,
    ).flattenToString()

    return enabledServices
      .split(':')
      .any { serviceName -> serviceName.equals(expectedService, ignoreCase = true) }
  }
}
