package com.benschroth.daylightmic.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.benschroth.daylightmic.core.Dictation
import com.benschroth.daylightmic.model.DownloadState
import com.benschroth.daylightmic.model.ModelCatalog
import com.benschroth.daylightmic.model.ModelManager
import com.benschroth.daylightmic.settings.AppSettings
import com.benschroth.daylightmic.settings.DEFAULT_CLEANUP_PROMPT
import com.benschroth.daylightmic.settings.EngineKind
import com.benschroth.daylightmic.settings.Provider
import com.benschroth.daylightmic.settings.SettingsStore
import com.benschroth.daylightmic.settings.TriggerKey

@Composable
fun SettingsScreen(
    micGranted: Boolean,
    accessibilityEnabled: Boolean,
    setup: SetupActions,
    onBack: () -> Unit,
) {
    val settings by SettingsStore.settings.collectAsState()
    val download by ModelManager.download.collectAsState()
    val modelRevision by ModelManager.revision.collectAsState()

    Page(title = "Settings", onBack = onBack) {
        SectionHeader("Engine")
        Text("Where speech becomes text.", style = MaterialTheme.typography.bodyMedium, color = Paper.graphite)
        Spacer(Modifier.height(14.dp))
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
        Spacer(Modifier.height(20.dp))
        when (settings.engine) {
            EngineKind.LOCAL -> LocalEngineSection(settings, download, modelRevision)
            EngineKind.REMOTE -> RemoteEngineSection(settings)
        }

        SectionHeader("Cleanup pass")
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(modifier = Modifier.weight(1f)) {
                Text("Tidy the transcript", style = MaterialTheme.typography.titleMedium)
                Text(
                    "A chat model fixes punctuation and removes fillers before the text is inserted. Always remote, with your key.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.graphite,
                )
            }
            Spacer(Modifier.width(16.dp))
            Switch(
                checked = settings.cleanupEnabled,
                onCheckedChange = { on -> SettingsStore.update { it.copy(cleanupEnabled = on) } },
            )
        }
        if (settings.cleanupEnabled) {
            Spacer(Modifier.height(16.dp))
            ProviderChips(settings.cleanupProvider) { p -> SettingsStore.update { it.copy(cleanupProvider = p) } }
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.cleanupModel,
                onValueChange = { v -> SettingsStore.update { it.copy(cleanupModel = v) } },
                label = { Text("Model") },
                placeholder = { Text(settings.cleanupProvider.defaultChatModel) },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))
            ApiKeyField(settings, settings.cleanupProvider)
            Spacer(Modifier.height(12.dp))
            OutlinedTextField(
                value = settings.cleanupPrompt,
                onValueChange = { v -> SettingsStore.update { it.copy(cleanupPrompt = v) } },
                label = { Text("Instructions for the model") },
                minLines = 3,
                modifier = Modifier.fillMaxWidth(),
            )
            TextButton(onClick = { SettingsStore.update { it.copy(cleanupPrompt = DEFAULT_CLEANUP_PROMPT) } }) {
                Text("Reset instructions")
            }
        }

        SectionHeader("Speech")
        OutlinedTextField(
            value = settings.language,
            onValueChange = { v -> SettingsStore.update { it.copy(language = v.trim()) } },
            label = { Text("Language code") },
            supportingText = { Text("Two letters, such as en or de. Leave empty to detect automatically.") },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = if (settings.maxRecordSeconds == 0) "" else settings.maxRecordSeconds.toString(),
            onValueChange = { v ->
                if (v.isEmpty()) SettingsStore.update { it.copy(maxRecordSeconds = 0) }
                else v.toIntOrNull()?.let { n -> SettingsStore.update { it.copy(maxRecordSeconds = n) } }
            },
            label = { Text("Longest recording, in seconds") },
            supportingText = { Text("Recording stops by itself after this. 5 to 600.") },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            modifier = Modifier.fillMaxWidth(),
        )

        AdvancedSection(settings, micGranted, accessibilityEnabled, setup)
    }
}

@Composable
private fun LocalEngineSection(settings: AppSettings, download: DownloadState, revision: Int) {
    val context = LocalContext.current
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    Text(
        "Whisper runs on the tablet. Nothing leaves the device. Models download once from Hugging Face.",
        style = MaterialTheme.typography.bodyMedium,
        color = Paper.graphite,
    )
    Spacer(Modifier.height(8.dp))
    ModelCatalog.models.forEach { model ->
        val downloaded = ModelManager.isDownloaded(context, model)
        val running = download as? DownloadState.Running
        val failed = download as? DownloadState.Failed
        HorizontalDivider(color = Paper.mist)
        Column(modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(
                    selected = settings.localModel == model.id,
                    onClick = { SettingsStore.update { it.copy(localModel = model.id) } },
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(model.id, style = MonoStyle)
                    MetaLine("${model.label}  ·  ${model.sizeMb} MB${if (downloaded) "  ·  on device" else ""}")
                }
                when {
                    running?.modelId == model.id -> TextButton(onClick = { ModelManager.cancelDownload() }) { Text("Cancel") }
                    downloaded -> TextButton(onClick = { ModelManager.delete(context, model) }) { Text("Remove") }
                    else -> OutlinedButton(
                        onClick = { ModelManager.startDownload(context, model) },
                        enabled = running == null,
                    ) { Text("Download") }
                }
            }
            if (running?.modelId == model.id) {
                LinearProgressIndicator(
                    progress = { running.progress },
                    modifier = Modifier.fillMaxWidth().padding(start = 48.dp, end = 8.dp, bottom = 8.dp),
                )
            }
            if (failed?.modelId == model.id) {
                Text("Download failed. ${failed.message}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.padding(start = 48.dp))
                TextButton(onClick = { ModelManager.dismissFailure() }, modifier = Modifier.padding(start = 36.dp)) { Text("Dismiss") }
            }
        }
    }
    HorizontalDivider(color = Paper.mist)
    val selected = ModelCatalog.byId(settings.localModel)
    if (selected != null && !ModelManager.isDownloaded(context, selected)) {
        Spacer(Modifier.height(12.dp))
        Text("Download ${selected.id} before dictating, or the bubble will report an error.", style = MaterialTheme.typography.bodyMedium)
    }
}

