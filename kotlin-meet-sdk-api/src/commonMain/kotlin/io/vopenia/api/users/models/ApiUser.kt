package io.vopenia.api.users.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiUser(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("short_name")
    val shortName: String? = null,
    val timezone: String,
    val language: String
)
