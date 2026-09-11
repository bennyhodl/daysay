package dev.bennyb.daysay.ui

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material.icons.outlined.KeyboardArrowUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import dev.bennyb.daysay.R
import dev.bennyb.daysay.core.Dictation
import dev.bennyb.daysay.model.DownloadState
import dev.bennyb.daysay.model.LocalModel
import dev.bennyb.daysay.model.ModelCatalog
import dev.bennyb.daysay.model.ModelManager
import dev.bennyb.daysay.model.RemoteModel
import dev.bennyb.daysay.settings.AppSettings
import dev.bennyb.daysay.settings.DEFAULT_CLEANUP_PROMPT
import dev.bennyb.daysay.settings.Provider
import dev.bennyb.daysay.settings.SettingsStore
import dev.bennyb.daysay.settings.TriggerKey

/** What the key dialog is for: choosing a remote model, or turning on the cleanup pass. */
private sealed interface KeyRequest {
    val provider: Provider
    data class ForModel(override val provider: Provider) : KeyRequest
    data class ForCleanup(override val provider: Provider, val chooseProvider: Boolean) : KeyRequest
}

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
    var keyRequest by remember { mutableStateOf<KeyRequest?>(null) }

    keyRequest?.let { request ->
        KeyDialog(
            request = request,
            initialKey = settings.apiKey(request.provider),
            onSave = { provider, key ->
                SettingsStore.update { s ->
                    val withKey = s.withKey(provider, key)
                    when (request) {
                        is KeyRequest.ForModel -> withKey.withModel(ModelCatalog.remoteId(provider))
                        is KeyRequest.ForCleanup -> withKey.copy(provider = provider, cleanupEnabled = true)
                    }
                }
                keyRequest = null
            },
            onDismiss = { keyRequest = null },
        )
    }

    Page(title = "Settings", onBack = onBack) {
        SectionHeader("Model")
        Text("Where speech becomes text. Tap one to use it.", style = MaterialTheme.typography.bodyMedium, color = Paper.graphite)
        Spacer(Modifier.height(8.dp))
        @Suppress("UNUSED_VARIABLE") val observedRevision = modelRevision
        ModelCatalog.local.forEach { model ->
            HorizontalDivider(color = Paper.mist)
            LocalModelRow(model, settings, download)
        }
        ModelCatalog.remote.forEach { model ->
            HorizontalDivider(color = Paper.mist)
            RemoteModelRow(
                model = model,
                settings = settings,
                onSelect = {
                    if (settings.hasKey(model.provider)) SettingsStore.update { it.withModel(model.id) }
                    else keyRequest = KeyRequest.ForModel(model.provider)
                },
                onEditKey = { keyRequest = KeyRequest.ForModel(model.provider) },
            )
        }
        HorizontalDivider(color = Paper.mist)

        SectionHeader("Cleanup pass")
        CleanupRow(
            settings = settings,
            onToggle = { on ->
                when {
                    !on -> SettingsStore.update { it.copy(cleanupEnabled = false) }
                    settings.hasKey(settings.provider) -> SettingsStore.update { it.copy(cleanupEnabled = true) }
                    else -> keyRequest = KeyRequest.ForCleanup(settings.provider, chooseProvider = !settings.isRemote)
                }
            },
            onChangeProvider = { keyRequest = KeyRequest.ForCleanup(settings.provider, chooseProvider = true) },
        )

        AdvancedSection(settings, micGranted, accessibilityEnabled, setup)
    }
}

@Composable
private fun LocalModelRow(model: LocalModel, settings: AppSettings, download: DownloadState) {
    val context = LocalContext.current
    val downloaded = ModelManager.isDownloaded(context, model)
    val running = (download as? DownloadState.Running)?.takeIf { it.modelId == model.id }
    val failed = (download as? DownloadState.Failed)?.takeIf { it.modelId == model.id }
    val selected = settings.model == model.id

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clickable {
                SettingsStore.update { it.withModel(model.id) }
                if (!downloaded && download !is DownloadState.Running) ModelManager.startDownload(context, model)
            }
            .padding(vertical = 10.dp),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Dot(selected)
            Spacer(Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium)
                MetaLine(model.detail + if (downloaded) "  ·  on device" else "")
            }
            when {
                running != null -> IconButton(onClick = { ModelManager.cancelDownload() }) {
                    Icon(Icons.Outlined.Close, contentDescription = "Cancel download")
                }
                downloaded -> IconButton(onClick = { ModelManager.delete(context, model) }) {
                    Icon(Icons.Outlined.Delete, contentDescription = "Remove from device")
                }
                else -> IconButton(
                    onClick = { ModelManager.startDownload(context, model) },
                    enabled = download !is DownloadState.Running,
                ) {
                    Icon(painterResource(R.drawable.ic_download), contentDescription = "Download")
                }
            }
        }
        if (running != null) {
            LinearProgressIndicator(
                progress = { running.progress },
                modifier = Modifier.fillMaxWidth().padding(start = 30.dp, top = 8.dp),
            )
        }
        if (failed != null) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(start = 30.dp, top = 4.dp)) {
                Text("Download failed. ${failed.message}", style = MaterialTheme.typography.bodySmall, modifier = Modifier.weight(1f))
                TextButton(onClick = { ModelManager.dismissFailure() }) { Text("Dismiss") }
            }
        }
        if (selected && !downloaded && running == null) {
            Text(
                "Not on the device yet. Tap the arrow to download it.",
                style = MaterialTheme.typography.bodySmall,
                modifier = Modifier.padding(start = 30.dp, top = 4.dp),
            )
        }
    }
}

