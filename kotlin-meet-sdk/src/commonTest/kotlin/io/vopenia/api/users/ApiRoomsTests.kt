package io.vopenia.api.users

import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.api.rooms.models.RequestEntryStatus
import io.vopenia.api.rooms.models.RoomAccessLevel
import io.vopenia.api.utils.log.LogSession
import io.vopenia.client.Api
import io.vopenia.client.AuthenticationInformation
import korlibs.time.DateTime
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.test.fail

@Suppress("SwallowedException")
class ApiRoomsTests {
    var ownerToken: AuthenticationInformation? = null
    var kleperfToken: AuthenticationInformation? = null

    val apiOwnerMeet = Api(
        "http://localhost:8071/api/v1.0",
        enableHttpLogs = true
    ) {
        ownerToken ?: LogSession().getAuthenticateAnswer("meet", "meet").also {
            ownerToken = it
        }
    }

    val apiOwnerKleperf = Api(
        "http://localhost:8071/api/v1.0",
        enableHttpLogs = true
    ) {
        kleperfToken ?: LogSession().getAuthenticateAnswer("kleperf", "kleperf").also {
            kleperfToken = it
        }
    }

    @Test
    fun testRooms() = runTest {
        // passing test means that we won't have creds or serialization issues for now
        val rooms = apiOwnerMeet.rooms.rooms()
        println(apiOwnerMeet.rooms.rooms())

        val newRoom = apiOwnerMeet.rooms.createRoom(
            NewRoomParam(
                name = "${DateTime.nowUnixMillisLong()}",
                configuration = "",
                accessLevel = RoomAccessLevel.Public
            )
        )
        println(newRoom)

        val newRooms = apiOwnerMeet.rooms.rooms()
        println(newRooms)

        val updated = apiOwnerMeet.rooms.updateRoom(
            newRoom.id,
            NewRoomParam(
                name = "${newRoom.name}_updated",
                configuration = "",
                accessLevel = RoomAccessLevel.Public
            )
        )

        assertEquals(updated.name, "${newRoom.name}_updated")
        assertNull(rooms.results.find { it.id == newRoom.id })
        assertNotNull(newRooms.results.find { it.id == newRoom.id })

        apiOwnerMeet.rooms.deleteRoom(newRoom.id)
    }

    @Test
    fun requestEntry() = runTest {
        listOf(
            Triple(
                RoomAccessLevel.Public,
                RequestEntryStatus.Accepted,
                RequestEntryStatus.Accepted
            ),
            Triple(
                RoomAccessLevel.Restricted,
                RequestEntryStatus.Waiting,
                RequestEntryStatus.Waiting
            ),
        ).forEach { (roomVisibility, ownerStatus, outsiderStatus) ->
            val newRoom = apiOwnerMeet.rooms.createRoom(
                NewRoomParam(
                    name = "${DateTime.nowUnixMillisLong()}",
                    configuration = "",
                    accessLevel = roomVisibility
                )
            )

            val result1 = apiOwnerMeet.rooms.requestEntry(newRoom.id, "owner")
            assertEquals(ownerStatus, result1.status)

            val result2 = apiOwnerKleperf.rooms.requestEntry(newRoom.id, "outside")
            assertEquals(outsiderStatus, result2.status)

            apiOwnerMeet.rooms.deleteRoom(newRoom.id)
        }
    }

    @Test
    fun testInvite() = runTest {
        val newRoom = apiOwnerMeet.rooms.createRoom(
            NewRoomParam(
                name = "${DateTime.nowUnixMillisLong()}",
                configuration = "",
                accessLevel = RoomAccessLevel.Public
            )
        )

        apiOwnerMeet.rooms.invite(newRoom.id, "codlabtech+test@gmail.com")

        try {
            apiOwnerMeet.rooms.invite(newRoom.id)
            fail("the call should have failed")
        } catch (err: Throwable) {
            // expected
        }

        apiOwnerMeet.rooms.deleteRoom(newRoom.id)
    }

    @Test
    fun testEnterAccepted() = runTest {
        val newRoom = apiOwnerMeet.rooms.createRoom(
            NewRoomParam(
                name = "${DateTime.nowUnixMillisLong()}",
                configuration = "",
                accessLevel = RoomAccessLevel.Restricted
            )
        )

        val requestEntry = apiOwnerKleperf.rooms.requestEntry(newRoom.id, "myself")
        assertEquals(RequestEntryStatus.Waiting, requestEntry.status)

        val waiting1 = apiOwnerMeet.rooms.waitingParticipants(newRoom.id)
        assertTrue(waiting1.participants.isNotEmpty())
        assertEquals("myself", waiting1.participants[0].username)

        apiOwnerMeet.rooms.enter(newRoom.id, requestEntry.id, true)

        val requestEntryAfter = apiOwnerKleperf.rooms.requestEntry(newRoom.id, requestEntry)
        val waiting = apiOwnerMeet.rooms.waitingParticipants(newRoom.id)
        assertEquals(RequestEntryStatus.Accepted, requestEntryAfter.status)
        assertNotNull(requestEntryAfter.livekit)

        assertTrue(waiting.participants.isEmpty())

        apiOwnerMeet.rooms.deleteRoom(newRoom.id)
    }

    @Test
    fun testEnterDenied() = runTest {
        val newRoom = apiOwnerMeet.rooms.createRoom(
            NewRoomParam(
                name = "${DateTime.nowUnixMillisLong()}",
                configuration = "",
                accessLevel = RoomAccessLevel.Restricted
            )
        )

        val requestEntry = apiOwnerKleperf.rooms.requestEntry(newRoom.id, "myself")
        assertEquals(RequestEntryStatus.Waiting, requestEntry.status)

        val waiting1 = apiOwnerMeet.rooms.waitingParticipants(newRoom.id)
        assertTrue(waiting1.participants.isNotEmpty())
        assertEquals("myself", waiting1.participants[0].username)

        apiOwnerMeet.rooms.enter(newRoom.id, requestEntry.id, false)

        apiOwnerKleperf.rooms.rooms()

        val requestEntryAfter = apiOwnerKleperf.rooms.requestEntry(newRoom.id, requestEntry)
        assertEquals(RequestEntryStatus.Denied, requestEntryAfter.status)
        assertNull(requestEntryAfter.livekit)

        val waiting2 = apiOwnerMeet.rooms.waitingParticipants(newRoom.id)
        assertTrue(waiting2.participants.isEmpty())

        apiOwnerMeet.rooms.deleteRoom(newRoom.id)
    }

    @Test
    fun testCreation() = runTest {
        val common =
            NewRoomParam(
                name = "${DateTime.nowUnixMillisLong()}",
                configuration = "",
                accessLevel = RoomAccessLevel.Public
            )

        val newRoomMeet = apiOwnerMeet.rooms.createRoom(common)

        try {
            apiOwnerKleperf.rooms.createRoom(common)
            fail("should have failed")
        } catch (err: Throwable) {
            // nothing
        }

        apiOwnerMeet.rooms.deleteRoom(newRoomMeet.id)
    }
}
