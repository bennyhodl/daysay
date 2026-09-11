package dev.bennyb.daysay.engine

import android.util.Log
import dev.bennyb.daysay.audio.Recorder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Runs whisper.cpp on the device. The context stays loaded between requests because loading
 * takes longer than a short transcription. All native calls run on one thread: whisper.cpp
 * contexts are not thread safe.
 */
object LocalWhisperEngine : TranscriptionEngine {
    private const val TAG = "LocalWhisper"
    private val dispatcher = Executors.newSingleThreadExecutor { Thread(it, "whisper") }.asCoroutineDispatcher()

    private var ptr = 0L
    private var loadedPath: String? = null

    @Volatile var modelFile: File? = null

    private val threads: Int
        get() = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    override suspend fun transcribe(samples: ShortArray, language: String): String = withContext(dispatcher) {
        val file = modelFile ?: throw IllegalStateException("No local model selected")
        if (!file.exists()) throw IllegalStateException("Model not downloaded: ${file.name}")
        ensureLoaded(file)
        // whisper.cpp needs at least one second of audio; pad short clips with silence.
        val floats = Recorder.toFloats(samples).let { if (it.size < 16_000) it.copyOf(16_000) else it }
        val started = System.currentTimeMillis()
        val text = WhisperLib.transcribe(ptr, threads, language, "", floats)
        Log.i(TAG, "transcribed ${floats.size / 16_000f}s in ${System.currentTimeMillis() - started} ms with $threads threads")
        text.trim()
    }

    private fun ensureLoaded(file: File) {
        if (ptr != 0L && loadedPath == file.absolutePath) return
        releaseLocked()
        Log.i(TAG, "loading ${file.name}; ${WhisperLib.systemInfo()}")
        ptr = WhisperLib.initContext(file.absolutePath)
        if (ptr == 0L) throw IllegalStateException("Could not load model ${file.name}")
        loadedPath = file.absolutePath
    }

    private fun releaseLocked() {
        if (ptr != 0L) {
            WhisperLib.freeContext(ptr)
            ptr = 0L
            loadedPath = null
        }
    }

    /** Frees the context from another engine's caller. Must hop to [dispatcher]: whisper.cpp contexts are single-thread. */
    suspend fun release() = withContext(dispatcher) { releaseLocked() }
}
