package dev.batipy.rungo.data.network.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ChatMessageDto(
    val id: Int,
    val sender: Int,
    @SerialName("sender_name") val senderName: String,
    @SerialName("sender_role") val senderRole: String,
    val text: String,
    @SerialName("is_read") val isRead: Boolean,
    @SerialName("created_at") val createdAt: String
)

// One shape covers all three frame kinds the server sends over the socket
// (history/message/error) — simpler than a polymorphic serializer for three
// mutually-exclusive optional fields.
@Serializable
data class ChatFrame(
    val type: String,
    val messages: List<ChatMessageDto>? = null,
    val message: ChatMessageDto? = null,
    val detail: JsonElement? = null
)

@Serializable
data class ChatOutgoingMessage(val text: String)
