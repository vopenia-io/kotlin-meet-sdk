package io.vopenia.api.config.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Backend frontend configuration (`GET /api/v1.0/config/`).
 *
 * Shape derived from `meet/src/backend/core/api/__init__.py:get_frontend_configuration`.
 * All fields default to null/empty to absorb future settings appended via
 * `settings.FRONTEND_CONFIGURATION.update(...)` on the backend without
 * breaking deserialization.
 */
@Serializable
data class ApiConfig(
    @SerialName("LANGUAGE_CODE")
    val languageCode: String? = null,
    val recording: ApiRecordingConfig? = null,
    val telephony: ApiTelephonyConfig? = null,
    val subtitle: ApiSubtitleConfig? = null,
    val livekit: ApiLivekitConfig? = null,
)

@Serializable
data class ApiRecordingConfig(
    @SerialName("is_enabled")
    val isEnabled: Boolean = false,
    @SerialName("available_modes")
    val availableModes: List<String> = emptyList(),
    @SerialName("expiration_days")
    val expirationDays: Int? = null,
    @SerialName("max_duration")
    val maxDuration: Int? = null,
)

@Serializable
data class ApiTelephonyConfig(
    val enabled: Boolean = false,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("default_country")
    val defaultCountry: String? = null,
)

@Serializable
data class ApiSubtitleConfig(
    val enabled: Boolean = false,
)

@Serializable
data class ApiLivekitConfig(
    val url: String? = null,
    @SerialName("force_wss_protocol")
    val forceWssProtocol: Boolean = false,
    @SerialName("enable_firefox_proxy_workaround")
    val enableFirefoxProxyWorkaround: Boolean = false,
    @SerialName("default_sources")
    val defaultSources: List<String> = emptyList(),
)
