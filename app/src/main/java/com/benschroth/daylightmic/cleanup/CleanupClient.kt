package com.benschroth.daylightmic.cleanup

import com.benschroth.daylightmic.engine.ChatResponse
import com.benschroth.daylightmic.net.ApiException
import com.benschroth.daylightmic.net.Http
import com.benschroth.daylightmic.settings.Provider
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
            .header("HTTP-Referer", "https://github.com/bennyhodl/daylight-mic")
            .header("X-Title", "Daylight Mic")
            .post(payload.toString().toRequestBody("application/json".toMediaType()))
            .build()
        val cleaned = ChatResponse.text(Http.execute(request))
        // A model that answers with nothing must not wipe the user's words.
        cleaned.ifBlank { transcript }
    }
}
