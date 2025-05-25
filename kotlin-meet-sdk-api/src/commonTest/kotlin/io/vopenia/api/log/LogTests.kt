package io.vopenia.api.log

import io.vopenia.api.utils.GetTokens
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogTests {
    @Test
    fun testLog() = runTest {
        println(GetTokens("meet", "meet"))
    }
}
