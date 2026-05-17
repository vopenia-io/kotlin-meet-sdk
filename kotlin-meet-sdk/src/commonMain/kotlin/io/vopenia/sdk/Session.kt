package io.vopenia.sdk

import io.vopenia.api.Api
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.Livekit
import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.sdk.devices.Devices
import io.vopenia.sdk.recording.Recording
import io.vopenia.sdk.recording.toRecording
import io.vopenia.sdk.room.Room
import io.vopenia.sdk.room.RoomAccessLevel
import io.vopenia.sdk.user.User
import io.vopenia.sdk.user.toUser
import io.vopenia.sdk.utils.AuthenticationInformation
import io.vopenia.sdk.utils.getAllRooms

class Session(
    prefixApi: String,
    enableHttpLog: Boolean = false,
    refreshAuthenticationInformation: suspend () -> AuthenticationInformation?
) {
    internal val api = Api(prefixApi, enableHttpLog) {
        refreshAuthenticationInformation()?.let {
            io.vopenia.api.AuthenticationInformation(
                csrftoken = it.csrftoken,
                meetSessionId = it.meetSessionId
            )
        }
    }
    private var rooms = mutableListOf<Room>()

    val devices = Devices(api)

    suspend fun me(): User = api.users.me().toUser()

    suspend fun createRoom(
        name: String,
        accessLevel: RoomAccessLevel? = null
    ): Room {
        val apiRoom = api.rooms.createRoom(
            NewRoomParam(
                name,
                accessLevel = accessLevel?.toApi()
            )
        )

        return checkAppendRoom(apiRoom)
    }

    suspend fun rooms(): List<Room> {
        val result = getAllRooms(api)

        // why not using this ? -> because rooms() and room() would probably collide
        // val toRemove = rooms.filter { room ->
        //    null == result.find { it.id == room.id }
        // }
        // rooms -= toRemove.toSet()

        val toAdd = mutableListOf<Room>()
        // check for existing rooms
        result.forEach { apiRoom ->
            val existing = rooms.find { it.id == apiRoom.id }
            if (null != existing) {
                existing.internalRoom = apiRoom
            } else {
                toAdd += Room(this, apiRoom, apiRoom.id)
            }
        }

        return rooms
    }


    suspend fun room(slug: String): Room? {
        val apiRoom = api.rooms.room(slug) ?: return null

        return checkAppendRoom(apiRoom)
    }

    fun roomFromPushedValues(
        id: String,
        name: String,
        slug: String,
        accessLevel: RoomAccessLevel,
        // accesses: List<ApiAccess>,
        isAdministrable: Boolean,
        livekitUrl: String? = null,
        livekitRoom: String? = null,
        livekitToken: String? = null
    ) = checkAppendRoom(
        ApiRoom(
            id = id,
            name = name,
            slug = slug,
            accessLevel = accessLevel.toApi(),
            // accesses = accesses,
            isAdministrable = isAdministrable,
            livekit = Livekit.from(
                url = livekitUrl,
                room = livekitRoom,
                token = livekitToken
            )
        )
    )

    /**
     * List recordings owned by the current user. Pagination is one-based —
     * page 0 returns the first page.
     */
    suspend fun recordings(page: Int = 0): List<Recording> {
        val response = if (page == 0) api.recordings.recordings()
        else api.recordings.recordings(page)
        return response.results.map { it.toRecording() }
    }

    /**
     * Fetch a single recording by id. Returns null if not found / not authorised.
     */
    suspend fun recording(id: String): Recording? =
        api.recordings.recording(id)?.toRecording()

    /**
     * Delete a recording. Only allowed when the recording is in a final state.
     */
    suspend fun deleteRecording(id: String) = api.recordings.deleteRecording(id)

    private fun checkAppendRoom(apiRoom: ApiRoom): Room {
        val existing = rooms.find { it.id == apiRoom.id }
        if (null != existing) {
            existing.internalRoom = apiRoom
            // return the existing one
            return existing
        } else {
            val wrapper = Room(this, apiRoom, apiRoom.id)
            rooms += wrapper

            return wrapper
        }
    }
}

fun Livekit.Companion.from(
    url: String?,
    room: String?,
    token: String?
): Livekit? {
    if (null == url) return null
    if (null == room) return null
    if (null == token) return null

    return Livekit(url = url, room = room, token = token)
}
