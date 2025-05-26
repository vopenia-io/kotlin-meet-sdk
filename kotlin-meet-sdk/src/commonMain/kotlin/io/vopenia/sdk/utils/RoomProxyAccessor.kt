package io.vopenia.sdk.utils

import io.vopenia.sdk.room.Room

object RoomProxyAccessor {
    fun getRoom(room: Room) = room.liveKitRoom as Any
}