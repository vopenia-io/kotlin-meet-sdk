package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ApiRequestEntryStatus {
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
