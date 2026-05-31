package io.vopenia.sdk.config

import io.vopenia.api.config.models.ApiConfig
import io.vopenia.sdk.room.recording.RecordingMode

/**
 * Capability flags surfaced by the Meet backend at `GET /config/`.
 * Used by the UI to gate features the deployment doesn't allow:
 * if `recording.isEnabled == false`, the Record / Transcribe tiles
 * should be hidden or disabled regardless of the user's role.
 */
data class ServerCapabilities(
    val languageCode: String?,
    val recording: RecordingCapabilities,
    val telephony: TelephonyCapabilities,
    val subtitle: SubtitleCapabilities,
)

data class RecordingCapabilities(
    val isEnabled: Boolean,
    /** Which recording modes the backend has workers wired up for. */
    val availableModes: Set<RecordingMode>,
    val expirationDays: Int?,
    val maxDurationSeconds: Int?,
) {
    val screenRecordingEnabled: Boolean
        get() = isEnabled && RecordingMode.ScreenRecording in availableModes
    val transcriptEnabled: Boolean
        get() = isEnabled && RecordingMode.Transcript in availableModes
}

data class TelephonyCapabilities(
    val enabled: Boolean,
    val phoneNumber: String?,
    val defaultCountry: String?,
)

data class SubtitleCapabilities(
    val enabled: Boolean,
)

internal fun ApiConfig.toServerCapabilities(): ServerCapabilities = ServerCapabilities(
    languageCode = languageCode,
    recording = RecordingCapabilities(
        isEnabled = recording?.isEnabled ?: false,
        availableModes = recording?.availableModes
            ?.mapNotNull { it.toRecordingModeOrNull() }
            ?.toSet()
            ?: emptySet(),
        expirationDays = recording?.expirationDays,
        maxDurationSeconds = recording?.maxDuration,
    ),
    telephony = TelephonyCapabilities(
        enabled = telephony?.enabled ?: false,
        phoneNumber = telephony?.phoneNumber,
        defaultCountry = telephony?.defaultCountry,
    ),
    subtitle = SubtitleCapabilities(
        enabled = subtitle?.enabled ?: false,
    ),
)

/**
 * Map the backend's `available_modes` strings (e.g. `"screen_recording"`,
 * `"transcript"`) onto the typed enum. Unknown values are ignored so a
 * future backend mode doesn't break deserialization.
 */
private fun String.toRecordingModeOrNull(): RecordingMode? = when (this) {
    "screen_recording" -> RecordingMode.ScreenRecording
    "transcript" -> RecordingMode.Transcript
    else -> null
}
