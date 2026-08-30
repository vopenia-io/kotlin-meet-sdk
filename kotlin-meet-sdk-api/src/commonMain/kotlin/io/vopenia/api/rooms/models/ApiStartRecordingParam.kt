package io.vopenia.api.rooms.models

import kotlinx.serialization.Serializable

@Serializable
data class ApiStartRecordingParam(
    val mode: ApiRecordingMode
)
