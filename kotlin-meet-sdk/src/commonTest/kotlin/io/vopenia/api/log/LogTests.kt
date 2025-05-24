package io.vopenia.api.log

import io.vopenia.api.utils.log.LogSession
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogTests {
    private val log = LogSession()

    @Test
    fun testLog() = runTest {
        println(log.getAuthenticateAnswer("meet", "meet"))
    }
}
