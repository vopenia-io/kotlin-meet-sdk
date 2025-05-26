package io.vopenia.sdk.room

import io.vopenia.api.rooms.models.ApiAccess
import io.vopenia.sdk.user.User
import io.vopenia.sdk.user.to

data class Access(
    val id: String,
    val user: User,
    val resource: String,
    val role: String
)

fun ApiAccess.to() = Access(
    id,
    user = user.to(),
    resource,
    role
)
