package io.vopenia.client

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.vopenia.api.users.ApiUsers

class Api(
    prefix: String,
    enableHttpLogs: Boolean = false,
    getBearer: suspend () -> String,
) {
    private val client = createClient(
        Configuration(enableLogs = enableHttpLogs)
    ) {
        // nothing
    }

    /**
     * Access the users & user endpoints
     */
    val users = ApiUsers(client, prefix, getBearer)
}
