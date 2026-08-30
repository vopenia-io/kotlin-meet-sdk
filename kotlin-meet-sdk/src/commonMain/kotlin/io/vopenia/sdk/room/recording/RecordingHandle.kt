package io.vopenia.sdk.room.recording

import io.vopenia.sdk.room.Room

/**
 * Opaque handle returned by [Room.startRecording]. Holds the mode and start
 * timestamp so the UI can render an in-flight indicator without re-querying
 * the backend. Call [stop] to terminate the recording (equivalent to
 * `Room.stopRecording()`).
 */
class RecordingHandle internal constructor(
    private val room: Room,
    val mode: RecordingMode,
    val startedAt: Long
) {
    suspend fun stop() = room.stopRecording()
}
