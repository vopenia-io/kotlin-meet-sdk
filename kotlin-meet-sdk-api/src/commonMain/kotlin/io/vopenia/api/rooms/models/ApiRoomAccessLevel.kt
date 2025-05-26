package io.vopenia.api.rooms.models

import kotlinx.serialization.SerialName

enum class ApiRoomAccessLevel {
    @SerialName("public")
    Public,

    @SerialName("trusted")
    Trusted,

    @SerialName("restricted")
    Restricted
}
