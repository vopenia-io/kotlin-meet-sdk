package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiRequestEntryAnswer(
    val id: String,
    val username: String,
    val status: ApiRequestEntryStatus,
    val color: String,
    val livekit: Livekit? = null
)