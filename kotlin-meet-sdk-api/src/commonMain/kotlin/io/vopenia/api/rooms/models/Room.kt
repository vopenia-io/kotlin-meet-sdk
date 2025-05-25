package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class Room(
    val id: String,
    val name: String,
    val slug: String,
    val configuration: JsonElement,
    @SerialName("access_level")
    val accessLevel: RoomAccessLevel,
    // val language: String,
    val accesses: List<Access>,
    val livekit: Livekit,
    @SerialName("is_administrable")
    val isAadministrable: Boolean
)

enum class RoomAccessLevel {
    @SerialName("public")
    Public,

    @SerialName("trusted")
    Trusted,

    @SerialName("restricted")
    Restricted
}
