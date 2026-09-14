package dev.bennyb.daysay.engine

import android.util.Log
import dev.bennyb.daysay.audio.Recorder
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import java.util.concurrent.Executors

/**
 * Runs whisper.cpp's vendored Parakeet (TDT) support on the device. Detects language on its own;
 * `settings.language` is ignored. The context stays loaded between requests because loading takes
 * longer than a short transcription. All native calls run on one thread: parakeet.cpp contexts
 * are not thread safe.
 */
object LocalParakeetEngine : TranscriptionEngine {
    private const val TAG = "LocalParakeet"
    private val dispatcher = Executors.newSingleThreadExecutor { Thread(it, "parakeet") }.asCoroutineDispatcher()

    private var ptr = 0L
    private var loadedPath: String? = null

    @Volatile var modelFile: File? = null

    private val threads: Int
        get() = Runtime.getRuntime().availableProcessors().coerceIn(2, 4)

    override suspend fun transcribe(samples: ShortArray, language: String): String = withContext(dispatcher) {
        val file = modelFile ?: throw IllegalStateException("No local model selected")
        if (!file.exists()) throw IllegalStateException("Model not downloaded: ${file.name}")
        ensureLoaded(file)
        // whisper.cpp needs one second; keep the same floor here so both engines see the same audio.
        val floats = Recorder.toFloats(samples).let { if (it.size < 16_000) it.copyOf(16_000) else it }
        val started = System.currentTimeMillis()
        val text = ParakeetLib.transcribe(ptr, threads, floats)
        Log.i(TAG, "transcribed ${floats.size / 16_000f}s in ${System.currentTimeMillis() - started} ms with $threads threads")
        text.trim()
    }

    private fun ensureLoaded(file: File) {
        if (ptr != 0L && loadedPath == file.absolutePath) return
        releaseLocked()
        Log.i(TAG, "loading ${file.name}; ${ParakeetLib.systemInfo()}")
        ptr = ParakeetLib.initContext(file.absolutePath)
        if (ptr == 0L) throw IllegalStateException("Could not load model ${file.name}")
        loadedPath = file.absolutePath
    }

    private fun releaseLocked() {
        if (ptr != 0L) {
            ParakeetLib.freeContext(ptr)
            ptr = 0L
            loadedPath = null
        }
    }

    /** Frees the context from another engine's caller. Must hop to [dispatcher]: parakeet.cpp contexts are single-thread. */
    suspend fun release() = withContext(dispatcher) { releaseLocked() }
}
