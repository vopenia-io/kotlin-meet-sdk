package io.vopenia.api.config

import io.ktor.client.HttpClient
import io.vopenia.api.AuthenticationInformation
import io.vopenia.api.config.models.ApiConfig
import io.vopenia.api.utils.AbstractApi

/**
 * Binding for the Meet backend frontend-configuration endpoint
 * (`GET /api/v1.0/config/`). Used by the SDK to surface server capability
 * flags (recording enabled, transcription modes, telephony, …) so the UI
 * can hide/disable features the deployment doesn't allow.
 */
class ApiConfigs(
    client: HttpClient,
    prefix: String,
    getAuthent: suspend () -> AuthenticationInformation?
) {
    private val wrapper = AbstractApi(client, prefix, getAuthent)

    /**
     * Fetch the deployment's frontend configuration. Returns the full DTO
     * — pick the fields you need (typically `recording.isEnabled`).
     */
    suspend fun config(): ApiConfig = wrapper.get<ApiConfig>("config")
}
