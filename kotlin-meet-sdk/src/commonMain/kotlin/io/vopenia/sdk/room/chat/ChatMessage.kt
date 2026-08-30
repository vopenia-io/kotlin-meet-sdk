package io.vopenia.sdk.room.chat

import io.vopenia.livekit.participant.chat.ChatMessage as InternalChatMessage

/**
 * Chat message exchanged in a [io.vopenia.sdk.room.Room]. Mirrors LiveKit
 * Components' wire format (id, timestamp, message, deleted, generated, editTimestamp)
 * so that Web (`@livekit/components-react`) and Mobile clients in the same room
 * exchange messages transparently.
 *
 * No backend persistence — history is a property of the consumer (UI) layer.
 */
data class ChatMessage(
    val id: String,
    val author: String?,
    val text: String,
    val timestamp: Long,
    val editTimestamp: Long? = null,
    val deleted: Boolean = false,
    val generated: Boolean = false
)

internal fun InternalChatMessage.toFacade(): ChatMessage = ChatMessage(
    id = id,
    author = senderIdentity,
    text = message,
    timestamp = timestamp,
    editTimestamp = editTimestamp,
    deleted = deleted,
    generated = generated
)
