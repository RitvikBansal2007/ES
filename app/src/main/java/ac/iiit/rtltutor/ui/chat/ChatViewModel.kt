package ac.iiit.rtltutor.ui.chat

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import ac.iiit.rtltutor.ai.GenieWrapper
import ac.iiit.rtltutor.ai.LlamaServerManager
import ac.iiit.rtltutor.models.ChatMessage
import java.util.UUID

sealed class ServerState {
    object Idle      : ServerState()
    object Starting  : ServerState()
    data class Log(val line: String) : ServerState()
    object Ready     : ServerState()
    data class Error(val msg: String) : ServerState()
}

class ChatViewModel(application: Application) : AndroidViewModel(application) {

    private val serverManager = LlamaServerManager(application)
    private val genie         = GenieWrapper(application)

    // Master list mutated directly — snapshot into LiveData atomically
    private val messageList = mutableListOf<ChatMessage>()

    private val _messages    = MutableLiveData<List<ChatMessage>>(emptyList())
    val messages: LiveData<List<ChatMessage>> = _messages

    val isTyping             = MutableLiveData(false)

    private val _serverState = MutableLiveData<ServerState>(ServerState.Idle)
    val serverState: LiveData<ServerState> = _serverState

    private val history      = mutableListOf<Map<String, String>>()

    // ─────────────────────────────────────────────────────────────────────────

    /** Called when Chat screen opens. Starts the server if not already running. */
    fun startServer() {
        if (serverManager.isRunning()) {
            _serverState.value = ServerState.Ready
            return
        }
        _serverState.value = ServerState.Starting

        serverManager.start(
            onReady = {
                _serverState.postValue(ServerState.Ready)
            },
            onError = { msg ->
                _serverState.postValue(ServerState.Error(msg))
            },
            onLog = { line ->
                _serverState.postValue(ServerState.Log(line))
            }
        )
    }

    fun sendMessage(userText: String) {
        if (userText.isBlank()) return

        // 1. User message — setValue (main thread, synchronous)
        val userMsg = chatMsg(userText, isFromAI = false)
        messageList.add(userMsg)
        _messages.value = messageList.toList()

        history.add(mapOf("role" to "user", "content" to userText))
        isTyping.value = true

        // 2. Empty AI placeholder — synchronous
        val aiId = UUID.randomUUID().toString()
        messageList.add(chatMsg("", isFromAI = true, id = aiId))
        _messages.value = messageList.toList()

        // 3. Stream
        var accumulated = ""
        genie.generate(
            messages  = history.toList(),
            onToken   = { token ->
                accumulated += token
                updateMsg(aiId, accumulated)
            },
            onComplete = {
                isTyping.postValue(false)
                history.add(mapOf("role" to "assistant", "content" to accumulated))
            },
            onError = { errMsg ->
                isTyping.postValue(false)
                updateMsg(aiId, errMsg)
                if (history.lastOrNull()?.get("role") == "user") history.removeLastOrNull()
                _serverState.postValue(ServerState.Error(errMsg))
            }
        )
    }

    fun clearChat() {
        history.clear()
        messageList.clear()
        _messages.value = emptyList()
    }

    // ─────────────────────────────────────────────────────────────────────────

    private fun chatMsg(
        content: String,
        isFromAI: Boolean,
        id: String = UUID.randomUUID().toString()
    ) = ChatMessage(
        id = id, userId = if (isFromAI) "ai" else "user",
        content = content, isFromAI = isFromAI,
        bloomLevel = 0, kolbStage = "",
        timestamp = System.currentTimeMillis()
    )

    private fun updateMsg(id: String, content: String) {
        val idx = messageList.indexOfLast { it.id == id }
        if (idx >= 0) {
            messageList[idx] = messageList[idx].copy(content = content)
            _messages.postValue(messageList.toList())
        }
    }

    override fun onCleared() {
        super.onCleared()
        // Don't stop the server — keep it running while app is alive
        genie.unload()
    }
}
