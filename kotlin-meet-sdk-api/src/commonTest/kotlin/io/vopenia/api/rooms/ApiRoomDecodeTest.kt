package io.vopenia.api.rooms

import io.vopenia.api.VopeniaApiJson
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.ApiRoomAccessLevel
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Offline decode-leniency regression tests, run with the exact production Json
 * settings ([VopeniaApiJson]).
 *
 * Context: the deployed backend omitted `is_administrable` from the POST rooms/
 * (create) response; ApiRoom declared it required, so the decode threw a
 * MissingFieldException, room creation died with a raw error toast, and Google
 * Play rejected the app for Broken Functionality. These tests pin the contract:
 * a room payload missing optional-in-practice fields MUST still decode.
 */
class ApiRoomDecodeTest {
    @Test
    fun decodesCreateResponseWithoutIsAdministrableNorAccessLevel() {
        val room = VopeniaApiJson.decodeFromString(
            ApiRoom.serializer(),
            """{"id":"abc","name":"demo","slug":"abc-defg-hij"}""",
        )

        assertEquals("abc", room.id)
        assertEquals("abc-defg-hij", room.slug)
        assertFalse(room.isAdministrable)
        assertEquals(ApiRoomAccessLevel.Public, room.accessLevel)
    }

    @Test
    fun keepsExplicitValuesWhenPresent() {
        val room = VopeniaApiJson.decodeFromString(
            ApiRoom.serializer(),
            """
            {
              "id": "abc",
              "name": "demo",
              "slug": "abc-defg-hij",
              "access_level": "restricted",
              "is_administrable": true,
              "livekit": {"url": "wss://lk", "room": "abc-defg-hij", "token": "tok"}
            }
            """.trimIndent(),
        )

        assertTrue(room.isAdministrable)
        assertEquals(ApiRoomAccessLevel.Restricted, room.accessLevel)
        assertEquals("tok", room.livekit?.token)
    }

    @Test
    fun decodesAccessesWithSlimEmbeddedUser() {
        // The accesses[].user embedded serializer is slimmer than the standalone
        // user endpoint — missing email/timezone/language (or access role) must
        // not kill the whole room decode.
        val room = VopeniaApiJson.decodeFromString(
            ApiRoom.serializer(),
            """
            {
              "id": "abc",
              "name": "demo",
              "slug": "abc-defg-hij",
              "accesses": [{"id": "acc1", "user": {"id": "u1"}}]
            }
            """.trimIndent(),
        )

        assertEquals("u1", room.accesses.single().user.id)
        assertEquals("", room.accesses.single().user.email)
        assertEquals("", room.accesses.single().role)
    }

    @Test
    fun coercesUnknownAccessLevelAndUnknownKeysToDefaults() {
        // A future backend adding an access level or new keys must not kill the
        // decode: coerceInputValues falls back to the property default and
        // ignoreUnknownKeys skips additions.
        val room = VopeniaApiJson.decodeFromString(
            ApiRoom.serializer(),
            """
            {
              "id": "abc",
              "name": "demo",
              "slug": "abc-defg-hij",
              "access_level": "brand-new-level",
              "some_future_key": {"nested": 1}
            }
            """.trimIndent(),
        )

        assertEquals(ApiRoomAccessLevel.Public, room.accessLevel)
    }
}
