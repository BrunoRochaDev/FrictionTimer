package com.brunorochamoura.frictiontimer.ui

import android.content.SharedPreferences
import android.util.Log

fun saveWaitTime(prefs: SharedPreferences, pkg: String, minutes: Long, seconds: Long) {
    val totalMs = (minutes * 60_000 + seconds * 1000).coerceAtLeast(1000L)
    prefs.edit()
        .putLong("${pkg}_wait_minutes", minutes)
        .putLong("${pkg}_wait_seconds", seconds)
        .putLong("${pkg}_wait_time_ms", totalMs)
        .apply()
    Log.d("SettingsScreen", "Saved wait time for $pkg: ${minutes}m ${seconds}s")
}

fun saveCooldownTime(prefs: SharedPreferences, pkg: String, minutes: Long, seconds: Long) {
    val totalMs = (minutes * 60_000 + seconds * 1000).coerceAtLeast(1000L)
    prefs.edit()
        .putLong("${pkg}_cooldown_minutes", minutes)
        .putLong("${pkg}_cooldown_seconds", seconds)
        .putLong("${pkg}_cooldown_time_ms", totalMs)
        .apply()
    Log.d("SettingsScreen", "Saved cooldown time for $pkg: ${minutes}m ${seconds}s")
}

fun saveMessages(prefs: SharedPreferences, pkg: String, messages: List<String>) {
    prefs.edit()
        .putStringSet("${pkg}_motivational_messages", messages.toSet())
        .apply()
    Log.d("SettingsScreen", "Saved motivational messages for $pkg: $messages")
}
