package io.vopenia.konfig

import kotlinx.serialization.Serializable

@Serializable
data class AuthenticationInformation(
    val csrftoken: String,
    val meetSessionId: String
)
