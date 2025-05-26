package io.vopenia.sdk

import io.vopenia.konfig.Konfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class VisioSdkTests {
    @Test
    fun createSessionAndCheckRooms() = runTest {
        val session = VisioSdk.openSession(
            "${Konfig.tunnelApiForwarder}/api/v1.0",
            true
        ) {
            GetTokens("meet", "meet")
        }

        session.rooms()
    }
}