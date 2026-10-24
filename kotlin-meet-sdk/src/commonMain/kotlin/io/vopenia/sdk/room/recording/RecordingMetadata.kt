package io.vopenia.sdk.room.recording

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

/**
 * Recording state the Meet backend keeps in the LiveKit room metadata: `recording_mode`
 * (`screen_recording` or `transcript`) and `recording_status` (`starting`, `started`, then
 * `saving` once stopped). Meet Web's recording badge reads these two fields, together with
 * LiveKit's `isRecording`; the flag alone stays true while a stopped recording is saved.
 */
data class RecordingMetadata(
    val mode: RecordingMode?,
    val status: Status?,
) {
    enum class Status { Starting, Started, Saving }

    companion object {
        val None = RecordingMetadata(mode = null, status = null)

        /** Reads the two recording fields of a room metadata JSON; anything else gives [None]. */
        fun parse(metadata: String?): RecordingMetadata {
            if (metadata.isNullOrBlank()) return None
            val json = runCatching { Json.parseToJsonElement(metadata) }.getOrNull() as? JsonObject
                ?: return None
            val mode = when ((json["recording_mode"] as? JsonPrimitive)?.content) {
                "screen_recording" -> RecordingMode.ScreenRecording
                "transcript" -> RecordingMode.Transcript
                else -> null
            }
            val status = when ((json["recording_status"] as? JsonPrimitive)?.content) {
                "starting" -> Status.Starting
                "started" -> Status.Started
                "saving" -> Status.Saving
                else -> null
            }
            return RecordingMetadata(mode, status)
        }
    }
}
