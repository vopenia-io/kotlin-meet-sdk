package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

@Serializable
data class Livekit(
    val url: String,
    val room: String,
    val token: String
)
