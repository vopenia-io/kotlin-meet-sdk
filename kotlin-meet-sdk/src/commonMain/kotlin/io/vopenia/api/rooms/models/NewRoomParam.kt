package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class NewRoomParam(
    val name: String,
    val configuration: String,
    @SerialName("access_level")
    val accessLevel: RoomAccessLevel,
)
