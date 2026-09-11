package dev.bennyb.daysay.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import dev.bennyb.daysay.R
import dev.bennyb.daysay.core.Delivery
import dev.bennyb.daysay.core.Dictation
import dev.bennyb.daysay.core.DictationState
import dev.bennyb.daysay.settings.AppSettings
import dev.bennyb.daysay.settings.SettingsStore

@Composable
fun HomeScreen(
    micGranted: Boolean,
    accessibilityEnabled: Boolean,
    setup: SetupActions,
    onOpenHistory: () -> Unit,
    onOpenSettings: () -> Unit,
) {
    val state by Dictation.state.collectAsState()
    val settings by SettingsStore.settings.collectAsState()
    val focus = LocalFocusManager.current
    val ready = micGranted && accessibilityEnabled

    Page(
        title = "Daysay",
        titleIcon = R.drawable.ic_daysay_mark,
        centered = true,
        actions = {
            IconButton(onClick = onOpenHistory) { Icon(painterResource(R.drawable.ic_history), contentDescription = "History") }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
        },
    ) {
        if (!ready) {
            SetupBlock(micGranted, accessibilityEnabled, setup)
            Spacer(Modifier.height(40.dp))
        }

        // The signature: the live waveform is the test button. Tap it, speak, tap it again.
        InkSlab(
            modifier = Modifier.clickable(enabled = micGranted) {
                focus.clearFocus()
                Dictation.toggle()
            },
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 24.dp)) {
                ModelLine(settings)
                Spacer(Modifier.height(28.dp))
                Waveform(state = state, modifier = Modifier.fillMaxWidth().height(112.dp))
                Spacer(Modifier.height(24.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = slabLabel(state, micGranted),
                        style = MaterialTheme.typography.bodyLarge,
                        color = Paper.paper,
                        modifier = Modifier.weight(1f),
                    )
                    if (state !is DictationState.Idle && state !is DictationState.Done && state !is DictationState.Failed) {
                        TextButton(onClick = { Dictation.cancel() }) { Text("Cancel", color = Paper.paper) }
                    }
                }
            }
        }
    }
}

/** The model in use, printed small in the corner of the slab. Same words as the floating panel. */
@Composable
private fun ModelLine(settings: AppSettings) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(settings.summary.uppercase(), style = EyebrowStyle, color = Paper.ash)
        if (settings.isRemote) {
            Icon(painterResource(R.drawable.ic_cloud), contentDescription = "Remote", tint = Paper.ash, modifier = Modifier.size(16.dp))
        }
    }
}

@Composable
private fun SetupBlock(micGranted: Boolean, accessibilityEnabled: Boolean, setup: SetupActions) {
    Text("Two steps before the button works.", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(20.dp))
    SetupRow(
        done = micGranted,
        title = "Microphone",
        detail = "Audio stays on the tablet unless you pick a remote model.",
        action = "Allow",
        onAction = setup.onRequestMic,
    )
    Spacer(Modifier.height(12.dp))
    SetupRow(
        done = accessibilityEnabled,
        title = "Accessibility service",
        detail = "Sees the button and types the text. Greyed out? Allow restricted settings in App info.",
        action = "Open settings",
        onAction = setup.onOpenAccessibilitySettings,
        secondary = "App info",
        onSecondary = setup.onOpenAppInfo,
    )
}

@Composable
private fun SetupRow(
    done: Boolean,
    title: String,
    detail: String,
    action: String,
    onAction: () -> Unit,
    secondary: String? = null,
    onSecondary: (() -> Unit)? = null,
) {
    InkCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            CheckMark(done)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(2.dp))
                Text(detail, style = MaterialTheme.typography.bodySmall, color = Paper.graphite)
            }
            if (!done) {
                Spacer(Modifier.width(16.dp))
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    if (secondary != null && onSecondary != null) {
                        TextButton(onClick = onSecondary) { Text(secondary) }
                    }
                    OutlinedButton(onClick = onAction) { Text(action) }
                }
            }
        }
    }
}

@Composable
private fun CheckMark(done: Boolean) {
    val base = Modifier.size(28.dp)
    if (done) {
        Icon(
            Icons.Outlined.Check,
            contentDescription = "Done",
            tint = Paper.paper,
            modifier = base.background(Paper.ink, CircleShape).padding(5.dp),
        )
    } else {
        Spacer(base.border(1.5.dp, Paper.ink, CircleShape))
    }
}

private fun slabLabel(state: DictationState, micGranted: Boolean): String = when (state) {
    DictationState.Idle -> if (micGranted) "Tap to try, or press the side button" else "Allow the microphone to try it"
    is DictationState.Listening -> "Listening. Tap to stop"
    DictationState.Transcribing -> "Transcribing"
    DictationState.Cleaning -> "Cleaning up"
    is DictationState.Done -> describeDelivery(state.delivery)
    is DictationState.Failed -> state.message
}

fun describeDelivery(delivery: Delivery): String = when (delivery) {
    Delivery.INSERTED -> "Inserted into the focused field"
    Delivery.PASTED -> "Pasted into the focused field"
    Delivery.CLIPBOARD -> "Copied to clipboard"
}

fun formatDuration(seconds: Float): String {
    val s = seconds.toInt()
    return if (s < 60) "${s}s" else "${s / 60}m ${s % 60}s"
}
