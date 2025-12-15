package io.vopenia.api.devices

import io.ktor.client.HttpClient
import io.vopenia.api.AuthenticationInformation
import io.vopenia.api.devices.models.RegisterFCM
import io.vopenia.api.devices.models.RegisterIOS
import io.vopenia.api.devices.models.UnregisterDevice
import io.vopenia.api.utils.AbstractApi

class ApiDevices(
    client: HttpClient,
    prefix: String,
    getAuthent: suspend () -> AuthenticationInformation?
) {
    private val wrapper = AbstractApi(client, prefix, getAuthent)

    /**
     * Register a new iOS device. No-op when the registrationId already exists
     */
    suspend fun registeriOS(
        registrationId: String,
        deviceId: String,
        name: String
    ) = wrapper.postUnit(
        "devices/apns/register/", RegisterIOS(
            registrationId = registrationId,
            deviceId = deviceId,
            name = name
        )
    )

    /**
     * Register a new Android device. No-op when the registrationId already exists
     */
    suspend fun registerAndroid(
        registrationId: String,
    ) = registerFCM(RegisterFCM(registrationId = registrationId, browser = "android"))

    /**
     * Register a new Browser device. No-op when the registrationId already exists
     */
    suspend fun registerBrowser(
        registrationId: String,
        p256dh: String,
        auth: String,
    ) = registerFCM(
        RegisterFCM(
            registrationId = registrationId,
            p256dh = p256dh,
            auth = auth,
            browser = "browser"
        )
    )

    private suspend fun registerFCM(body: RegisterFCM) =
        wrapper.postUnit("devices/webpush/register/", body)

    suspend fun unregisterBrowser(registrationId: String) =
        wrapper.postUnit("devices/webpush/unregister/", UnregisterDevice(registrationId))

    suspend fun unregisteriOS(registrationId: String) =
        wrapper.postUnit("devices/apns/unregister/", UnregisterDevice(registrationId))
}
