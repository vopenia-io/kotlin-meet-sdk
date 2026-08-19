package io.vopenia.api.recordings.models

import io.vopenia.api.rooms.models.ApiRecordingMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Decode-lenient: one drifted item must not kill the whole recordings list.
// Only the primary key and the embedded room stay required; an unknown future
// status/mode coerces to the neutral pending/screen values instead of throwing.
@Serializable
data class ApiRecording(
    val id: String,
    val room: ApiRecordingRoom,
    @SerialName("created_at")
    val createdAt: String = "",
    @SerialName("updated_at")
    val updatedAt: String = "",
    val status: ApiRecordingStatus = ApiRecordingStatus.INITIATED,
    val mode: ApiRecordingMode = ApiRecordingMode.SCREEN_RECORDING,
    val key: String? = null,
    @SerialName("is_expired")
    val isExpired: Boolean = false,
    @SerialName("expired_at")
    val expiredAt: String? = null
)