@Composable
private fun RemoteEngineSection(settings: AppSettings) {
    Text("Audio is sent to the provider with your own key.", style = MaterialTheme.typography.bodyMedium, color = Paper.graphite)
    Spacer(Modifier.height(12.dp))
    ProviderChips(settings.sttProvider) { p -> SettingsStore.update { it.copy(sttProvider = p) } }
    if (!settings.sttProvider.hasTranscriptionEndpoint) {
        Spacer(Modifier.height(8.dp))
        Text(
            "${settings.sttProvider.label} has no transcription endpoint, so the audio goes to a chat model that accepts audio.",
            style = MaterialTheme.typography.bodySmall,
            color = Paper.graphite,
        )
    }
    Spacer(Modifier.height(12.dp))
    OutlinedTextField(
        value = settings.sttModel,
        onValueChange = { v -> SettingsStore.update { it.copy(sttModel = v) } },
        label = { Text("Model") },
        placeholder = { Text(settings.sttProvider.defaultSttModel) },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    Spacer(Modifier.height(12.dp))
    ApiKeyField(settings, settings.sttProvider)
}

@Composable
private fun AdvancedSection(settings: AppSettings, micGranted: Boolean, accessibilityEnabled: Boolean, setup: SetupActions) {
    var open by rememberSaveable { mutableStateOf(false) }
    val learnMode by Dictation.learnMode.collectAsState()
    val lastKey by Dictation.lastKey.collectAsState()
    val trigger = settings.trigger

    Column(modifier = Modifier.fillMaxWidth().padding(top = 32.dp).animateContentSize()) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth().clickable { open = !open },
        ) {
            Eyebrow("Advanced", modifier = Modifier.weight(1f))
            Icon(
                if (open) Icons.Outlined.KeyboardArrowUp else Icons.Outlined.KeyboardArrowDown,
                contentDescription = if (open) "Collapse" else "Expand",
            )
        }
        HorizontalDivider(modifier = Modifier.padding(top = 10.dp), color = Paper.ink)
        if (!open) return@Column

        Spacer(Modifier.height(20.dp))
        Text("Trigger button", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            when {
                trigger == TriggerKey.DC1_SIDE -> "Orange side button"
                trigger == TriggerKey.DC1_TOP -> "Orange top button"
                trigger.isSet -> "${android.view.KeyEvent.keyCodeToString(trigger.keyCode)} (key ${trigger.keyCode}, scan ${trigger.scanCode})"
                else -> "Not set"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.graphite,
        )
        Spacer(Modifier.height(12.dp))
        if (learnMode) {
            Text("Press the button you want to use.", style = MaterialTheme.typography.bodyLarge)
            Spacer(Modifier.height(8.dp))
            OutlinedButton(onClick = { Dictation.learnMode.value = false }) { Text("Cancel") }
        } else {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(
                    selected = trigger == TriggerKey.DC1_SIDE,
                    onClick = { SettingsStore.update { it.copy(trigger = TriggerKey.DC1_SIDE) } },
                    label = { Text("Side") },
                )
                FilterChip(
                    selected = trigger == TriggerKey.DC1_TOP,
                    onClick = { SettingsStore.update { it.copy(trigger = TriggerKey.DC1_TOP) } },
                    label = { Text("Top") },
                )
                OutlinedButton(onClick = { Dictation.learnMode.value = true }, enabled = accessibilityEnabled) {
                    Text("Learn another key")
                }
            }
        }
        lastKey?.let {
            Spacer(Modifier.height(8.dp))
            MetaLine("Last key seen: ${it.name} (key ${it.keyCode}, scan ${it.scanCode}, device ${it.deviceId})")
        }

        Spacer(Modifier.height(28.dp))
        Text("Permissions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
        PermissionRow("Microphone", micGranted, "Allow", setup.onRequestMic)
        PermissionRow("Accessibility service", accessibilityEnabled, "Open settings", setup.onOpenAccessibilitySettings)
        TextButton(onClick = setup.onOpenAppInfo) { Text("Open App info") }
    }
}

@Composable
private fun PermissionRow(label: String, ok: Boolean, action: String, onAction: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(if (ok) "●" else "○", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!ok) OutlinedButton(onClick = onAction) { Text(action) } else MetaLine("Granted")
    }
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
