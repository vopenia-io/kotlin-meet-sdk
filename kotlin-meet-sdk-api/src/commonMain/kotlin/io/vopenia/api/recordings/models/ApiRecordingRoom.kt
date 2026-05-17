package io.vopenia.api.recordings.models

import io.vopenia.api.rooms.models.ApiRoomAccessLevel
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Minimal projection of a Room embedded in a Recording payload.
 * Matches the `ListRoomSerializer` shape returned by the backend
 * (`id`, `name`, `slug`, `access_level`).
 */
@Serializable
data class ApiRecordingRoom(
    val id: String,
    val name: String,
    val slug: String,
    @SerialName("access_level")
    val accessLevel: ApiRoomAccessLevel? = null
)
