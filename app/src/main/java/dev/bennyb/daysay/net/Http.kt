package dev.bennyb.daysay.net

import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONObject
import java.io.IOException
import java.util.concurrent.TimeUnit

class ApiException(message: String) : IOException(message)

object Http {
    val client: OkHttpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(15, TimeUnit.SECONDS)
            .readTimeout(120, TimeUnit.SECONDS)
            .writeTimeout(120, TimeUnit.SECONDS)
            .build()
    }

    /** Executes the request and returns the body. Turns API errors into a readable message. */
    fun execute(request: Request): String {
        val response: Response = client.newCall(request).execute()
        response.use {
            val body = it.body.string()
            if (!it.isSuccessful) throw ApiException(describeError(it.code, body))
            return body
        }
    }

    private fun describeError(code: Int, body: String): String {
        val detail = runCatching {
            val json = JSONObject(body)
            val err = json.opt("error")
            when (err) {
                is JSONObject -> err.optString("message").ifBlank { err.toString() }
                is String -> err
                else -> json.optString("message")
            }
        }.getOrNull()?.takeIf { it.isNotBlank() } ?: body.take(200)
        return "HTTP $code: $detail"
    }
}
