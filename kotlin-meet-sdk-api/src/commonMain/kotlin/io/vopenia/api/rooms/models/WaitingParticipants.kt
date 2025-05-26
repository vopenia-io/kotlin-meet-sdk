package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

@Serializable
data class WaitingParticipants(
    val participants: List<ApiRequestEntryAnswer>
)
