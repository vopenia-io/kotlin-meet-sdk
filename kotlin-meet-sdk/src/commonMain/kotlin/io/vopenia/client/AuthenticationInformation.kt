package io.vopenia.client

data class AuthenticationInformation(
    val csrftoken: String,
    val meetSessionId: String
)
