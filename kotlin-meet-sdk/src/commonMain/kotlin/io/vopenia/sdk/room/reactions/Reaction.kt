package io.vopenia.sdk.room.reactions

/**
 * Ephemeral reaction broadcast over LiveKit's data channel. Reactions are
 * transient — no persistence, no replay on reconnect.
 *
 * [emoji] is the raw Meet wire key (e.g. `"red-heart"`). Callers expecting a
 * typed value can use [meetEmoji], which is non-null when the wire key matches
 * one of [MeetReactionEmoji]'s entries and null when it's an unknown / custom
 * value (Meet Web would also drop such reactions).
 */
data class Reaction(
    val author: String?,
    val emoji: String,
    val timestamp: Long
) {
    val meetEmoji: MeetReactionEmoji? get() = MeetReactionEmoji.fromKey(emoji)
}
