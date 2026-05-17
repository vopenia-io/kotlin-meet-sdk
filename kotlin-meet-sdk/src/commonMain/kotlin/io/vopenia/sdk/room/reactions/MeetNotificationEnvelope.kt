package io.vopenia.sdk.room.reactions

import kotlinx.serialization.Serializable

/**
 * JSON envelope used by Meet Web (`useNotifyParticipants` + `MainNotificationToast`)
 * for all `room.localParticipant.publishData()` notifications: reactions,
 * recording start/stop announcements, etc. The shape is `{type, ...extra}` with
 * the discriminator `type` mapping to one of `NotificationType` values from
 * `src/frontend/src/features/notifications/NotificationType.ts`.
 *
 * For reactions specifically (`type = "reactionReceived"`), the payload nests
 * an `data: {emoji}` object — see [MeetReactionData].
 */
@Serializable
internal data class MeetNotificationEnvelope(
    val type: String,
    val data: MeetReactionData? = null
)

@Serializable
internal data class MeetReactionData(
    val emoji: String
)
