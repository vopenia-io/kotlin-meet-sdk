package io.vopenia.api.recordings

import io.ktor.client.HttpClient
import io.vopenia.api.AuthenticationInformation
import io.vopenia.api.recordings.models.ApiRecording
import io.vopenia.api.utils.AbstractApi
import io.vopenia.api.utils.Page

class ApiRecordings(
    client: HttpClient,
    prefix: String,
    getAuthent: suspend () -> AuthenticationInformation?
) {
    private val wrapper = AbstractApi(client, prefix, getAuthent)

    /**
     * List recordings owned by the authenticated user.
     */
    suspend fun recordings(): Page<ApiRecording> = wrapper.get("recordings/")

    /**
     * Page-aware variant.
     */
    suspend fun recordings(page: Int): Page<ApiRecording> =
        wrapper.get("recordings/?page=$page")

    /**
     * Fetch a single recording by id. Returns `null` if not found or not authorised.
     */
    suspend fun recording(id: String): ApiRecording? = try {
        wrapper.get("recordings/$id/")
    } catch (err: Throwable) {
        null
    }

    /**
     * Delete a recording. Only allowed when its status is final
     * (stopped/saved/aborted/failed_to_start/failed_to_stop).
     */
    suspend fun deleteRecording(id: String) = wrapper.delete("recordings/$id/")
}
