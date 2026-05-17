package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiRemoveParticipantParam(
    @SerialName("participant_identity")
    val participantIdentity: String
)
