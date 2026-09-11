package dev.bennyb.daysay.settings

import android.content.Context
import android.content.SharedPreferences
import dev.bennyb.daysay.model.ModelCatalog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/**
 * Remote providers. All three expose an OpenAI-compatible chat endpoint.
 * Only Groq and OpenAI expose a dedicated audio transcription endpoint;
 * OpenRouter transcribes through a chat completion with an audio content part.
 * One API key per provider, shared by transcription and the cleanup pass.
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

/**
 * All user settings as one value.
 *
 * [model] is a catalog id: a local whisper model, or `remote:<PROVIDER>` for a remote one.
 * [provider] is the remote provider in use. It follows the model when a remote model is chosen,
 * and is picked by the user when the cleanup pass runs next to a local model.
 */
data class AppSettings(
    val trigger: TriggerKey = TriggerKey.DC1_SIDE,
    val model: String = ModelCatalog.DEFAULT_ID,
    val provider: Provider = Provider.GROQ,
    val apiKeys: Map<Provider, String> = emptyMap(),
    val sttModel: String = "",
    val cleanupEnabled: Boolean = false,
    val cleanupModel: String = "",
    val cleanupPrompt: String = DEFAULT_CLEANUP_PROMPT,
    val language: String = "en",
    val maxRecordSeconds: Int = 120,
) {
    val isRemote: Boolean get() = ModelCatalog.isRemote(model)
    fun apiKey(provider: Provider): String = apiKeys[provider].orEmpty()
    fun hasKey(provider: Provider): Boolean = apiKey(provider).isNotBlank()
    fun sttModelOrDefault(): String = sttModel.ifBlank { provider.defaultSttModel }
    fun cleanupModelOrDefault(): String = cleanupModel.ifBlank { provider.defaultChatModel }

    /** One line naming the model in use: "Medium  ·  on device", "Groq  ·  remote  ·  cleanup". */
    val summary: String
        get() {
            val where = if (isRemote) "remote" else "on device"
            val cleanup = if (cleanupEnabled) "  ·  cleanup" else ""
            return "${ModelCatalog.displayName(model)}  ·  $where$cleanup"
        }

    fun withKey(provider: Provider, key: String): AppSettings = copy(apiKeys = apiKeys + (provider to key.trim()))

    /** Selecting a remote model also makes its provider the one the cleanup pass uses. */
    fun withModel(id: String): AppSettings {
        val remote = ModelCatalog.remoteProvider(id)
        return if (remote != null) copy(model = id, provider = remote) else copy(model = id)
    }
}

object SettingsStore {
    private lateinit var prefs: SharedPreferences
    private val _settings = MutableStateFlow(AppSettings())
    val settings: StateFlow<AppSettings> get() = _settings
    val current: AppSettings get() = _settings.value

    fun init(context: Context) {
        prefs = context.applicationContext.getSharedPreferences("daysay", Context.MODE_PRIVATE)
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
        model = prefs.getString("model", null)?.takeIf { ModelCatalog.byId(it) != null } ?: ModelCatalog.DEFAULT_ID,
        provider = enumOr(prefs.getString("provider", null), Provider.GROQ),
        apiKeys = Provider.entries.associateWith { prefs.getString("key_${it.name}", "").orEmpty() },
        sttModel = prefs.getString("stt_model", "").orEmpty(),
        cleanupEnabled = prefs.getBoolean("cleanup_enabled", false),
        cleanupModel = prefs.getString("cleanup_model", "").orEmpty(),
        cleanupPrompt = prefs.getString("cleanup_prompt", null) ?: DEFAULT_CLEANUP_PROMPT,
        language = prefs.getString("language", null) ?: "en",
        maxRecordSeconds = prefs.getInt("max_record_seconds", 120),
    )

    private fun save(s: AppSettings) {
        prefs.edit().apply {
            putInt("trigger_keycode", s.trigger.keyCode)
            putInt("trigger_scancode", s.trigger.scanCode)
            putString("model", s.model)
            putString("provider", s.provider.name)
            s.apiKeys.forEach { (p, k) -> putString("key_${p.name}", k) }
            putString("stt_model", s.sttModel)
            putBoolean("cleanup_enabled", s.cleanupEnabled)
            putString("cleanup_model", s.cleanupModel)
            putString("cleanup_prompt", s.cleanupPrompt)
            putString("language", s.language)
            putInt("max_record_seconds", s.maxRecordSeconds)
        }.apply()
    }

    private inline fun <reified T : Enum<T>> enumOr(name: String?, fallback: T): T =
        name?.let { runCatching { enumValueOf<T>(it) }.getOrNull() } ?: fallback
}
