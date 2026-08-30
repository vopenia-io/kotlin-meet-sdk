package io.vopenia.sdk.room.recording

import kotlinx.coroutines.flow.StateFlow

/**
 * Live handle returned by [io.vopenia.sdk.room.Room.requestToolStart]. Lets
 * the demander observe [status] until a terminal state is reached, and cancel
 * the request before it's answered. The handle is single-use — once
 * [status] reaches Accepted/Rejected/Expired, further state changes don't occur.
 */
interface ToolStartRequestHandle {
    val requestId: String
    val mode: RecordingMode
    val status: StateFlow<ToolRequestStatus>

    /**
     * Withdraw a still-pending request. No-op if it's already in a terminal
     * state. Broadcasts a `toolStartCancelled` notification on the data
     * channel so admin UIs can clear their pending toast.
     */
    suspend fun cancel()
}
