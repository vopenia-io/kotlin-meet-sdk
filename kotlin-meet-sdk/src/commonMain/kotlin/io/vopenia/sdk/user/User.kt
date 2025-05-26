package io.vopenia.sdk.user

import io.vopenia.api.users.models.ApiUser
import kotlinx.serialization.SerialName

data class User(
    val id: String,
    val email: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("short_name")
    val shortName: String? = null,
    val timezone: String,
    val language: String
)

fun ApiUser.to() = User(
    id = id,
    email = email,
    fullName = fullName,
    shortName = shortName,
    timezone = timezone,
    language = language
)