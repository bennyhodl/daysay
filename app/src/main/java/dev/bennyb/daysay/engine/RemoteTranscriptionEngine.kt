package dev.bennyb.daysay.engine

import android.util.Base64
import dev.bennyb.daysay.audio.Wav
import dev.bennyb.daysay.net.ApiException
import dev.bennyb.daysay.net.Http
import dev.bennyb.daysay.settings.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

class RemoteTranscriptionEngine(
    private val provider: Provider,
    private val model: String,
    private val apiKey: String,
) : TranscriptionEngine {

    override suspend fun transcribe(samples: ShortArray, language: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw ApiException("No API key set for ${provider.label}")
        val wav = Wav.encode(samples)
        if (provider.hasTranscriptionEndpoint) viaTranscriptionEndpoint(wav, language)
        else viaChatCompletion(wav, language)
    }

    /** OpenAI-compatible POST /audio/transcriptions (Groq, OpenAI). */
    private fun viaTranscriptionEndpoint(wav: ByteArray, language: String): String {
        val body = MultipartBody.Builder().setType(MultipartBody.FORM)
            .addFormDataPart("file", "speech.wav", wav.toRequestBody("audio/wav".toMediaType()))
            .addFormDataPart("model", model)
            .addFormDataPart("response_format", "json")
            .apply { if (language.isNotBlank()) addFormDataPart("language", language) }
            .build()
        val request = Request.Builder()
            .url("${provider.baseUrl}/audio/transcriptions")
            .header("Authorization", "Bearer $apiKey")
            .post(body)
            .build()
        val json = JSONObject(Http.execute(request))
        return json.optString("text").trim()
    }

    /** Chat completion with an input_audio content part (OpenRouter). */
    private fun viaChatCompletion(wav: ByteArray, language: String): String {
        val langHint = if (language.isNotBlank()) " The speech is in language code '$language'." else ""
        val instruction = "Transcribe this audio exactly as spoken.$langHint " +
            "Output only the transcript text. No preamble, no quotes, no commentary."
        val content = JSONArray()
            .put(JSONObject().put("type", "text").put("text", instruction))
            .put(
                JSONObject().put("type", "input_audio").put(
                    "input_audio",
                    JSONObject()
                        .put("data", Base64.encodeToString(wav, Base64.NO_WRAP))
                        .put("format", "wav")
                )
            )
        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0)
            .put("messages", JSONArray().put(JSONObject().put("role", "user").put("content", content)))
        val request = Request.Builder()
            .url("${provider.baseUrl}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://github.com/bennyhodl/daysay")
            .header("X-Title", "Daysay")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        return ChatResponse.text(Http.execute(request))
    }
}

object ChatResponse {
    fun text(body: String): String {
        val json = JSONObject(body)
        val choices = json.optJSONArray("choices") ?: throw ApiException("No choices in response")
        if (choices.length() == 0) throw ApiException("Empty choices in response")
        val message = choices.getJSONObject(0).optJSONObject("message") ?: throw ApiException("No message in response")
        val content = message.opt("content")
        val text = when (content) {
            null -> ""
            is String -> content
            is JSONArray -> buildString {
                for (i in 0 until content.length()) {
                    val part = content.optJSONObject(i) ?: continue
                    if (part.optString("type") == "text") append(part.optString("text"))
                }
            }
            else -> ""
        }
        return text.trim()
    }
}