@Composable
private fun RemoteModelRow(model: RemoteModel, settings: AppSettings, onSelect: () -> Unit, onEditKey: () -> Unit) {
    val selected = settings.model == model.id
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth().clickable(onClick = onSelect).padding(vertical = 10.dp),
    ) {
        Dot(selected)
        Spacer(Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(model.name, style = MaterialTheme.typography.titleMedium)
                Icon(painterResource(R.drawable.ic_cloud), contentDescription = "Remote", tint = Paper.graphite, modifier = Modifier.size(18.dp))
            }
            MetaLine(model.detail + if (settings.hasKey(model.provider)) "  ·  key set" else "")
        }
        IconButton(onClick = onEditKey) {
            Icon(Icons.Outlined.Edit, contentDescription = "API key")
        }
    }
}

@Composable
private fun CleanupRow(settings: AppSettings, onToggle: (Boolean) -> Unit, onChangeProvider: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp)) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Cleanup pass", style = MaterialTheme.typography.titleMedium)
            MetaLine(
                if (settings.cleanupEnabled) "${settings.provider.label}  ·  ${settings.cleanupModelOrDefault()}  ·  your key"
                else "Off  ·  always remote, with your key"
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "A chat model fixes punctuation and removes filler words before the text is inserted.",
                style = MaterialTheme.typography.bodyMedium,
                color = Paper.graphite,
            )
            if (settings.cleanupEnabled && !settings.isRemote) {
                TextButton(onClick = onChangeProvider, contentPadding = PaddingValues(0.dp)) {
                    Text("Change provider")
                }
            }
        }
        Spacer(Modifier.width(16.dp))
        Switch(checked = settings.cleanupEnabled, onCheckedChange = onToggle)
    }
}

/**
 * One key per provider. The same key serves transcription and the cleanup pass.
 * With a provider choice (cleanup next to a local model) the chips pick which one.
 */
@Composable
private fun KeyDialog(request: KeyRequest, initialKey: String, onSave: (Provider, String) -> Unit, onDismiss: () -> Unit) {
    var provider by remember { mutableStateOf(request.provider) }
    var key by remember { mutableStateOf(initialKey) }
    val settings by SettingsStore.settings.collectAsState()
    val chooseProvider = request is KeyRequest.ForCleanup && request.chooseProvider

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (chooseProvider) "Cleanup pass" else "${provider.label} API key") },
        text = {
            Column {
                if (chooseProvider) {
                    Text("Which provider runs the cleanup?", style = MaterialTheme.typography.bodyMedium, color = Paper.graphite)
                    Spacer(Modifier.height(12.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Provider.entries.forEach { p ->
                            FilterChip(
                                selected = provider == p,
                                onClick = { provider = p; key = settings.apiKey(p) },
                                label = { Text(p.label) },
                            )
                        }
                    }
                    Spacer(Modifier.height(16.dp))
                }
                Text(
                    "The key stays on the tablet. It is used for transcription and the cleanup pass on ${provider.label}.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Paper.graphite,
                )
                Spacer(Modifier.height(12.dp))
                OutlinedTextField(
                    value = key,
                    onValueChange = { key = it },
                    label = { Text("${provider.label} API key") },
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onSave(provider, key) }, enabled = key.isNotBlank()) { Text("Save") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
    )
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
        Text("Cleanup instructions", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
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

        Spacer(Modifier.height(20.dp))
        Text("Remote model ids", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(4.dp))
        Text(
            "Leave empty for the ${settings.provider.label} defaults.",
            style = MaterialTheme.typography.bodyMedium,
            color = Paper.graphite,
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = settings.sttModel,
            onValueChange = { v -> SettingsStore.update { it.copy(sttModel = v.trim()) } },
            label = { Text("Transcription model") },
            placeholder = { Text(settings.provider.defaultSttModel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
        Spacer(Modifier.height(12.dp))
        OutlinedTextField(
            value = settings.cleanupModel,
            onValueChange = { v -> SettingsStore.update { it.copy(cleanupModel = v.trim()) } },
            label = { Text("Cleanup model") },
            placeholder = { Text(settings.provider.defaultChatModel) },
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )

        Spacer(Modifier.height(28.dp))
        Text("Speech", style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(8.dp))
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

        Spacer(Modifier.height(28.dp))
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
        Dot(ok)
        Spacer(Modifier.width(12.dp))
        Text(label, modifier = Modifier.weight(1f), style = MaterialTheme.typography.bodyMedium)
        if (!ok) OutlinedButton(onClick = onAction) { Text(action) } else MetaLine("Granted")
    }
}
