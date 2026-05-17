package io.vopenia.api

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.vopenia.api.devices.ApiDevices
import io.vopenia.api.recordings.ApiRecordings
import io.vopenia.api.rooms.ApiRooms
import io.vopenia.api.users.ApiUsers

class Api(
    prefix: String,
    enableHttpLogs: Boolean = false,
    getAuthent: suspend () -> AuthenticationInformation?,
) {
    private val client = createClient(
        Configuration(enableLogs = enableHttpLogs)
    ) {
        // nothing
    }

    /**
     * Access the users & user endpoints
     */
    val users = ApiUsers(client, prefix, getAuthent)

    /**
     * Access the users & user endpoints
     */
    val rooms = ApiRooms(client, prefix, getAuthent)

    /**
     * Access the devices endpoints
     */
    val devices = ApiDevices(client, prefix, getAuthent)

    /**
     * Access the recordings endpoints (list / fetch / delete past recordings).
     * Recording start/stop on a specific room is exposed via [ApiRooms.startRecording]
     * and [ApiRooms.stopRecording].
     */
    val recordings = ApiRecordings(client, prefix, getAuthent)
}