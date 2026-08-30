package io.vopenia.sdk.room.transcription

/**
 * Returned by `Room.startTranscription(language)`. The backend agent terminates
 * with the room (no explicit stop endpoint) — [stop] is a no-op kept for API
 * symmetry; consumers may use it to clear local state.
 */
class TranscriptionHandle internal constructor(
    val language: String
) {
    suspend fun stop() {
        // No backend stop endpoint — agent self-terminates with the room.
    }
}
