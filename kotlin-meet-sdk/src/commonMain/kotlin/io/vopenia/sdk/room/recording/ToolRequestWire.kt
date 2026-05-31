package io.vopenia.sdk.room.recording

import io.vopenia.api.rooms.models.ApiRecordingMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Wire payloads carried inside the Meet notification envelope
 * `{type, data: <one of these>}` on the default data channel:
 *
 *  - `toolStartRequested`   — non-admin asks an admin to start a recording.
 *  - `toolStartAnswered`    — admin replies accept / reject.
 *  - `toolStartCancelled`   — demander withdraws a pending request.
 *
 * Compatible with the existing Meet Web `NotificationPayload` convention,
 * even though Meet Web hasn't shipped these types yet (`§8.2` proposal).
 * Adding them here doesn't break interop: Web's `MainNotificationToast`
 * default-cases unknown types.
 */
internal const val TOOL_START_REQUESTED = "toolStartRequested"
internal const val TOOL_START_ANSWERED = "toolStartAnswered"
internal const val TOOL_START_CANCELLED = "toolStartCancelled"

@Serializable
internal data class ToolStartRequestedPayload(
    @SerialName("request_id")
    val requestId: String,
    @SerialName("requester_name")
    val requesterName: String? = null,
    val mode: ApiRecordingMode,
    val timestamp: Long
)

@Serializable
internal data class ToolStartAnsweredPayload(
    @SerialName("request_id")
    val requestId: String,
    val accepted: Boolean
)

@Serializable
internal data class ToolStartCancelledPayload(
    @SerialName("request_id")
    val requestId: String
)
