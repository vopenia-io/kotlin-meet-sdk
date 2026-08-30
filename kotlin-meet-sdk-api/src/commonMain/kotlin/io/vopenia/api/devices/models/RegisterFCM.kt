package io.vopenia.api.devices.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterFCM(
    @SerialName("registration_id")
    val registrationId: String,
    val p256dh: String = "",
    val auth: String = "",
    val browser: String,
)