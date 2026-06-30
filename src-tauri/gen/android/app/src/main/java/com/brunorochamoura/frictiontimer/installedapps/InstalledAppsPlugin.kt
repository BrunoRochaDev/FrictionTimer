package com.brunorochamoura.frictiontimer.installedapps

import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import app.tauri.annotation.Command
import app.tauri.annotation.TauriPlugin
import app.tauri.plugin.Invoke
import app.tauri.plugin.Plugin
import java.util.Locale

data class InstalledAppResponse(
  val appId: String,
  val name: String,
)

@TauriPlugin
class InstalledAppsPlugin(private val activity: Activity) : Plugin(activity) {
  @Command
  fun listInstalledApps(invoke: Invoke) {
    try {
      val packageManager = activity.packageManager
      val launchIntent = Intent(Intent.ACTION_MAIN).apply {
        addCategory(Intent.CATEGORY_LAUNCHER)
      }

      val apps = queryLaunchableApps(packageManager, launchIntent)
        .asSequence()
        .map { resolveInfo ->
          val appId = resolveInfo.activityInfo.packageName
          val rawName = resolveInfo.loadLabel(packageManager)?.toString()?.trim().orEmpty()
          InstalledAppResponse(
            appId = appId,
            name = rawName.ifBlank { appId },
          )
        }
        .filter { app -> app.appId != activity.packageName }
        .distinctBy { app -> app.appId }
        .sortedWith(
          compareBy<InstalledAppResponse> { it.name.lowercase(Locale.ROOT) }
            .thenBy { it.appId },
        )
        .toList()

      invoke.resolveObject(apps)
    } catch (ex: Exception) {
      invoke.reject(ex.message, ex)
    }
  }

  @Suppress("DEPRECATION")
  private fun queryLaunchableApps(
    packageManager: PackageManager,
    launchIntent: Intent,
  ) = packageManager.queryIntentActivities(launchIntent, 0)
}
