package dev.batipy.rungo.ui.chat

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import dev.batipy.rungo.R
import dev.batipy.rungo.data.chat.ChatRepository
import dev.batipy.rungo.data.network.dto.ChatFrame
import dev.batipy.rungo.data.network.dto.ChatMessageDto
import dev.batipy.rungo.data.network.dto.ChatOutgoingMessage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.json.Json
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener

sealed interface ChatUiState {
    data object Connecting : ChatUiState
    data class Error(val message: String) : ChatUiState
    data class Ready(
        val messages: List<ChatMessageDto> = emptyList()
    ) : ChatUiState
}

class ChatViewModel(
    private val orderId: Int,
    val currentUserId: Int,
    private val chatRepository: ChatRepository,
    private val context: Context
) : ViewModel() {

    private val json = Json { ignoreUnknownKeys = true }
    private var socket: WebSocket? = null

    // Give a connection failure one silent retry before bothering the user
    // with an error screen — most "failures" right after opening chat are a
    // transient hiccup, not a real outage. Reset once we've actually seen a
    // successful connection, so a later drop gets its own fresh attempt.
    private var autoRetried = false

    private val _uiState = MutableStateFlow<ChatUiState>(ChatUiState.Connecting)
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    private val _message = MutableStateFlow<String?>(null)
    val message: StateFlow<String?> = _message.asStateFlow()

    private val listener = object : WebSocketListener() {
        override fun onMessage(webSocket: WebSocket, text: String) {
            if (webSocket !== socket) return
            val frame = try {
                json.decodeFromString(ChatFrame.serializer(), text)
            } catch (e: Exception) {
                return
            }
            when (frame.type) {
                "history" -> {
                    autoRetried = false
                    _uiState.value = ChatUiState.Ready(messages = frame.messages.orEmpty())
                }
                "message" -> {
                    val incoming = frame.message ?: return
                    updateReady { it.copy(messages = it.messages + incoming) }
                }
                "error" -> _message.value = context.getString(R.string.chat_send_error)
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            // Fires for the *old* socket too when connect() closes it to
            // replace it with a new one — ignore anything not from the
            // socket we're currently tracking.
            if (webSocket !== socket) return
            if (_uiState.value is ChatUiState.Ready && code == 1000) return
            handleDisconnect(code)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (webSocket !== socket) return
            handleDisconnect(response?.code ?: -1)
        }
    }

    init {
        connect()
    }

    private fun handleDisconnect(code: Int) {
        if (code == 4401 || code == 4403) {
            _uiState.value = ChatUiState.Error(closeReasonMessage(code))
            return
        }
        if (!autoRetried) {
            autoRetried = true
            openSocket()
        } else {
            _uiState.value = ChatUiState.Error(closeReasonMessage(code))
        }
    }

    private fun closeReasonMessage(code: Int): String = when (code) {
        4401, 4403 -> context.getString(R.string.chat_unavailable_error)
        else -> context.getString(R.string.chat_connection_error)
    }

    /** Public entry point (initial connect + the Error screen's retry button)
     * — always gets its own fresh silent-retry budget. */
    fun connect() {
        autoRetried = false
        openSocket()
    }

    private fun openSocket() {
        socket?.close(1000, null)
        _uiState.value = ChatUiState.Connecting
        socket = chatRepository.connect(orderId, listener)
    }

    /** Returns true if the message was handed off to the socket successfully. */
    fun sendMessage(text: String): Boolean {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return true
        val payload = json.encodeToString(ChatOutgoingMessage.serializer(), ChatOutgoingMessage(trimmed))
        val sent = socket?.send(payload) ?: false
        if (!sent) {
            _message.value = context.getString(R.string.chat_send_error)
        }
        return sent
    }

    fun consumeMessage() {
        _message.value = null
    }

    private inline fun updateReady(block: (ChatUiState.Ready) -> ChatUiState.Ready) {
        val current = _uiState.value as? ChatUiState.Ready ?: return
        _uiState.value = block(current)
    }

    override fun onCleared() {
        super.onCleared()
        socket?.close(1000, null)
    }

    class Factory(
        private val orderId: Int,
        private val currentUserId: Int,
        private val chatRepository: ChatRepository,
        private val context: Context
    ) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            return ChatViewModel(orderId, currentUserId, chatRepository, context) as T
        }
    }
}
