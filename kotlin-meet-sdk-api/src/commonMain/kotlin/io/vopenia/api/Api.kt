package io.vopenia.api

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.vopenia.api.config.ApiConfigs
import io.vopenia.api.devices.ApiDevices
import io.vopenia.api.recordings.ApiRecordings
import io.vopenia.api.rooms.ApiRooms
import io.vopenia.api.users.ApiUsers
import kotlinx.serialization.json.Json

// Single source of truth for the wire (de)serialization settings — also exercised
// directly by unit tests so decode-leniency regressions are caught offline.
//
// explicitNulls = false is REQUIRED: partial-update DTOs like ApiPatchRoomParam
// leave name/access_level null to mean "leave unchanged". With the default
// (explicitNulls = true) those nulls are serialized and the Django backend rejects
// e.g. `name: null` with a 400 on PATCH rooms/{id}/ (host-commands publish
// toggles). Meet Web sends only the changed keys; this matches that contract.
//
// ignoreUnknownKeys + coerceInputValues make response decoding resilient to
// backend drift: added keys are skipped, and an unknown enum value (e.g. a new
// access_level) or an explicit null on a defaulted field falls back to the
// property default instead of throwing and killing the whole feature. Response
// models must pair this with default values on fields the backend may omit
// (see ApiRoom.is_administrable — a Play-review-breaking crash).
internal val VopeniaApiJson = Json {
    explicitNulls = false
    encodeDefaults = true
    ignoreUnknownKeys = true
    coerceInputValues = true
    prettyPrint = true
}

class Api(
    prefix: String,
    enableHttpLogs: Boolean = false,
    getAuthent: suspend () -> AuthenticationInformation?,
) {
    private val client = createClient(
        Configuration(
            json = VopeniaApiJson,
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