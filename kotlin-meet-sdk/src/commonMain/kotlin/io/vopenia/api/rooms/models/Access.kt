package io.vopenia.api.rooms.models

import io.vopenia.api.users.models.User
import kotlinx.serialization.Serializable

@Serializable
data class Access(
    val id: String,
    val user: User,
    val resource: String,
    val role: String
)
