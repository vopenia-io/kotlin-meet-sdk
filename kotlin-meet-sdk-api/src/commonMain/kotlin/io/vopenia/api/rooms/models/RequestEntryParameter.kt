package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

@Serializable
data class RequestEntryParameter(
    val username: String
)
