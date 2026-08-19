package io.vopenia.api.rooms.models

import io.vopenia.api.users.models.ApiUser
import kotlinx.serialization.Serializable

// Decode-lenient: embedded in every ApiRoom.accesses entry — a drifted access
// serializer must not kill the whole room decode (see ApiRoom.is_administrable).
@Serializable
data class ApiAccess(
    val id: String,
    val user: ApiUser,
    val resource: String = "",
    val role: String = ""
)
