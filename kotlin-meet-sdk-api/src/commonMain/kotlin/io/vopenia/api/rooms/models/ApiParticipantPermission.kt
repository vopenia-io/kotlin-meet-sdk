package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Subset of LiveKit's `ParticipantPermission` proto that the Meet backend
 * accepts on `POST rooms/{id}/update-participant/`. Mirrors the fields the
 * Meet Web frontend reads back from `participant.permissions` and re-posts.
 *
 * Important — V1 omits two LiveKit fields:
 *  - `canUpdateMetadata`
 *  - `canSubscribeMetrics`
 *
 * because the Vopenia SDK's `ParticipantPermissions` model doesn't expose
 * them yet, so we can't read them back to round-trip the call. **To be
 * validated by integration test**: if the backend resets omitted fields to
 * proto defaults, we must enrich `ParticipantPermissions` upstream and
 * always include them.
 */
@Serializable
data class ApiParticipantPermission(
    @SerialName("can_publish")
    val canPublish: Boolean,

    /**
     * Allowed source identifiers, drawn from LiveKit's `TrackSource` enum:
     * `"MICROPHONE"`, `"CAMERA"`, `"SCREEN_SHARE"`, `"SCREEN_SHARE_AUDIO"`.
     */
    @SerialName("can_publish_sources")
    val canPublishSources: List<String>,

    @SerialName("can_subscribe")
    val canSubscribe: Boolean? = null,

    @SerialName("can_publish_data")
    val canPublishData: Boolean? = null
)

/** LiveKit `TrackSource` wire values used in `can_publish_sources`. */
object ApiTrackSource {
    const val MICROPHONE = "MICROPHONE"
    const val CAMERA = "CAMERA"
    const val SCREEN_SHARE = "SCREEN_SHARE"
    const val SCREEN_SHARE_AUDIO = "SCREEN_SHARE_AUDIO"
}
