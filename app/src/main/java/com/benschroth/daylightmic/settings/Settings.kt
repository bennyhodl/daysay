package com.benschroth.daylightmic.settings

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

enum class EngineKind { LOCAL, REMOTE }

/**
 * Remote providers. All three expose an OpenAI-compatible chat endpoint.
 * Only Groq and OpenAI expose a dedicated audio transcription endpoint;
 * OpenRouter transcribes through a chat completion with an audio content part.
 */
enum class Provider(
    val label: String,
    val baseUrl: String,
    val hasTranscriptionEndpoint: Boolean,
    val defaultSttModel: String,
    val defaultChatModel: String,
) {
    GROQ(
        label = "Groq",
        baseUrl = "https://api.groq.com/openai/v1",
        hasTranscriptionEndpoint = true,
        defaultSttModel = "whisper-large-v3-turbo",
        defaultChatModel = "openai/gpt-oss-20b",
    ),
    OPENAI(
        label = "OpenAI",
        baseUrl = "https://api.openai.com/v1",
        hasTranscriptionEndpoint = true,
        defaultSttModel = "gpt-transcribe",
        defaultChatModel = "gpt-5.6-luna",
    ),
    OPENROUTER(
        label = "OpenRouter",
        baseUrl = "https://openrouter.ai/api/v1",
        hasTranscriptionEndpoint = false,
        defaultSttModel = "google/gemini-2.5-flash-lite",
        defaultChatModel = "google/gemini-2.5-flash-lite",
    ),
}

const val DEFAULT_CLEANUP_PROMPT =
    "You clean up voice dictation transcripts. Fix punctuation, capitalization and obvious " +
        "transcription errors. Remove filler words (um, uh, like, you know) and false starts. " +
        "Keep the speaker's words, tone and meaning. Do not add, summarize or answer anything. " +
        "Output only the cleaned text, nothing else."

data class TriggerKey(val keyCode: Int, val scanCode: Int) {
    val isSet: Boolean get() = keyCode > 0 || scanCode > 0

    fun matches(keyCode: Int, scanCode: Int): Boolean {
        if (!isSet) return false
        // A physical button has a stable scan code. When we know it, require it: this keeps an
        // injected KEYCODE_BACK from the navigation gesture apart from a hardware BACK button.
        if (this.scanCode > 0) return this.scanCode == scanCode && (this.keyCode <= 0 || this.keyCode == keyCode)
        return this.keyCode == keyCode
    }

    companion object {
        val NONE = TriggerKey(-1, -1)

        /** The DC-1 orange side button: Linux KEY_F11 (scan 87), Android KEYCODE_F11 (141). */
        val DC1_SIDE = TriggerKey(keyCode = 141, scanCode = 87)

        /** The DC-1 orange top button: Linux KEY_F12 (scan 88), Android KEYCODE_F12 (142). */
        val DC1_TOP = TriggerKey(keyCode = 142, scanCode = 88)
    }
}

data class AppSettings(
    val trigger: TriggerKey = TriggerKey.DC1_SIDE,
    val engine: EngineKind = EngineKind.LOCAL,
    val localModel: String = "base.en-q5_1",
    val sttProvider: Provider = Provider.GROQ,
    val sttModel: String = "",
    val apiKeys: Map<Provider, String> = emptyMap(),
    val cleanupEnabled: Boolean = false,
    val cleanupProvider: Provider = Provider.GROQ,
    val cleanupModel: String = "",
    val cleanupPrompt: String = DEFAULT_CLEANUP_PROMPT,
    val language: String = "en",
    val maxRecordSeconds: Int = 120,
) {
    fun apiKey(provider: Provider): String = apiKeys[provider].orEmpty()
    fun sttModelOrDefault(): String = sttModel.ifBlank { sttProvider.defaultSttModel }
    fun cleanupModelOrDefault(): String = cleanupModel.ifBlank { cleanupProvider.defaultChatModel }
}

object SettingsStore {
    private lateinit var prefs: SharedPreferences
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> get() = _settings
    val current: AppSettings get() = _settings.value

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("daylight_mic", Context.MODE_PRIVATE)
        _settings.value = load()
    }

    fun update(block: (AppSettings) -> AppSettings) {
        val next = block(_settings.value)
        _settings.value = next
        save(next)
    }

    private fun load(): AppSettings = AppSettings(
        trigger = TriggerKey(
            prefs.getInt("trigger_keycode", TriggerKey.DC1_SIDE.keyCode),
            prefs.getInt("trigger_scancode", TriggerKey.DC1_SIDE.scanCode),
        ),
        engine = enumOr(prefs.getString("engine", null), EngineKind.LOCAL),
        localModel = prefs.getString("local_model", null) ?: "base.en-q5_1",
        sttProvider = enumOr(prefs.getString("stt_provider", null), Provider.GROQ),
        sttModel = prefs.getString("stt_model", "").orEmpty(),
        apiKeys = Provider.entries.associateWith { prefs.getString("key_${it.name}", "").orEmpty() },
        cleanupEnabled = prefs.getBoolean("cleanup_enabled", false),
        cleanupProvider = enumOr(prefs.getString("cleanup_provider", null), Provider.GROQ),
        cleanupModel = prefs.getString("cleanup_model", "").orEmpty(),
        cleanupPrompt = prefs.getString("cleanup_prompt", null) ?: DEFAULT_CLEANUP_PROMPT,
        language = prefs.getString("language", null) ?: "en",
        maxRecordSeconds = prefs.getInt("max_record_seconds", 120),
    )

    private fun save(s: AppSettings) {
        prefs.edit().apply {
            putInt("trigger_keycode", s.trigger.keyCode)
            putInt("trigger_scancode", s.trigger.scanCode)
            putString("engine", s.engine.name)
            putString("local_model", s.localModel)
            putString("stt_provider", s.sttProvider.name)
            putString("stt_model", s.sttModel)
            s.apiKeys.forEach { (p, k) -> putString("key_${p.name}", k) }
            putBoolean("cleanup_enabled", s.cleanupEnabled)
            putString("cleanup_provider", s.cleanupProvider.name)
            putString("cleanup_model", s.cleanupModel)
            putString("cleanup_prompt", s.cleanupPrompt)
            putString("language", s.language)
            putInt("max_record_seconds", s.maxRecordSeconds)
        }.apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}
