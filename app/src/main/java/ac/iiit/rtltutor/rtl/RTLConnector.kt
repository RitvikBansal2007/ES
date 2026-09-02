package ac.iiit.rtltutor.rtl

import ac.iiit.rtltutor.models.ExperimentState
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.WebSocket
import okhttp3.WebSocketListener

/**
 * RTLConnector — manages WebSocket connection to the RTL lab hardware/server.
 */
class RTLConnector {

    private val client = OkHttpClient()
    private var webSocket: WebSocket? = null
    private var _latestState: ExperimentState? = null

    /** The most recently received experiment state from the lab. */
    val latestState: ExperimentState? get() = _latestState

    private var onStateUpdate: ((ExperimentState) -> Unit)? = null

    /**
     * Connect to RTL lab server via WebSocket.
     * @param url   WebSocket URL e.g. "ws://rtl.iiit.ac.in/ws"
     * @param token JWT auth token
     */
    fun connect(url: String, token: String, onUpdate: (ExperimentState) -> Unit) {
        onStateUpdate = onUpdate
        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        webSocket = client.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                // TODO: parse JSON into ExperimentState using Gson
                // Stub: create dummy state
                val state = ExperimentState(
                    voltage = 5.0,
                    current = 2.5,
                    frequency = 100.0,
                    timestamp = System.currentTimeMillis(),
                    rawJson = text
                )
                _latestState = state
                onUpdate(state)
            }
        })
    }

    /**
     * Disconnect the WebSocket and release resources.
     */
    fun disconnect() {
        webSocket?.close(1000, "User disconnected")
        webSocket = null
    }
}
