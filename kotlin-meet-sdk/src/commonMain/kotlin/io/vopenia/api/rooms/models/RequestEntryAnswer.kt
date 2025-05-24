package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RequestEntryAnswer(
    val id: String,
    val username: String,
    val status: RequestEntryStatus,
    val color: String,
    val livekit: Livekit? = null
)

@Serializable
enum class RequestEntryStatus {
    @SerialName("waiting")
    Waiting,

    @SerialName("accepted")
    Accepted,

    @SerialName("idle")
    Idle,

    @SerialName("denied")
    Denied,

    @SerialName("timeout")
    Timeout,
}
