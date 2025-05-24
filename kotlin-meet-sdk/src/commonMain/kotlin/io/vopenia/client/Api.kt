package io.vopenia.client

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.vopenia.api.rooms.ApiRooms
import io.vopenia.api.users.ApiUsers

class Api(
    prefix: String,
    enableHttpLogs: Boolean = false,
    getAuthent: suspend () -> AuthenticationInformation,
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
}
