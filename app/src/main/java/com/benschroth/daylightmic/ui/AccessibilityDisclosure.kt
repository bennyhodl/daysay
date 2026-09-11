package com.benschroth.daylightmic.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

/**
 * Prominent disclosure shown before the user is sent to enable the accessibility service.
 * Google Play requires this for apps that use the AccessibilityService API for a purpose other
 * than assisting people with disabilities: it must be in the app, describe the data accessed,
 * explain how it is used, and require an affirmative action.
 */
@Composable
fun AccessibilityDisclosureDialog(onAgree: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("How Daylight Mic uses the accessibility service") },
        text = {
            Column {
                Text(
                    "Daylight Mic asks for the accessibility service for three things and nothing else.",
                    style = MaterialTheme.typography.bodyMedium,
                )
                Spacer(Modifier.height(12.dp))
                Bullet("It watches hardware key presses so the orange button can start and stop dictation. Other keys are seen but ignored.")
                Bullet("It shows the small dictation panel on top of other apps.")
                Bullet("It types your transcript into the text field that has focus. To do that it reads the text and cursor position of that one field, at that moment.")
                Spacer(Modifier.height(12.dp))
                Text(
                    "The service does not read, store, or send the content of your screen, your keystrokes, or " +
                        "your passwords. The only data that leaves the tablet is your recorded audio and " +
                        "transcript, and only when you choose a remote engine or the cleanup pass, and only to " +
                        "the provider you configured with your own key.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            }
        },
        confirmButton = { TextButton(onClick = onAgree) { Text("Agree and open settings") } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Not now") } },
    )
}

@Composable
private fun Bullet(text: String) {
    Text("•  $text", style = MaterialTheme.typography.bodyMedium, modifier = Modifier.padding(bottom = 6.dp))
}
