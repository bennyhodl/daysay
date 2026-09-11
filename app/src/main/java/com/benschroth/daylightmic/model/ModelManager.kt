package com.benschroth.daylightmic.model

import android.content.Context
import com.benschroth.daylightmic.net.ApiException
import com.benschroth.daylightmic.net.Http
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import okhttp3.Request
import java.io.File

data class LocalModel(val id: String, val label: String, val sizeMb: Int) {
    val fileName: String get() = "ggml-$id.bin"
    val url: String get() = "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$fileName"
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Running(val modelId: String, val progress: Float) : DownloadState()
    data class Failed(val modelId: String, val message: String) : DownloadState()
}

object ModelCatalog {
    val models = listOf(
        LocalModel("tiny.en-q5_1", "Tiny English. Fastest, drops words", 32),
        LocalModel("base.en-q5_1", "Base English. Recommended", 60),
        LocalModel("small.en-q5_1", "Small English. Best quality, slow", 190),
        LocalModel("base-q5_1", "Base multilingual", 60),
        LocalModel("small-q5_1", "Small multilingual. Slow", 190),
    )

    fun byId(id: String): LocalModel? = models.firstOrNull { it.id == id }
}

object ModelManager {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var job: Job? = null
    private val _download = MutableStateFlow<DownloadState>(DownloadState.Idle)
    val download: StateFlow<DownloadState> get() = _download

    /** Bumped whenever the set of downloaded files changes, so the UI re-reads the file system. */
    val revision = MutableStateFlow(0)

    fun dir(context: Context): File = File(context.filesDir, "models").also { it.mkdirs() }
    fun file(context: Context, model: LocalModel): File = File(dir(context), model.fileName)
    fun isDownloaded(context: Context, model: LocalModel): Boolean = file(context, model).let { it.exists() && it.length() > 1_000_000 }

    /** Starts a download in the background. Progress and failure are reported through [download]. */
    fun startDownload(context: Context, model: LocalModel) {
        if (_download.value is DownloadState.Running) return
        val app = context.applicationContext
        job = scope.launch { runCatching { download(app, model) } }
    }

    fun cancelDownload() {
        job?.cancel()
        job = null
        _download.value = DownloadState.Idle
    }

    fun dismissFailure() {
        if (_download.value is DownloadState.Failed) _download.value = DownloadState.Idle
    }

    fun delete(context: Context, model: LocalModel) {
        file(context, model).delete()
        revision.value++
    }

    suspend fun download(context: Context, model: LocalModel) = withContext(Dispatchers.IO) {
        val target = file(context, model)
        val tmp = File(target.path + ".part")
        _download.value = DownloadState.Running(model.id, 0f)
        try {
            val request = Request.Builder().url(model.url).build()
            Http.client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) throw ApiException("HTTP ${response.code} while downloading ${model.fileName}")
                val body = response.body
                val total = body.contentLength()
                body.byteStream().use { input ->
                    tmp.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        var done = 0L
                        while (true) {
                            ensureActive()
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            if (total > 0) _download.value = DownloadState.Running(model.id, done.toFloat() / total)
                        }
                    }
                }
            }
            if (!tmp.renameTo(target)) throw ApiException("Could not move downloaded model into place")
            _download.value = DownloadState.Idle
            revision.value++
        } catch (t: Throwable) {
            tmp.delete()
            _download.value = DownloadState.Failed(model.id, t.message ?: t.javaClass.simpleName)
            throw t
        }
    }
}
