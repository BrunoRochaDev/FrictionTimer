package com.brunorochamoura.friction_timer

import android.content.Context

class FrictionRuntimeStateStore(context: Context) {
  private val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

  fun isInCooldown(appId: String, nowMs: Long): Boolean =
    FrictionOverlayLogic.isCooldownActive(getCooldownUntil(appId), nowMs)

  fun setCooldownUntil(appId: String, untilMs: Long) {
    prefs.edit().putLong(cooldownKey(appId), untilMs).apply()
  }

  fun nextMessage(appId: String, messages: List<String>, fallback: String): String {
    val selection = FrictionOverlayLogic.selectMessage(
      messages = messages,
      nextIndex = prefs.getInt(messageIndexKey(appId), 0),
      fallback = fallback,
    )

    prefs.edit().putInt(messageIndexKey(appId), selection.nextIndex).apply()
    return selection.message
  }

  private fun getCooldownUntil(appId: String): Long = prefs.getLong(cooldownKey(appId), 0L)

  private fun cooldownKey(appId: String): String = "cooldown_until_ms:$appId"

  private fun messageIndexKey(appId: String): String = "next_message_index:$appId"

  companion object {
    private const val PREFS_NAME = "friction_runtime_state"
  }
}
