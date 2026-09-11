package com.benschroth.daylightmic.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.benschroth.daylightmic.core.Dictation
import com.benschroth.daylightmic.core.DictationState
import com.benschroth.daylightmic.model.DownloadState
import com.benschroth.daylightmic.model.ModelCatalog
import com.benschroth.daylightmic.model.ModelManager
import com.benschroth.daylightmic.settings.AppSettings
import com.benschroth.daylightmic.settings.DEFAULT_CLEANUP_PROMPT
import com.benschroth.daylightmic.settings.EngineKind
import com.benschroth.daylightmic.settings.Provider
import com.benschroth.daylightmic.settings.SettingsStore

@Composable
fun SettingsScreen(
    micGranted: Boolean,
    accessibilityEnabled: Boolean,
    onRequestMic: () -> Unit,
    onOpenAccessibilitySettings: () -> Unit,
    onOpenAppInfo: () -> Unit,
) {
    val settings by SettingsStore.settings.collectAsState()
    val state by Dictation.state.collectAsState()
    val learnMode by Dictation.learnMode.collectAsState()
    val lastKey by Dictation.lastKey.collectAsState()
    val lastTranscript by Dictation.lastTranscript.collectAsState()
    val download by ModelManager.download.collectAsState()
    val modelRevision by ModelManager.revision.collectAsState()
    val context = LocalContext.current
    val focus = LocalFocusManager.current

    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Text("Daylight Mic", style = MaterialTheme.typography.headlineMedium)
            Text(
                "Press the button once to listen, press again to transcribe. The text goes into the " +
                    "focused field and to the clipboard.",
                style = MaterialTheme.typography.bodyMedium,
            )

            SectionTitle("Setup")
            StatusRow(
                ok = micGranted,
                label = "Microphone permission",
                action = if (micGranted) null else "Grant",
                onAction = onRequestMic,
            )
            StatusRow(
                ok = accessibilityEnabled,
                label = "Accessibility service",
                action = if (accessibilityEnabled) null else "Open settings",
                onAction = onOpenAccessibilitySettings,
            )
            if (!accessibilityEnabled) {
                Text(
                    "If the switch is greyed out: open App info, tap the menu in the top right, " +
                        "choose Allow restricted settings, then come back.",
                    style = MaterialTheme.typography.bodySmall,
                )
                TextButton(onClick = onOpenAppInfo) { Text("Open App info") }
            }

            SectionTitle("Trigger button")
            val trigger = settings.trigger
            Text(
                when {
                    trigger == com.benschroth.daylightmic.settings.TriggerKey.DC1_SIDE -> "Current: orange side button (F11)"
                    trigger == com.benschroth.daylightmic.settings.TriggerKey.DC1_TOP -> "Current: orange top button (F12)"
                    trigger.isSet -> "Current: ${android.view.KeyEvent.keyCodeToString(trigger.keyCode)} " +
                        "(key ${trigger.keyCode}, scan ${trigger.scanCode})"
                    else -> "Not set"
                },
                style = MaterialTheme.typography.bodyMedium,
            )
            if (learnMode) {
                Text("Press the orange button now.", style = MaterialTheme.typography.titleMedium)
                OutlinedButton(onClick = { Dictation.learnMode.value = false }) { Text("Cancel") }
            } else {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { Dictation.learnMode.value = true }, enabled = accessibilityEnabled) {
                        Text("Learn button")
                    }
                    OutlinedButton(onClick = { SettingsStore.update { it.copy(trigger = com.benschroth.daylightmic.settings.TriggerKey.DC1_SIDE) } }) {
                        Text("Side button")
                    }
                    OutlinedButton(onClick = { SettingsStore.update { it.copy(trigger = com.benschroth.daylightmic.settings.TriggerKey.DC1_TOP) } }) {
                        Text("Top button")
                    }
                }
            }
            lastKey?.let {
                Text(
                    "Last key seen: ${it.name} (key ${it.keyCode}, scan ${it.scanCode}, device ${it.deviceId})",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
            if (!accessibilityEnabled) {
                Text("Enable the accessibility service first. It is what sees the button.", style = MaterialTheme.typography.bodySmall)
            }

            SectionTitle("Test")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { focus.clearFocus(); Dictation.toggle() },
                    enabled = micGranted && state !is DictationState.Transcribing && state !is DictationState.Cleaning,
                ) {
                    Text(if (state is DictationState.Listening) "Stop and transcribe" else "Start listening")
                }
                if (state !is DictationState.Idle) {
                    OutlinedButton(onClick = { Dictation.cancel() }) { Text("Cancel") }
                }
            }
            Text("State: ${describe(state)}", style = MaterialTheme.typography.bodyMedium)
            if (lastTranscript.isNotBlank()) {
                Text("Last transcript", style = MaterialTheme.typography.labelLarge)
                Text(lastTranscript, style = MaterialTheme.typography.bodyMedium)
            }

            SectionTitle("Engine")
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = settings.engine == EngineKind.LOCAL,
                    onClick = { SettingsStore.update { it.copy(engine = EngineKind.LOCAL) } },
                    label = { Text("On device") },
                )
                FilterChip(
                    selected = settings.engine == EngineKind.REMOTE,
                    onClick = { SettingsStore.update { it.copy(engine = EngineKind.REMOTE) } },
                    label = { Text("Remote API") },
                )
            }

            when (settings.engine) {
                EngineKind.LOCAL -> LocalEngineSection(settings, download, modelRevision)
                EngineKind.REMOTE -> RemoteEngineSection(settings)
            }

            SectionTitle("Cleanup pass")
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Switch(
                    checked = settings.cleanupEnabled,
                    onCheckedChange = { on -> SettingsStore.update { it.copy(cleanupEnabled = on) } },
                )
                Text("Send the transcript to a chat model for punctuation and filler removal")
            }
            if (settings.cleanupEnabled) {
                ProviderChips(settings.cleanupProvider) { p -> SettingsStore.update { it.copy(cleanupProvider = p) } }
                OutlinedTextField(
                    value = settings.cleanupModel,
                    onValueChange = { v -> SettingsStore.update { it.copy(cleanupModel = v) } },
                    label = { Text("Model") },
                    placeholder = { Text(settings.cleanupProvider.defaultChatModel) },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )
                ApiKeyField(settings, settings.cleanupProvider)
                OutlinedTextField(
                    value = settings.cleanupPrompt,
                    onValueChange = { v -> SettingsStore.update { it.copy(cleanupPrompt = v) } },
                    label = { Text("Prompt") },
                    minLines = 3,
                    modifier = Modifier.fillMaxWidth(),
                )
                TextButton(onClick = { SettingsStore.update { it.copy(cleanupPrompt = DEFAULT_CLEANUP_PROMPT) } }) {
                    Text("Reset prompt")
                }
            }

            SectionTitle("Options")
            OutlinedTextField(
                value = settings.language,
                onValueChange = { v -> SettingsStore.update { it.copy(language = v.trim()) } },
                label = { Text("Language code (empty = auto detect)") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            OutlinedTextField(
                value = if (settings.maxRecordSeconds == 0) "" else settings.maxRecordSeconds.toString(),
                onValueChange = { v -> if (v.isEmpty()) SettingsStore.update { it.copy(maxRecordSeconds = 0) } else v.toIntOrNull()?.let { n -> SettingsStore.update { it.copy(maxRecordSeconds = n) } } },
                label = { Text("Maximum recording length in seconds (5 to 600)") },
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(24.dp))
        }
    }
}

