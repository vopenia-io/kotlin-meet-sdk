package io.vopenia.sdk.room

internal expect fun currentTimeMillis(): Long

/**
 * Returns the current time formatted as an ISO-8601 UTC timestamp, e.g.
 * `2026-05-17T22:00:00.000Z`. Used to populate the LiveKit participant
 * attribute `handRaisedAt` in the format expected by Meet Web
 * (`new Date().toISOString()`).
 */
internal expect fun currentTimeMillisToIso(): String

/**
 * Returns a random UUID-string suitable for cross-participant correlation
 * (e.g. tool-request id). RFC 4122 v4 on all platforms.
 */
internal expect fun newUuid(): String
