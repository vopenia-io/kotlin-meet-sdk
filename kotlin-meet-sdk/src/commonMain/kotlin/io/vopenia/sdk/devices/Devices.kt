package io.vopenia.sdk.devices

import io.vopenia.api.Api

class Devices(
    private val api: Api
) {
    suspend fun registerAndroid(registrationId: String) {
        api.devices.registerAndroid(registrationId)
    }

    suspend fun registeriOS(
        registrationId: String,
        deviceId: String,
        name: String
    ) {
        api.devices.registeriOS(registrationId, deviceId, name)
    }

    suspend fun registerBrowser(
        registrationId: String,
        p256dh: String,
        auth: String,
    ) {
        api.devices.registerBrowser(registrationId, p256dh, auth)
    }

    suspend fun unregisterBrowser(registrationId: String) {
        api.devices.unregisterBrowser(registrationId)
    }

    suspend fun unregisterAndroid(registrationId: String) {
        api.devices.unregisteriOS(registrationId)
    }

    suspend fun unregisteriOS(registrationId: String) {
        api.devices.unregisteriOS(registrationId)
    }
}