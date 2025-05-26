package io.vopenia.sdk.room

import io.vopenia.api.rooms.models.ApiRequestEntryStatus

enum class RequestEntryStatus {
    Waiting,
    Accepted,
    Idle,
    Denied,
    Timeout,
}

fun ApiRequestEntryStatus.to() = when (this) {
    ApiRequestEntryStatus.Waiting -> RequestEntryStatus.Waiting
    ApiRequestEntryStatus.Accepted -> RequestEntryStatus.Accepted
    ApiRequestEntryStatus.Idle -> RequestEntryStatus.Idle
    ApiRequestEntryStatus.Denied -> RequestEntryStatus.Denied
    ApiRequestEntryStatus.Timeout -> RequestEntryStatus.Timeout
}