@Composable
private fun LocalEngineSection(settings: AppSettings, download: DownloadState, revision: Int) {
    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    Text("Models are downloaded once from Hugging Face and stay on the device.", style = MaterialTheme.typography.bodySmall)
    ModelCatalog.models.forEach { model ->
        val downloaded = ModelManager.isDownloaded(context, model)
        val running = download as? DownloadState.Running
        val failed = download as? DownloadState.Failed
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.localModel == model.id,
                    onClick = { SettingsStore.update { it.copy(localModel = model.id) } },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.id, style = MaterialTheme.typography.bodyLarge)
                    Text("${model.label}. ${model.sizeMb} MB", style = MaterialTheme.typography.bodySmall)
                }
                when {
                    running?.modelId == model.id -> TextButton(onClick = { ModelManager.cancelDownload() }) { Text("Cancel") }
                    downloaded -> TextButton(onClick = { ModelManager.delete(context, model) }) { Text("Delete") }
                    else -> OutlinedButton(
                        onClick = { ModelManager.startDownload(context, model) },
                        enabled = running == null,
                    ) { Text("Download") }
                }
            }
            if (running?.modelId == model.id) {
                LinearProgressIndicator(
                    progress = { running.progress },
                    modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 8.dp),
                )
            }
            if (failed?.modelId == model.id) {
                Text("Download failed: ${failed.message}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 48.dp))
                TextButton(onClick = { ModelManager.dismissFailure() }, modifier = Modifier.padding(start = 36.dp)) { Text("Dismiss") }
            }
        }
    }
    val selected = ModelCatalog.byId(settings.localModel)
    if (selected != null && !ModelManager.isDownloaded(context, selected)) {
        Text("The selected model is not downloaded yet.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RemoteEngineSection(settings: AppSettings) {
    ProviderChips(settings.sttProvider) { p -> SettingsStore.update { it.copy(sttProvider = p) } }
    if (!settings.sttProvider.hasTranscriptionEndpoint) {
        Text(
            "${settings.sttProvider.label} has no transcription endpoint. The audio is sent to a chat model " +
                "that accepts audio input.",
            style = MaterialTheme.typography.bodySmall,
        )
    }
    OutlinedTextField(
        value = settings.sttModel,
        onValueChange = { v -> SettingsStore.update { it.copy(sttModel = v) } },
        label = { Text("Model") },
        placeholder = { Text(settings.sttProvider.defaultSttModel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    ApiKeyField(settings, settings.sttProvider)
}

@Composable
private fun ProviderChips(selected: Provider, onSelect: (Provider) -> Unit) {
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Provider.entries.forEach { p ->
            FilterChip(selected = selected == p, onClick = { onSelect(p) }, label = { Text(p.label) })
        }
    }
}

@Composable
private fun ApiKeyField(settings: AppSettings, provider: Provider) {
    OutlinedTextField(
        value = settings.apiKey(provider),
        onValueChange = { v -> SettingsStore.update { it.copy(apiKeys = it.apiKeys + (provider to v.trim())) } },
        label = { Text("${provider.label} API key") },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
        modifier = Modifier.fillMaxWidth(),
    )
}

@Composable
private fun SectionTitle(text: String) {
    Column(modifier = Modifier.fillMaxWidth().padding(top = 8.dp)) {
        Text(text, style = MaterialTheme.typography.titleLarge)
        HorizontalDivider(modifier = Modifier.padding(top = 4.dp))
    }
}

@Composable
private fun StatusRow(ok: Boolean, label: String, action: String?, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
        Text(if (ok) "●" else "○", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f))
        if (action != null) OutlinedButton(onClick = onAction) { Text(action) }
    }
}

private fun describe(state: DictationState): String = when (state) {
    DictationState.Idle -> "Idle"
    is DictationState.Listening -> "Listening"
    DictationState.Transcribing -> "Transcribing"
    DictationState.Cleaning -> "Cleaning up"
    is DictationState.Done -> when (state.delivery) {
        com.benschroth.daylightmic.core.Delivery.INSERTED -> "Done. Inserted into the focused field"
        com.benschroth.daylightmic.core.Delivery.PASTED -> "Done. Pasted into the focused field"
        com.benschroth.daylightmic.core.Delivery.CLIPBOARD -> "Done. Copied to clipboard"
    }
    is DictationState.Failed -> "Failed. ${state.message}"
}
