package io.vopenia.sdk.room

internal expect fun currentTimeMillis(): Long

/**
 * Returns the current time formatted as an ISO-8601 UTC timestamp, e.g.
 * `2026-05-17T22:00:00.000Z`. Used to populate the LiveKit participant
 * attribute `handRaisedAt` in the format expected by Meet Web
 * (`new Date().toISOString()`).
 */
internal expect fun currentTimeMillisToIso(): String
