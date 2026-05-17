package io.vopenia.sdk.recording

import io.vopenia.api.recordings.models.ApiRecording
import io.vopenia.api.recordings.models.ApiRecordingStatus
import io.vopenia.api.rooms.models.ApiRecordingMode
import io.vopenia.sdk.room.recording.RecordingMode

/**
 * Lifecycle state of a recording, projected from the backend.
 */
enum class RecordingStatus {
    Initiated,
    Active,
    Stopped,
    Saved,
    Aborted,
    FailedToStart,
    FailedToStop,
    NotificationSucceeded;

    /** True when the recording's lifecycle has terminated (success or failure). */
    val isFinal: Boolean
        get() = this == Stopped ||
                this == Saved ||
                this == Aborted ||
                this == FailedToStart ||
                this == FailedToStop
}

/**
 * SDK-side view of a past or in-flight recording. Exposed via [io.vopenia.sdk.Session.recordings].
 */
data class Recording(
    val id: String,
    val roomId: String,
    val roomName: String,
    val roomSlug: String,
    val mode: RecordingMode,
    val status: RecordingStatus,
    val createdAt: String,
    val updatedAt: String,
    val key: String? = null,
    val isExpired: Boolean = false,
    val expiredAt: String? = null
)

internal fun ApiRecording.toRecording(): Recording = Recording(
    id = id,
    roomId = room.id,
    roomName = room.name,
    roomSlug = room.slug,
    mode = mode.toFacade(),
    status = status.toFacade(),
    createdAt = createdAt,
    updatedAt = updatedAt,
    key = key,
    isExpired = isExpired,
    expiredAt = expiredAt
)

internal fun ApiRecordingMode.toFacade(): RecordingMode = when (this) {
    ApiRecordingMode.SCREEN_RECORDING -> RecordingMode.ScreenRecording
    ApiRecordingMode.TRANSCRIPT -> RecordingMode.Transcript
}

internal fun ApiRecordingStatus.toFacade(): RecordingStatus = when (this) {
    ApiRecordingStatus.INITIATED -> RecordingStatus.Initiated
    ApiRecordingStatus.ACTIVE -> RecordingStatus.Active
    ApiRecordingStatus.STOPPED -> RecordingStatus.Stopped
    ApiRecordingStatus.SAVED -> RecordingStatus.Saved
    ApiRecordingStatus.ABORTED -> RecordingStatus.Aborted
    ApiRecordingStatus.FAILED_TO_START -> RecordingStatus.FailedToStart
    ApiRecordingStatus.FAILED_TO_STOP -> RecordingStatus.FailedToStop
    ApiRecordingStatus.NOTIFICATION_SUCCEEDED -> RecordingStatus.NotificationSucceeded
}
