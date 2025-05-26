package io.vopenia.api

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationInformation(
    val csrftoken: String,
    val meetSessionId: String
)