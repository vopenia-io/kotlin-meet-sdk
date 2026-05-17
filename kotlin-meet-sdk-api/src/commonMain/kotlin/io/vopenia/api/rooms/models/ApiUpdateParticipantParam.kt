package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class ApiUpdateParticipantParam(
    @SerialName("participant_identity")
    val participantIdentity: String,
    val name: String? = null,
    val attributes: Map<String, String>? = null
)
