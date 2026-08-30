package io.vopenia.api.users.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Decode-lenient: this DTO is also embedded in other payloads (e.g. every
// ApiRoom.accesses entry), where the backend uses slimmer serializers. Only the
// primary key stays required — a missing display field must not kill the whole
// parent decode (see ApiRoom.is_administrable for the production incident).
@Serializable
data class ApiUser(
    val id: String,
    val email: String = "",
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("short_name")
    val shortName: String? = null,
    val timezone: String = "",
    val language: String = ""
)
