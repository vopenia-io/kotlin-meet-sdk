package io.vopenia.sdk.room

import io.vopenia.api.rooms.models.ApiRequestEntryAnswer

data class RequestEntryAnswer(
    val id: String,
    val username: String,
    val status: RequestEntryStatus,
    val color: String,
)

fun ApiRequestEntryAnswer.to() = RequestEntryAnswer(
    id = id,
    username = username,
    status = status.to(),
    color = color
)