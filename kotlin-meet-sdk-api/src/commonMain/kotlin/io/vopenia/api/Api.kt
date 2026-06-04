package io.vopenia.api

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.vopenia.api.config.ApiConfigs
import io.vopenia.api.devices.ApiDevices
import io.vopenia.api.recordings.ApiRecordings
import io.vopenia.api.rooms.ApiRooms
import io.vopenia.api.users.ApiUsers
import kotlinx.serialization.json.Json

class Api(
    prefix: String,
    enableHttpLogs: Boolean = false,
    getAuthent: suspend () -> AuthenticationInformation?,
) {
    private val client = createClient(
        Configuration(
            // explicitNulls = false is REQUIRED: partial-update DTOs like
            // ApiPatchRoomParam leave name/access_level null to mean "leave
            // unchanged". With the default (explicitNulls = true) those nulls are
            // serialized and the Django backend rejects e.g. `name: null` with a
            // 400 on PATCH rooms/{id}/ (host-commands publish toggles). Meet Web
            // sends only the changed keys; this matches that contract.
            json = Json {
                explicitNulls = false
                encodeDefaults = true
                ignoreUnknownKeys = true
                prettyPrint = true
            },
            enableLogs = enableHttpLogs,
        )
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

    /**
     * Access the frontend-configuration endpoint (`GET /config/`).
     * Used to surface server capabilities so the UI can gate features
     * (recording, transcription, telephony) by deployment.
     */
    val config = ApiConfigs(client, prefix, getAuthent)
}