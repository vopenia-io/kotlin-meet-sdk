package io.vopenia.sdk.room.recording

import kotlinx.serialization.Serializable

/**
 * A pending "Start recording" / "Start transcription" request issued by a
 * non-admin participant to the room's admins/owners. Reflects Meet Web's
 * `RestrictedToolAccess` flow where the tile is gated on admin approval.
 *
 * Wire transport: JSON envelope on the default data channel
 * (`{type:"toolStartRequested",data:{requestId,...}}`). Conventions live in
 * [io.vopenia.sdk.room.Room].
 */
@Serializable
data class ToolStartRequest(
    /** Server-stable identifier (UUID) the admin will pass back to answer. */
    val requestId: String,
    /** LiveKit identity of the participant who issued the request. */
    val requesterIdentity: String,
    /** Display name of the requester, when available — convenience for UI. */
    val requesterName: String?,
    /** Mode requested: transcript or screen recording. */
    val mode: RecordingMode,
    /** ms-epoch when the request was issued (Meet uses this to expire stale UI). */
    val timestamp: Long
)

/**
 * Local status of a request the **current participant** issued via
 * [io.vopenia.sdk.room.Room.requestToolStart]. Drives the demander UI.
 *
 * State machine: `Pending → (Accepted | Rejected | Expired)`. Terminal states
 * are sticky — the StateFlow stays at the final value.
 */
sealed class ToolRequestStatus {
    /** Sent on the wire, waiting for an admin to act. */
    object Pending : ToolRequestStatus()
    /** An admin accepted; the tool was started server-side. */
    object Accepted : ToolRequestStatus()
    /** An admin explicitly rejected. */
    object Rejected : ToolRequestStatus()
    /** No admin answered within the timeout, or the admin disconnected. */
    object Expired : ToolRequestStatus()
}
