package io.vopenia.api.recordings.models

import io.vopenia.api.rooms.models.ApiRecordingMode
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiRecording(
    val id: String,
    val room: ApiRecordingRoom,
    @SerialName("created_at")
    val createdAt: String,
    @SerialName("updated_at")
    val updatedAt: String,
    val status: ApiRecordingStatus,
    val mode: ApiRecordingMode,
    val key: String? = null,
    @SerialName("is_expired")
    val isExpired: Boolean = false,
    @SerialName("expired_at")
    val expiredAt: String? = null
)
