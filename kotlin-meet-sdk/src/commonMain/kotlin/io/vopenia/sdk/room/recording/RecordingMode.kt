package io.vopenia.sdk.room.recording

import io.vopenia.api.rooms.models.ApiRecordingMode

/**
 * Recording mode selected when starting a recording on a [io.vopenia.sdk.room.Room].
 *
 * - [ScreenRecording] produces an MP4 video composite of the room.
 * - [Transcript] captures audio for offline ASR / transcript generation.
 */
enum class RecordingMode {
    ScreenRecording,
    Transcript;

    internal fun toApi(): ApiRecordingMode = when (this) {
        ScreenRecording -> ApiRecordingMode.SCREEN_RECORDING
        Transcript -> ApiRecordingMode.TRANSCRIPT
    }
}
