package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiMuteParticipantParam(
    @SerialName("participant_identity")
    val participantIdentity: String,
    @SerialName("track_sid")
    val trackSid: String
)
