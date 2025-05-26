package io.vopenia.sdk.room

import io.vopenia.api.rooms.models.ApiRoomAccessLevel

enum class RoomAccessLevel {
    Public,

    Trusted,

    Restricted;

    internal fun toApi() = when (this) {
        Public -> ApiRoomAccessLevel.Public
        Trusted -> ApiRoomAccessLevel.Trusted
        Restricted -> ApiRoomAccessLevel.Restricted
    }
}

fun ApiRoomAccessLevel.to() = when (this) {
    ApiRoomAccessLevel.Public -> RoomAccessLevel.Public
    ApiRoomAccessLevel.Trusted -> RoomAccessLevel.Public
    ApiRoomAccessLevel.Restricted -> RoomAccessLevel.Public
}