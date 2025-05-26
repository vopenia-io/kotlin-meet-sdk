package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName

enum class RoomAccessLevel {
    @SerialName("public")
    Public,

    @SerialName("trusted")
    Trusted,

    @SerialName("restricted")
    Restricted
}
