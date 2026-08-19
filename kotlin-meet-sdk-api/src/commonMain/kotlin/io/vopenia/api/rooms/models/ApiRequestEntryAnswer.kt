package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

// Decode-lenient (lobby flow): only the id stays required. A missing display
// field or an unknown future status must not kill the request-entry poll or the
// waiting-participants list — unknown statuses coerce to Waiting, the neutral
// still-pending state.
@Serializable
data class ApiRequestEntryAnswer(
    val id: String,
    val username: String = "",
    val status: ApiRequestEntryStatus = ApiRequestEntryStatus.Waiting,
    val color: String = "",
    val livekit: Livekit? = null
)