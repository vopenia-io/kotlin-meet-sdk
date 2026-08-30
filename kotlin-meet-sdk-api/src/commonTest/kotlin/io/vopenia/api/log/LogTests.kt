package io.vopenia.api.log

import io.vopenia.api.utils.GetTokens
import io.vopenia.api.utils.skipIfTunnelsUnconfigured
import kotlinx.coroutines.test.runTest
import kotlin.test.Test

class LogTests {
    @Test
    fun testLog() = runTest {
        if (skipIfTunnelsUnconfigured("testLog")) return@runTest
        println(GetTokens("meet", "meet"))
    }
}
