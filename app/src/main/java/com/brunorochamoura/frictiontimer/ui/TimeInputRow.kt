package com.brunorochamoura.frictiontimer.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun TimeInputRow(
    label: String,
    minutes: Long,
    seconds: Long,
    onChange: (Long, Long) -> Unit
) {
    Column {
        Text(
            label,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(Modifier.height(4.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            var minText by remember { mutableStateOf(minutes.toString()) }
            var secText by remember { mutableStateOf(seconds.toString()) }

            BasicTextField(
                value = minText,
                onValueChange = {
                    minText = it
                    onChange(it.toLongOrNull() ?: 0L, secText.toLongOrNull() ?: 0L)
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .width(50.dp)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(4.dp),
                singleLine = true,
            )
            Text(
                " min ",
                color = MaterialTheme.colorScheme.onSecondary
            )

            BasicTextField(
                value = secText,
                onValueChange = {
                    secText = it
                    onChange(minText.toLongOrNull() ?: 0L, it.toLongOrNull() ?: 0L)
                },
                textStyle = androidx.compose.ui.text.TextStyle(
                    color = MaterialTheme.colorScheme.onSecondary
                ),
                modifier = Modifier
                    .width(50.dp)
                    .background(MaterialTheme.colorScheme.secondary)
                    .padding(4.dp),
                singleLine = true,
            )
            Text(
                " sec",
                color = MaterialTheme.colorScheme.onSecondary
            )
        }
    }
}