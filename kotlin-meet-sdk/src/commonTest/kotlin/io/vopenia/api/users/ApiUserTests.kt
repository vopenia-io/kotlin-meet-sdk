package io.vopenia.api.users

import io.vopenia.client.Api
import io.vopenia.konfig.Konfig
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class ApiUserTests {
    val api = Api(
        "http://localhost:8071/api/v1.0",
        enableHttpLogs = true
    ) { Konfig.cookieToken }

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
