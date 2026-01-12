package com.brunorochamoura.frictiontimer.ui

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.brunorochamoura.frictiontimer.MainActivity

@Composable
fun SettingsScreen(context: Context) {
    val prefs: SharedPreferences =
        context.getSharedPreferences("friction_timer", Context.MODE_PRIVATE)

    var targetApps by remember {
        mutableStateOf(
            prefs.getStringSet("target_apps", emptySet())!!.toMutableList()
        )
    }

    var newAppPackage by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            "Friction Timer Settings",
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(16.dp))

        Button(
            onClick = { (context as MainActivity).requestOverlayPermission() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Enable Overlay")
        }

        Spacer(Modifier.height(8.dp))

        Button(
            onClick = { (context as MainActivity).openAccessibilitySettings() },
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            )
        ) {
            Text("Enable Accessibility Service")
        }

        Spacer(Modifier.height(24.dp))

        Text(
            "Add Target App",
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(8.dp))

        Row(verticalAlignment = Alignment.CenterVertically) {
            BasicTextField(
                value = newAppPackage,
                onValueChange = { newAppPackage = it },
                singleLine = true,
                modifier = Modifier
                    .weight(1f)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(8.dp),
                textStyle = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.onSecondary)
            )
            Spacer(Modifier.width(8.dp))
            Button(
                onClick = {
                    val pkg = newAppPackage.trim()
                    if (pkg.isNotEmpty() && !targetApps.contains(pkg)) {
                        targetApps = (targetApps + pkg).toMutableList()
                        prefs.edit()
                            .putStringSet("target_apps", targetApps.toSet())
                            .apply()
                        Log.d("SettingsScreen", "Added target app: $pkg")
                    }
                    newAppPackage = ""
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                )
            ) {
                Text("Add")
            }
        }

        Spacer(Modifier.height(24.dp))

        if (targetApps.isEmpty()) {
            Text(
                "No target apps added",
                color = MaterialTheme.colorScheme.onBackground
            )
        } else {
            Text(
                "Target Apps",
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
            Spacer(Modifier.height(8.dp))

            LazyRow {
                items(targetApps) { pkg ->
                    Card(
                        modifier = Modifier
                            .width(260.dp)
                            .height(420.dp)
                            .padding(8.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surface
                        )
                    ) {
                        AppSettingsPage(
                            prefs = prefs,
                            pkg = pkg,
                            onRemove = {
                                targetApps = (targetApps - pkg).toMutableList()
                                prefs.edit()
                                    .putStringSet("target_apps", targetApps.toSet())
                                    .apply()
                            }
                        )
                    }
                }
            }
        }
    }
}