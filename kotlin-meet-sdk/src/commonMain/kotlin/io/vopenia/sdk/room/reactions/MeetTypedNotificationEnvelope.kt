package io.vopenia.sdk.room.reactions

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Generic Meet notification envelope used when the payload shape differs per
 * `type` and we want to read `type` first, then decode the rest with a
 * type-specific serializer.
 *
 * Distinct from [MeetNotificationEnvelope] which hard-codes `data` as a
 * [MeetReactionData] for the reaction-specific code path.
 */
@Serializable
internal data class MeetTypedNotificationEnvelope(
    val type: String,
    val data: JsonElement? = null
)
