package dev.bennyb.daysay.model

import android.content.Context
import dev.bennyb.daysay.net.ApiException
import dev.bennyb.daysay.net.Http
import dev.bennyb.daysay.settings.Provider
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

/** One entry in the model list. Local whisper models and remote providers share it. */
sealed interface Model {
    val id: String
    /** The plain name shown everywhere: Small, Medium, Groq. */
    val name: String
    /** One line under the name: language, size, or the remote model id. */
    val detail: String
    val isRemote: Boolean
}

/** Which native engine loads and runs a [LocalModel]. See jni.c for the two JNI bridges. */
enum class Engine { WHISPER, PARAKEET }

data class LocalModel(
    override val id: String,
    override val name: String,
    val language: String,
    val sizeMb: Int,
    val note: String,
    val engine: Engine = Engine.WHISPER,
    private val fileNameOverride: String? = null,
    private val urlOverride: String? = null,
) : Model {
    override val isRemote: Boolean get() = false
    override val detail: String get() = "$language  ·  $sizeMb MB  ·  $note"
    val fileName: String get() = fileNameOverride ?: "ggml-$id.bin"
    val url: String get() = urlOverride ?: "https://huggingface.co/ggerganov/whisper.cpp/resolve/main/$fileName"
}

data class RemoteModel(val provider: Provider) : Model {
    override val id: String get() = ModelCatalog.remoteId(provider)
    override val name: String get() = provider.label
    override val isRemote: Boolean get() = true
    override val detail: String get() = "${provider.defaultSttModel}  ·  your key  ·  needs Wi-Fi"
}

sealed class DownloadState {
    data object Idle : DownloadState()
    data class Running(val modelId: String, val progress: Float) : DownloadState()
    data class Failed(val modelId: String, val message: String) : DownloadState()
}

object ModelCatalog {
    const val DEFAULT_ID = "base.en-q5_1"
    private const val REMOTE_PREFIX = "remote:"

    val local = listOf(
        LocalModel("tiny.en-q5_1", "Small", "English", 32, "fastest, drops words"),
        LocalModel("base.en-q5_1", "Medium", "English", 60, "recommended"),
        LocalModel("small.en-q5_1", "Large", "English", 190, "best quality, slow"),
        LocalModel("base-q5_1", "Medium multilingual", "Any language", 60, "recommended"),
        LocalModel("small-q5_1", "Large multilingual", "Any language", 190, "best quality, slow"),
        LocalModel(
            "parakeet-tdt-0.6b-v2-q8_0", "Parakeet", "English", 630, "fastest",
            engine = Engine.PARAKEET,
            fileNameOverride = "ggml-parakeet-tdt-0.6b-v2-q8_0.bin",
            urlOverride = "https://huggingface.co/ggml-org/parakeet-GGUF/resolve/main/ggml-parakeet-tdt-0.6b-v2-q8_0.bin",
        ),
    )
    val remote = Provider.entries.map { RemoteModel(it) }
    val all: List<Model> = local + remote

    fun byId(id: String): Model? = all.firstOrNull { it.id == id }
    fun localById(id: String): LocalModel? = local.firstOrNull { it.id == id }
    fun remoteId(provider: Provider): String = REMOTE_PREFIX + provider.name
    fun isRemote(id: String): Boolean = id.startsWith(REMOTE_PREFIX)
    fun remoteProvider(id: String): Provider? =
        if (isRemote(id)) Provider.entries.firstOrNull { it.name == id.removePrefix(REMOTE_PREFIX) } else null

    /** The plain name for an id, or the id itself for entries from older versions. */
    fun displayName(id: String): String = byId(id)?.name ?: id
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
