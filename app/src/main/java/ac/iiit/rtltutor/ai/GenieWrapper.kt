package ac.iiit.rtltutor.ai

import android.content.Context
import android.util.Log
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.util.concurrent.TimeUnit

/**
 * GenieWrapper — sends messages directly to the llama-server running on the device.
 *
 * Start the server via ADB before launching the app:
 *   adb shell "pkill -f llama-server"
 *   adb shell "LD_LIBRARY_PATH=/data/local/tmp /data/local/tmp/llama-server \
 *     -m /data/local/tmp/Llama-3.2-3B-Instruct-Q4_K_M.gguf \
 *     -ngl 99 --host 127.0.0.1 --port 8080 --ctx-size 2048 &"
 *
 * The app connects directly to 127.0.0.1:8080 (device localhost).
 * No adb forward needed for the app itself.
 */
class GenieWrapper(private val context: Context) {

    companion object {
        private const val TAG = "GenieWrapper"
        const val SERVER_URL = "http://127.0.0.1:8080"
        private const val CHAT_ENDPOINT = "$SERVER_URL/v1/chat/completions"
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(120, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    /** Check if the server is reachable. Called synchronously. */
    fun isServerRunning(): Boolean {
        return try {
            val req = Request.Builder().url("$SERVER_URL/v1/models").get().build()
            client.newCall(req).execute().use { it.isSuccessful }
        } catch (e: Exception) {
            false
        }
    }

    /** No-op — server manages its own lifecycle. */
    fun loadModel(modelPath: String) {
        Log.d(TAG, "GenieWrapper ready. Server: $SERVER_URL")
    }

    /**
     * Stream a completion from the server.
     * [messages] is a list of {role, content} maps.
     * Each token is delivered via [onToken].
     * [onError] is called with a human-readable message on failure.
     */
    fun generate(
        messages: List<Map<String, String>>,
        onToken: (String) -> Unit,
        onComplete: () -> Unit,
        onError: (String) -> Unit
    ) {
        Thread {
            try {
                val messagesArray = JSONArray().apply {
                    messages.forEach { msg ->
                        put(JSONObject().apply {
                            put("role", msg["role"] ?: "user")
                            put("content", msg["content"] ?: "")
                        })
                    }
                }

                val body = JSONObject().apply {
                    put("messages", messagesArray)
                    put("stream", true)
                    put("max_tokens", 1024)
                    put("temperature", 0.7)
                }.toString().toRequestBody("application/json".toMediaType())

                val request = Request.Builder()
                    .url(CHAT_ENDPOINT)
                    .post(body)
                    .build()

                client.newCall(request).execute().use { response ->
                    if (!response.isSuccessful) {
                        onError("Server error: HTTP ${response.code}")
                        return@use
                    }

                    val reader = BufferedReader(
                        InputStreamReader(response.body!!.byteStream())
                    )

                    var line: String?
                    while (reader.readLine().also { line = it } != null) {
                        val raw = line ?: continue
                        if (!raw.startsWith("data: ")) continue
                        val payload = raw.removePrefix("data: ").trim()
                        if (payload == "[DONE]") break

                        try {
                            val chunk = JSONObject(payload)
                            val delta = chunk
                                .getJSONArray("choices")
                                .getJSONObject(0)
                                .getJSONObject("delta")
                            val token = delta.optString("content", "")
                            if (token.isNotEmpty()) onToken(token)
                        } catch (_: Exception) { /* skip malformed chunk */ }
                    }
                    onComplete()
                }
            } catch (e: Exception) {
                val msg = when {
                    e.message?.contains("refused") == true ->
                        "Cannot connect to llama-server.\n\nRun this in terminal:\nadb shell \"/data/local/tmp/llama-server -m /data/local/tmp/Llama-3.2-3B-Instruct-Q4_K_M.gguf -ngl 99 --host 127.0.0.1 --port 8080 &\""
                    e.message?.contains("timeout") == true ->
                        "Connection timed out. Is the server still running?"
                    else -> "Error: ${e.message}"
                }
                Log.e(TAG, "generate() failed: ${e.message}")
                onError(msg)
            }
        }.start()
    }

    fun unload() {
        Log.d(TAG, "GenieWrapper unloaded")
    }
}
