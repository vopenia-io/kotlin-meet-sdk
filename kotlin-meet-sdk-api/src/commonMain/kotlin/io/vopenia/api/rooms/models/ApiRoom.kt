package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
data class ApiRoom(
    val id: String,
    val name: String,
    val slug: String,
    val configuration: JsonElement? = null,
    @SerialName("access_level")
    val accessLevel: RoomAccessLevel,
    // val language: String,
    val accesses: List<Access> = emptyList(),
    val livekit: Livekit? = null,
    @SerialName("is_administrable")
    val isAadministrable: Boolean
)
