package com.brunorochamoura.frictiontimer.ui

import android.content.SharedPreferences
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlin.math.max

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppSettingsPage(
    prefs: SharedPreferences,
    pkg: String,
    onRemove: () -> Unit
) {
    // ---------------------------------------------------------------------
    // Canonical persisted state (milliseconds only)
    // ---------------------------------------------------------------------

    val waitMsKey = "${pkg}_wait_time_ms"
    val cooldownMsKey = "${pkg}_cooldown_time_ms"

    var waitTimeMs by remember {
        mutableStateOf(prefs.getLong(waitMsKey, 30_000L))
    }

    var cooldownTimeMs by remember {
        mutableStateOf(prefs.getLong(cooldownMsKey, 300_000L))
    }

    // ---------------------------------------------------------------------
    // UI-derived state (minutes / seconds)
    // ---------------------------------------------------------------------

    var waitMinutes by remember { mutableStateOf(waitTimeMs / 60_000) }
    var waitSeconds by remember { mutableStateOf((waitTimeMs / 1_000) % 60) }

    var cooldownMinutes by remember { mutableStateOf(cooldownTimeMs / 60_000) }
    var cooldownSeconds by remember { mutableStateOf((cooldownTimeMs / 1_000) % 60) }

    var showDeleteConfirm by remember { mutableStateOf(false) }

    // ---------------------------------------------------------------------
    // Messages
    // ---------------------------------------------------------------------

    var messages by remember {
        mutableStateOf(
            prefs.getStringSet(
                "${pkg}_motivational_messages",
                setOf("Pause. Is this intentional?")
            )!!.toList()
        )
    }

    var newMessage by remember { mutableStateOf("") }

    // ---------------------------------------------------------------------
    // Helpers
    // ---------------------------------------------------------------------

    fun persistWaitTime(minutes: Long, seconds: Long) {
        val totalMs = max(1, (minutes * 60 + seconds)) * 1_000
        waitTimeMs = totalMs
        prefs.edit().putLong(waitMsKey, totalMs).apply()
    }

    fun persistCooldownTime(minutes: Long, seconds: Long) {
        val totalMs = max(1, (minutes * 60 + seconds)) * 1_000
        cooldownTimeMs = totalMs
        prefs.edit().putLong(cooldownMsKey, totalMs).apply()
    }

    fun saveMessages(updated: List<String>) {
        prefs.edit()
            .putStringSet("${pkg}_motivational_messages", updated.toSet())
            .apply()
    }

    // ---------------------------------------------------------------------
    // UI
    // ---------------------------------------------------------------------

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.secondary)
            .verticalScroll(rememberScrollState())
            .padding(12.dp)
    ) {

        // Header
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = pkg,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondary,
                modifier = Modifier.weight(1f)
            )

            IconButton(onClick = { showDeleteConfirm = true }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Remove app",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }

        Spacer(Modifier.height(20.dp))

        // Timing
        Text(
            text = "Timing",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(Modifier.padding(12.dp)) {

                TimeInputRow(
                    label = "Wait before proceed",
                    minutes = waitMinutes,
                    seconds = waitSeconds
                ) { m, s ->
                    waitMinutes = m
                    waitSeconds = s
                    persistWaitTime(m, s)
                }

                Spacer(Modifier.height(8.dp))

                TimeInputRow(
                    label = "Cooldown after proceed",
                    minutes = cooldownMinutes,
                    seconds = cooldownSeconds
                ) { m, s ->
                    cooldownMinutes = m
                    cooldownSeconds = s
                    persistCooldownTime(m, s)
                }

                Spacer(Modifier.height(16.dp))

                TextButton(
                    onClick = { prefs.edit().remove("${pkg}_cooldown_end").apply() },
                    colors = ButtonDefaults.textButtonColors(
                        containerColor = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ),
                    modifier = Modifier.align(Alignment.CenterHorizontally)
                ) {
                    Text("Reset cooldown")
                }
            }
        }

        Spacer(Modifier.height(20.dp))

        // Messages
        Text(
            text = "Warning Messages",
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSecondary
        )

        Spacer(Modifier.height(8.dp))

        Surface(
            shape = MaterialTheme.shapes.medium,
            color = MaterialTheme.colorScheme.background,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            LazyColumn(
                modifier = Modifier
                    .heightIn(max = 160.dp)
                    .padding(8.dp)
            ) {
                itemsIndexed(messages) { index, msg ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )

                        IconButton(onClick = {
                            val updated = messages.toMutableList()
                            updated.removeAt(index)
                            messages = updated
                            saveMessages(updated)
                        }) {
                            Icon(
                                imageVector = Icons.Default.Delete,
                                contentDescription = "Delete message",
                                tint = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }

        Spacer(Modifier.height(12.dp))

        OutlinedTextField(
            value = newMessage,
            onValueChange = { newMessage = it },
            label = { Text("New message") },
            singleLine = true,
            colors = TextFieldDefaults.outlinedTextFieldColors(
                focusedBorderColor = MaterialTheme.colorScheme.onBackground,
                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground,
                cursorColor = MaterialTheme.colorScheme.onBackground,
                focusedLabelColor = MaterialTheme.colorScheme.onBackground,
                unfocusedLabelColor = MaterialTheme.colorScheme.onBackground
            ),
            textStyle = TextStyle(color = MaterialTheme.colorScheme.background),
            modifier = Modifier.fillMaxWidth()
        )

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = {
                if (newMessage.isNotBlank()) {
                    val updated = messages + newMessage.trim()
                    messages = updated
                    saveMessages(updated)
                    newMessage = ""
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = Modifier.align(Alignment.End)
        ) {
            Text("Add message")
        }
    }

    // Delete confirmation
    if (showDeleteConfirm) {
        AlertDialog(
            containerColor = MaterialTheme.colorScheme.background,
            titleContentColor = MaterialTheme.colorScheme.onBackground,
            textContentColor = MaterialTheme.colorScheme.onBackground,
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Remove app") },
            text = { Text("Are you sure you want to remove this app?") },
            confirmButton = {
                TextButton(onClick = {
                    showDeleteConfirm = false
                    onRemove()
                }) {
                    Text(text = "Remove", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) {
                    Text(text = "Cancel", color = MaterialTheme.colorScheme.onBackground)
                }
            }
        )
    }
}