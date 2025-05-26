package io.vopenia.sdk.utils

import io.vopenia.api.Api
import io.vopenia.api.rooms.models.ApiRoom

suspend fun getAllRooms(apiCall: Api): List<ApiRoom> {
    val rooms = mutableListOf<ApiRoom>()

    var result = apiCall.rooms.rooms()
    println(result.results)
    rooms += result.results

    while (null != result.next) {
        val next = result.next!!.split("?page=")[1].split("\"")[0].toInt()
        result = apiCall.rooms.rooms(next)
        println(result.results)
        rooms += result.results
    }

    return rooms
}