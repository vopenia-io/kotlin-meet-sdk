package io.vopenia.sdk.room.recording

/**
 * Local view of an in-flight recording. The "Ready(url)" state proposed in the
 * directives is intentionally not modelled here: the backend signals Egress
 * completion via a server-side webhook, not via a client-facing callback.
 * Consumers wanting the final asset should poll `Session.recordings()` or
 * subscribe to push notifications.
 */
sealed class RecordingState {
    object Idle : RecordingState()
    data class Recording(val handle: RecordingHandle) : RecordingState()
    object Stopping : RecordingState()
    data class Failed(val error: Throwable) : RecordingState()
}
