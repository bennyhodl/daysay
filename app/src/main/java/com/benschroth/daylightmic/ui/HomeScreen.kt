package com.benschroth.daylightmic.ui

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.border
import androidx.compose.foundation.background
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.List
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
import androidx.compose.ui.unit.dp
import com.benschroth.daylightmic.core.Delivery
import com.benschroth.daylightmic.core.Dictation
import com.benschroth.daylightmic.core.DictationState
import com.benschroth.daylightmic.model.ModelCatalog
import com.benschroth.daylightmic.model.TranscriptStore
import com.benschroth.daylightmic.settings.EngineKind
import com.benschroth.daylightmic.settings.SettingsStore

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
    val transcripts by TranscriptStore.items.collectAsState()
    val focus = LocalFocusManager.current
    val ready = micGranted && accessibilityEnabled

    Page(
        title = "Daylight Mic",
        actions = {
            IconButton(onClick = onOpenHistory) { Icon(Icons.AutoMirrored.Outlined.List, contentDescription = "History") }
            IconButton(onClick = onOpenSettings) { Icon(Icons.Outlined.Settings, contentDescription = "Settings") }
        },
    ) {
        Spacer(Modifier.height(24.dp))

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
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 26.dp)) {
                Waveform(state = state, modifier = Modifier.fillMaxWidth().height(96.dp))
                Spacer(Modifier.height(22.dp))
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

        Spacer(Modifier.height(40.dp))
        val last = transcripts.firstOrNull()
        Eyebrow("Last transcript")
        Spacer(Modifier.height(12.dp))
        if (last == null) {
            Text(
                "Nothing yet. In any app, tap a text field, press the orange side button, speak, and press it again.",
                style = MaterialTheme.typography.bodyLarge,
                color = Paper.graphite,
            )
        } else {
            Text(last.text, style = MonoStyle)
            Spacer(Modifier.height(10.dp))
            MetaLine("${last.engine}  ·  ${formatDuration(last.durationSeconds)}  ·  ${describeDelivery(last.delivery)}")
        }

        Spacer(Modifier.height(40.dp))
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.weight(1f)) {
                Eyebrow("Engine")
                Spacer(Modifier.height(4.dp))
                Text(engineSummary(settings.engine, settings.localModel, settings.sttProvider.label, settings.sttModelOrDefault()), style = MaterialTheme.typography.bodyMedium)
            }
            TextButton(onClick = onOpenSettings) { Text("Change") }
        }
    }
}

@Composable
private fun SetupBlock(micGranted: Boolean, accessibilityEnabled: Boolean, setup: SetupActions) {
    Text("Two steps before the button works.", style = MaterialTheme.typography.headlineMedium)
    Spacer(Modifier.height(20.dp))
    SetupRow(
        done = micGranted,
        title = "The app can't hear you yet",
        detail = "Allow the microphone. Audio stays on the tablet unless you choose a remote engine.",
        action = "Allow",
        onAction = setup.onRequestMic,
    )
    Spacer(Modifier.height(12.dp))
    SetupRow(
        done = accessibilityEnabled,
        title = "The orange button can't reach the app yet",
        detail = "Turn on the Daylight Mic accessibility service. It is how the app sees the button and types into " +
            "the focused field. If the switch is greyed out: open App info, tap the menu in the top right, " +
            "choose Allow restricted settings, then try again.",
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
        Row(verticalAlignment = Alignment.Top) {
            CheckMark(done)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Spacer(Modifier.height(4.dp))
                Text(detail, style = MaterialTheme.typography.bodyMedium, color = Paper.graphite)
                if (!done) {
                    Spacer(Modifier.height(14.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = onAction) { Text(action) }
                        if (secondary != null && onSecondary != null) {
                            TextButton(onClick = onSecondary) { Text(secondary) }
                        }
                    }
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

fun engineSummary(engine: EngineKind, localModel: String, providerLabel: String, remoteModel: String): String = when (engine) {
    EngineKind.LOCAL -> "On device  ·  ${ModelCatalog.byId(localModel)?.id ?: localModel}"
    EngineKind.REMOTE -> "$providerLabel  ·  $remoteModel"
}
