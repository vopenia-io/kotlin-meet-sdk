package io.vopenia.api.utils

import io.vopenia.api.rooms.models.Room
import io.vopenia.client.Api

suspend fun getAllRooms(apiCall: Api): List<Room> {
    val rooms = mutableListOf<Room>()

    var result = apiCall.rooms.rooms()
    println(result.results)
    rooms += result.results

    println("rooms -> ${result.results.size} -> ${rooms.size}")

    while (null != result.next) {
        val next = result.next!!.split("?page=")[1].split("\"")[0].toInt()
        println("next is not null -> $next")
        result = apiCall.rooms.rooms(next)
        println(result.results)
        rooms += result.results

        println("check next -> ${result.results.size} -> ${rooms.size}")
    }

    println("rooms size ${rooms.size}")

    return rooms
}