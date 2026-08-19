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
    // access_level and is_administrable MUST have defaults: this DTO decodes the
    // create/update/list room responses, and the deployed backend omits
    // is_administrable on some of them (e.g. POST rooms/). A required field there
    // makes the whole decode throw a MissingFieldException, which killed room
    // creation in production (Google Play review rejected the app on exactly
    // that). Mirrors the defaults of the lenient ApiRooms.ApiRoomResponse.
    @SerialName("access_level")
    val accessLevel: ApiRoomAccessLevel = ApiRoomAccessLevel.Public,
    // val language: String,
    val accesses: List<ApiAccess> = emptyList(),
    val livekit: Livekit? = null,
    @SerialName("is_administrable")
    val isAdministrable: Boolean = false
)
