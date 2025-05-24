package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RoomEnterParameter(
    @SerialName("participant_id")
    val participantId: String,
    @SerialName("allow_entry")
    val allowEntry: Boolean
)
