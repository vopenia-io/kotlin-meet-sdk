package io.vopenia.api.recordings.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ApiRecordingStatus {
    @SerialName("initiated")
    INITIATED,

    @SerialName("active")
    ACTIVE,

    @SerialName("stopped")
    STOPPED,

    @SerialName("saved")
    SAVED,

    @SerialName("aborted")
    ABORTED,

    @SerialName("failed_to_start")
    FAILED_TO_START,

    @SerialName("failed_to_stop")
    FAILED_TO_STOP,

    @SerialName("notification_succeeded")
    NOTIFICATION_SUCCEEDED
}
