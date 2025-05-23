package io.vopenia.api.users.models

import kotlinx.serialization.Serializable

@Serializable
data class UserPage(
    val count: Int,
    val next: String? = null,
    val previous: String? = null,
    val results: List<User>
)
