package dev.bennyb.daysay.cleanup

import dev.bennyb.daysay.engine.ChatResponse
import dev.bennyb.daysay.net.ApiException
import dev.bennyb.daysay.net.Http
import dev.bennyb.daysay.settings.Provider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject

/** Optional second pass: sends the raw transcript to a chat model with a cleanup prompt. */
class CleanupClient(
    private val provider: Provider,
    private val model: String,
    private val apiKey: String,
    private val prompt: String,
) {
    suspend fun clean(transcript: String): String = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) throw ApiException("No API key set for ${provider.label} (cleanup)")
        val payload = JSONObject()
            .put("model", model)
            .put("temperature", 0.2)
            .put(
                "messages",
                JSONArray()
                    .put(JSONObject().put("role", "system").put("content", prompt))
                    .put(JSONObject().put("role", "user").put("content", transcript))
            )
        val request = Request.Builder()
            .url("${provider.baseUrl}/chat/completions")
            .header("Authorization", "Bearer $apiKey")
            .header("HTTP-Referer", "https://github.com/bennyhodl/daysay")
            .header("X-Title", "Daysay")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val cleaned = ChatResponse.text(Http.execute(request))
        // A model that answers with nothing must not wipe the user's words.
        cleaned.ifBlank { transcript }
    }
}
