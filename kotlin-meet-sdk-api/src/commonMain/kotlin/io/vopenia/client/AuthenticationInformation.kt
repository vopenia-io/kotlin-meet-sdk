package io.vopenia.client

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationInformation(
    val csrftoken: String,
    val meetSessionId: String
)
