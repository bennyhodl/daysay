package com.benschroth.daylightmic.core

import android.Manifest
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.pm.PackageManager
import android.util.Log
import com.benschroth.daylightmic.audio.MicForegroundService
import com.benschroth.daylightmic.audio.Recorder
import com.benschroth.daylightmic.cleanup.CleanupClient
import com.benschroth.daylightmic.engine.LocalWhisperEngine
import com.benschroth.daylightmic.engine.RemoteTranscriptionEngine
import com.benschroth.daylightmic.engine.TranscriptionEngine
import com.benschroth.daylightmic.model.ModelCatalog
import com.benschroth.daylightmic.model.ModelManager
import com.benschroth.daylightmic.model.TranscriptStore
import com.benschroth.daylightmic.settings.AppSettings
import com.benschroth.daylightmic.settings.EngineKind
import com.benschroth.daylightmic.settings.SettingsStore
import com.benschroth.daylightmic.trigger.DictationAccessibilityService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

enum class Delivery { INSERTED, PASTED, CLIPBOARD }

sealed interface DictationState {
    data object Idle : DictationState
    data class Listening(val startedAt: Long) : DictationState
    data object Transcribing : DictationState
    data object Cleaning : DictationState
    data class Done(val text: String, val delivery: Delivery) : DictationState
    data class Failed(val message: String) : DictationState
}

/** A key event the accessibility service saw. Shown in the settings screen to identify the button. */
data class KeySeen(val keyCode: Int, val scanCode: Int, val deviceId: Int) {
    val name: String get() = android.view.KeyEvent.keyCodeToString(keyCode)
}

/**
 * The dictation state machine. One press starts listening, the next press stops it and runs
 * transcription, optional cleanup, and delivery. Shared by the hardware button, the settings
 * screen, the quick settings tile, and the toggle activity.
 */
object Dictation {
    private const val TAG = "Dictation"

    private lateinit var app: Context
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val lock = Any()

    private val _state = MutableStateFlow<DictationState>(DictationState.Idle)
    val state: StateFlow<DictationState> get() = _state

    val learnMode = MutableStateFlow(false)
    val lastKey = MutableStateFlow<KeySeen?>(null)
    val lastTranscript = MutableStateFlow("")

    /** Microphone level in 0..1 while listening. Drives the waveform visualisers. */
    val level = MutableStateFlow(0f)

    private var recorder: Recorder? = null
    private var work: Job? = null
    private var resetJob: Job? = null

    fun init(context: Context) {
        app = context.applicationContext
    }

    val hasMicPermission: Boolean
        get() = app.checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED

    /** Called from the button. Must return fast: the key event callback has a 500 ms budget. */
    fun toggle() {
        synchronized(lock) {
            when (_state.value) {
                is DictationState.Idle, is DictationState.Done, is DictationState.Failed -> start()
                is DictationState.Listening -> finish()
                is DictationState.Transcribing, is DictationState.Cleaning -> Unit
            }
        }
    }

    fun cancel() {
        val r: Recorder?
        synchronized(lock) {
            work?.cancel()
            work = null
            r = recorder
            recorder = null
            MicForegroundService.stop(app)
            level.value = 0f
            setState(DictationState.Idle)
        }
        // Joining the capture thread can take a moment. Keep it off the caller's thread.
        if (r != null) scope.launch { runCatching { r.stop() } }
    }

    private fun start() {
        resetJob?.cancel()
        if (!hasMicPermission) {
            fail("Microphone permission not granted. Open Daylight Mic to grant it.")
            return
        }
        val settings = SettingsStore.current
        level.value = 0f
        val r = Recorder(settings.maxRecordSeconds.coerceIn(5, 600), level) { toggle() }
        try {
            r.start()
        } catch (t: Throwable) {
            fail(t.message ?: "Microphone could not be opened")
            return
        }
        recorder = r
        MicForegroundService.start(app)
        setState(DictationState.Listening(System.currentTimeMillis()))
    }

    private fun finish() {
        val r = recorder ?: return
        recorder = null
        setState(DictationState.Transcribing)
        work = scope.launch {
            try {
                process(r, SettingsStore.current)
            } catch (t: Throwable) {
                if (t is kotlinx.coroutines.CancellationException) throw t
                Log.w(TAG, "dictation failed", t)
                fail(t.message ?: t.javaClass.simpleName)
            }
        }
    }

    private suspend fun process(r: Recorder, settings: AppSettings) {
        val samples = r.stop()
        level.value = 0f
        MicForegroundService.stop(app)
        if (Recorder.durationSeconds(samples) < 0.3f) {
            fail("Recording too short")
            return
        }
        if (Recorder.rms(samples) < 0.0005f) {
            fail("No audio captured. The microphone may be blocked for background apps.")
            return
        }

        val engine = engineFor(settings)
        var text = engine.transcribe(samples, settings.language.trim())
        if (text.isBlank()) {
            fail("No speech recognized")
            return
        }

        if (settings.cleanupEnabled) {
            setState(DictationState.Cleaning)
            val cleaner = CleanupClient(
                provider = settings.cleanupProvider,
                model = settings.cleanupModelOrDefault(),
                apiKey = settings.apiKey(settings.cleanupProvider),
                prompt = settings.cleanupPrompt,
            )
            text = cleaner.clean(text)
        }

        lastTranscript.value = text
        val delivery = deliver(text)
        TranscriptStore.add(text, engineLabel(settings), Recorder.durationSeconds(samples), delivery)
        setState(DictationState.Done(text, delivery))
        scheduleReset(2_000)
    }

    private fun engineLabel(settings: AppSettings): String = when (settings.engine) {
        EngineKind.LOCAL -> settings.localModel
        EngineKind.REMOTE -> "${settings.sttProvider.label} ${settings.sttModelOrDefault()}"
    }

    private fun engineFor(settings: AppSettings): TranscriptionEngine = when (settings.engine) {
        EngineKind.LOCAL -> {
            val model = ModelCatalog.byId(settings.localModel)
                ?: throw IllegalStateException("Unknown local model ${settings.localModel}")
            LocalWhisperEngine.modelFile = ModelManager.file(app, model)
            LocalWhisperEngine
        }
        EngineKind.REMOTE -> RemoteTranscriptionEngine(
            provider = settings.sttProvider,
            model = settings.sttModelOrDefault(),
            apiKey = settings.apiKey(settings.sttProvider),
        )
    }

    /** Always copies to the clipboard, then tries to put the text into the focused field. */
    private suspend fun deliver(text: String): Delivery {
        val clipboard = app.getSystemService(ClipboardManager::class.java)
        clipboard.setPrimaryClip(ClipData.newPlainText("Dictation", text))
        val service = DictationAccessibilityService.instance ?: return Delivery.CLIPBOARD
        return service.insertText(text) ?: Delivery.CLIPBOARD
    }

    private fun fail(message: String) {
        setState(DictationState.Failed(message))
        scheduleReset(4_000)
    }

    private fun scheduleReset(afterMs: Long) {
        resetJob?.cancel()
        resetJob = scope.launch {
            delay(afterMs)
            synchronized(lock) {
                if (_state.value is DictationState.Done || _state.value is DictationState.Failed) {
                    setState(DictationState.Idle)
                }
            }
        }
    }

    private fun setState(next: DictationState) {
        _state.value = next
    }
}
