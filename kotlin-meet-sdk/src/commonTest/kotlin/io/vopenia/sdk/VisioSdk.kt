package io.vopenia.sdk

import io.vopenia.konfig.Konfig
import io.vopenia.sdk.utils.AuthenticationInformation
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class VisioSdkTests {
    @Test
    fun createSessionAndCheckRooms() = runTest {
        fun unconfigured(value: String) =
            value.isBlank() || "ngrok.endpoint.for" in value

        if (unconfigured(Konfig.tunnelApiForwarder) || unconfigured(Konfig.tunnelEndpointTokenForwarder)) {
            println(
                "WARNING: skipping createSessionAndCheckRooms — " +
                    "VOPENIA_MEET_TESTS_TUNNEL_ENDPOINT / VOPENIA_MEET_TESTS_TUNNEL_API are not set " +
                    "(or still set to the placeholder ngrok URLs) in gradle.properties"
            )
            return@runTest
        }

        val session = VisioSdk.openSession(
            "${Konfig.tunnelApiForwarder}/api/v1.0",
            true
        ) {
            GetTokens("meet", "meet")?.let {
                AuthenticationInformation(
                    csrftoken = it.csrftoken,
                    meetSessionId = it.meetSessionId
                )
            }
        }

        session.rooms()
    }
}