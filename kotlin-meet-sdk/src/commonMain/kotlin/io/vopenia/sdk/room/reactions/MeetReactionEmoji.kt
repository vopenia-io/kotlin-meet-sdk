package io.vopenia.sdk.room.reactions

/**
 * The fixed catalog of reaction emojis that the Meet Web frontend accepts.
 *
 * Meet Web's [ReactionsToggle](src/frontend/src/features/rooms/livekit/components/controls/ReactionsToggle.tsx)
 * filters incoming `reactionReceived` notifications against this exact list of
 * string-keys (`Object.values(Emoji).includes(emoji)`). Any other value is
 * silently dropped on the Web side — so producers should always send one of
 * these constants when cross-platform interop is required.
 *
 * The [key] is the wire value that goes into the `data.emoji` field of the
 * notification envelope; the enum identifier is the Kotlin-idiomatic name.
 */
enum class MeetReactionEmoji(val key: String) {
    ThumbsUp("thumbs-up"),
    ThumbsDown("thumbs-down"),
    ClappingHands("clapping-hands"),
    RedHeart("red-heart"),
    FaceWithTearsOfJoy("face-with-tears-of-joy"),
    FaceWithOpenMouth("face-with-open-mouth"),
    PartyPopper("party-popper"),
    FoldedHands("folded-hands");

    companion object {
        /**
         * Parse a Meet wire `key` back to the typed enum, or null if the value
         * doesn't match any known reaction.
         */
        fun fromKey(key: String?): MeetReactionEmoji? =
            entries.firstOrNull { it.key == key }
    }
}
