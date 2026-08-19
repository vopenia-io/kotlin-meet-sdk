package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

// Decode-lenient: nested in room payloads. A partial livekit block must not kill
// the whole room decode — an empty url/token simply fails the later connect,
// which every caller already handles with a friendly error path.
@Serializable
data class Livekit(
    val url: String = "",
    val room: String = "",
    val token: String = ""
)
