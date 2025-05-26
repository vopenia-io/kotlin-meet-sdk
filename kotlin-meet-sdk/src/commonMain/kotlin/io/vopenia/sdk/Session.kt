package io.vopenia.sdk

import io.vopenia.api.Api
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.sdk.room.Room
import io.vopenia.sdk.room.RoomAccessLevel
import io.vopenia.sdk.utils.AuthenticationInformation
import io.vopenia.sdk.utils.getAllRooms

class Session(
    prefixApi: String,
    enableHttpLog: Boolean = false,
    refreshAuthenticationInformation: suspend () -> AuthenticationInformation
) {
    internal val api = Api(prefixApi, enableHttpLog) {
        refreshAuthenticationInformation().let {
            io.vopenia.api.AuthenticationInformation(
                csrftoken = it.csrftoken,
                meetSessionId = it.meetSessionId
            )
        }
    }
    private var rooms = mutableListOf<Room>()

    suspend fun createRoom(
        name: String,
        accessLevel: RoomAccessLevel
    ): Room {
        val apiRoom = api.rooms.createRoom(
            NewRoomParam(
                name,
                configuration = "",
                accessLevel = accessLevel.toApi()
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