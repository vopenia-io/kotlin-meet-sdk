package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Partial update payload for `PATCH rooms/{id}/`. Distinct from
 * [NewRoomParam] which requires `name` (used by POST/PUT). All fields are
 * optional — pass only what should change, and `name`/`access_level` keep
 * their current values when omitted.
 *
 * ⚠️ The HTTP client must be configured to **omit `null` fields** when
 * serialising; otherwise a `null` here would write the field on the server.
 */
@Serializable
data class ApiPatchRoomParam(
    val name: String? = null,
    @SerialName("access_level")
    val accessLevel: ApiRoomAccessLevel? = null,
    /**
     * Arbitrary JSON configuration. Backend persists it as-is for the
     * room's `configuration` field; Meet uses it to carry
     * `can_publish_sources` (host commands) and similar per-room defaults.
     */
    val configuration: JsonElement? = null
)
