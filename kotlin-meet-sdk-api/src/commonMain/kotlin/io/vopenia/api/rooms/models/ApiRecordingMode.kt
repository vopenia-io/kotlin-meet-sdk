package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ApiRecordingMode {
    @SerialName("screen_recording")
    SCREEN_RECORDING,

    @SerialName("transcript")
    TRANSCRIPT
}
