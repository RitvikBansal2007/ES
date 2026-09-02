package ac.iiit.rtltutor.ai

import android.content.Context
import android.util.Log
import java.io.File
import java.io.IOException
import java.net.InetSocketAddress
import java.net.Socket

/**
 * LlamaServerManager — starts llama-server as a child process of the app.
 *
 * WHY: SELinux Enforcing on Android blocks connections from untrusted_app domain
 * to processes started by the shell domain (even on loopback 127.0.0.1).
 * Running the server as the app's own child process puts it in the same domain,
 * making loopback connections work.
 *
 * SETUP (one-time, run from Mac terminal):
 *   adb push /path/to/llama-server /data/local/tmp/llama-server
 *   adb push /path/to/Llama-3.2-3B-Instruct-Q4_K_M.gguf /data/local/tmp/
 *   (already done — they're at /data/local/tmp/)
 *
 * The app copies the binary to its private filesDir and starts it from there.
 */
class LlamaServerManager(private val context: Context) {

    companion object {
        private const val TAG = "LlamaServerMgr"
        const val PORT = 8080
        const val SERVER_URL = "http://127.0.0.1:$PORT"

        // Source binary pushed via adb (world-readable)
        private const val SRC_BINARY  = "/data/local/tmp/llama-server"
        private const val MODEL_PATH  = "/data/local/tmp/Llama-3.2-3B-Instruct-Q4_K_M.gguf"
        private const val LIB_PATH    = "/data/local/tmp"

        // Destination inside app's private dir (W^X compliant, same app UID)
        private const val BINARY_NAME = "llama-server"
    }

    private var serverProcess: Process? = null
    @Volatile private var isRunning = false

    val destBinary: File get() = File(context.filesDir, BINARY_NAME)

    // ─────────────────────────────────────────────────────────────────────────
    // Public API
    // ─────────────────────────────────────────────────────────────────────────

    fun isRunning(): Boolean = isRunning && isPortOpen()

    /**
     * Start the server. Calls [onReady] when port 8080 accepts connections.
     * Calls [onError] with a human-readable message on failure.
     * Non-blocking — runs everything on a background thread.
     */
    fun start(
        onReady: () -> Unit,
        onError: (String) -> Unit,
        onLog: (String) -> Unit = {}
    ) {
        if (isRunning()) {
            Log.d(TAG, "Server already running, skipping start")
            onReady()
            return
        }
        Log.d(TAG, "start() called — beginning server setup")
        _serverState = ServerState.Starting

        Thread {
            try {
                // 1. Verify model exists
                val model = File(MODEL_PATH)
                Log.d(TAG, "Checking model at $MODEL_PATH — exists=${model.exists()} size=${model.length()}")
                if (!model.exists()) {
                    val msg = "Model not found at $MODEL_PATH.\n\nRun:\nadb push Llama-3.2-3B-Instruct-Q4_K_M.gguf /data/local/tmp/"
                    Log.e(TAG, msg)
                    onError(msg); return@Thread
                }

                // 2. Copy binary to app private dir
                val src = File(SRC_BINARY)
                Log.d(TAG, "Checking binary at $SRC_BINARY — exists=${src.exists()} size=${src.length()}")
                if (!src.exists()) {
                    val msg = "llama-server binary not found at $SRC_BINARY"
                    Log.e(TAG, msg)
                    onError(msg); return@Thread
                }

                if (!destBinary.exists() || destBinary.length() != src.length()) {
                    Log.d(TAG, "Copying binary: ${src.absolutePath} → ${destBinary.absolutePath}")
                    onLog("Copying binary to app dir…")
                    try {
                        src.copyTo(destBinary, overwrite = true)
                        Log.d(TAG, "Copy complete: ${destBinary.length()} bytes")
                    } catch (e: Exception) {
                        val msg = "Copy failed: ${e.message}"
                        Log.e(TAG, msg, e)
                        onError(msg); return@Thread
                    }
                } else {
                    Log.d(TAG, "Binary already up-to-date at ${destBinary.absolutePath}")
                }

                val execOk = destBinary.setExecutable(true, false)
                Log.d(TAG, "setExecutable result: $execOk, canExecute=${destBinary.canExecute()}")

                // 3. Kill any existing server on that port
                stopExistingOnPort()

                // 4. Launch
                Log.d(TAG, "Starting ProcessBuilder: ${destBinary.absolutePath} -ngl 0")
                onLog("Starting llama-server (CPU mode)…")

                val pb = ProcessBuilder(
                    destBinary.absolutePath,
                    "-m", MODEL_PATH,
                    "-ngl", "0",
                    "--host", "127.0.0.1",
                    "--port", "$PORT",
                    "--ctx-size", "2048",
                    "--threads", "8"
                ).apply {
                    environment()["LD_LIBRARY_PATH"] = "$LIB_PATH:/system/lib64"
                    redirectErrorStream(true)
                    directory(context.filesDir)
                }

                serverProcess = pb.start()
                isRunning = true
                Log.d(TAG, "Process started: PID=${serverProcess?.let { it.javaClass.name }}")

                // 5. Drain stdout
                Thread {
                    serverProcess?.inputStream?.bufferedReader()?.forEachLine { line ->
                        Log.d(TAG, "server>> $line")
                        onLog(line)
                    }
                    isRunning = false
                    Log.d(TAG, "Server stdout stream ended")
                }.start()

                // 6. Poll for readiness
                Log.d(TAG, "Polling port $PORT for readiness…")
                onLog("Loading model — this takes ~30s…")
                for (i in 1..90) {
                    Thread.sleep(1000)
                    if (isPortOpen()) {
                        Log.d(TAG, "✅ Server ready after ${i}s")
                        onReady()
                        return@Thread
                    }
                    if (serverProcess?.isAlive == false) {
                        val msg = "Server process exited after ${i}s. Check logcat tag=LlamaServerMgr"
                        Log.e(TAG, msg)
                        isRunning = false
                        onError(msg); return@Thread
                    }
                }
                val msg = "Port $PORT not responding after 90s"
                Log.e(TAG, msg)
                onError(msg)

            } catch (e: IOException) {
                val msg = "exec failed: ${e.message}"
                Log.e(TAG, msg, e)
                onError("$msg\n\nTry: adb shell setenforce 0")
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error: ${e.message}", e)
                onError("Error: ${e.message}")
            }
        }.start()
    }

    private enum class ServerState { Idle, Starting }

    fun stop() {
        serverProcess?.destroy()
        serverProcess = null
        isRunning = false
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun isPortOpen(): Boolean {
        return try {
            Socket().use { s ->
                s.connect(InetSocketAddress("127.0.0.1", PORT), 500)
                true
            }
        } catch (_: Exception) { false }
    }

    private fun stopExistingOnPort() {
        try {
            // Best-effort kill of any existing process on port 8080
            Runtime.getRuntime().exec(arrayOf("sh", "-c", "pkill -f llama-server")).waitFor()
            Thread.sleep(500)
        } catch (_: Exception) {}
    }
}
