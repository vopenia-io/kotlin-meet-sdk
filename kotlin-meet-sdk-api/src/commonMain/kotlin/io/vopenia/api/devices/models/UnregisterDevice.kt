package io.vopenia.api.devices.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class UnregisterDevice(
    @SerialName("registration_id")
    val registrationId: String,
)
