package io.vopenia.api.utils

import io.vopenia.konfig.Konfig

private fun unconfigured(value: String) =
    value.isBlank() || "ngrok.endpoint.for" in value

fun tunnelsConfigured(): Boolean =
    !unconfigured(Konfig.tunnelApiForwarder) && !unconfigured(Konfig.tunnelEndpointTokenForwarder)

fun skipIfTunnelsUnconfigured(testName: String): Boolean {
    if (tunnelsConfigured()) return false
    println(
        "WARNING: skipping $testName — VOPENIA_MEET_TESTS_TUNNEL_ENDPOINT / " +
            "VOPENIA_MEET_TESTS_TUNNEL_API are not set (or still set to the placeholder ngrok URLs) " +
            "in gradle.properties"
    )
    return true
}
