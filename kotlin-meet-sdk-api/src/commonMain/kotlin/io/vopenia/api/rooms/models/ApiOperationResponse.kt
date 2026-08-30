package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

/**
 * Common response shape for action endpoints (start/stop recording, start subtitle,
 * mute/update/remove participant). The backend returns either `{"message": "..."}`
 * or `{"status": "success"}` depending on the endpoint; both fields are optional.
 */
@Serializable
data class ApiOperationResponse(
    val message: String? = null,
    val status: String? = null
)
