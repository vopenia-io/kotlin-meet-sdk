package io.vopenia.api.devices.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class RegisterIOS(
    @SerialName("registration_id")
    val registrationId: String,
    @SerialName("device_id")
    val deviceId: String,
    val name: String,
)