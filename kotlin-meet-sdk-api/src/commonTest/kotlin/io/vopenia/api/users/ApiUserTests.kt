package io.vopenia.api.users

import io.vopenia.api.utils.GetTokens
import io.vopenia.api.Api
import io.vopenia.api.AuthenticationInformation
import io.vopenia.konfig.Konfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiUserTests {
    var ownerToken: AuthenticationInformation? = null
    val api = Api(
        "${Konfig.tunnelApiForwarder}/api/v1.0",
        // "http://localhost:8071/api/v1.0",
        enableHttpLogs = true
    ) {
        ownerToken ?: GetTokens("meet", "meet").also {
            ownerToken = it
        }
    }

    @Test
    fun testUsers() = runTest {
        val answer = api.users.users()

        // passing test means that we won't have creds or serialization issues for now
        println(answer)
    }

    @Test
    fun testMe() = runTest {
        val answer = api.users.me()

        // passing test means that we won't have creds or serialization issues for now
        println(answer)
    }
}
