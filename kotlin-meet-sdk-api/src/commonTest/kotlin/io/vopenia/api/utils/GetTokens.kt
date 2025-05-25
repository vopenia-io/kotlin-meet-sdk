package io.vopenia.api.utils

import eu.codlab.http.createClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.vopenia.client.AuthenticationInformation
import io.vopenia.konfig.Konfig
import io.vopenia.konfig.LogSession


private val client = createClient {
    // nothing
}

suspend fun GetTokens(username: String, password: String): AuthenticationInformation {
    try {
        return client.get(
            "${Konfig.tunnelEndpointTokenForwarder}?username=$username&password=$password"
        ).body<AuthenticationInformation>()
    } catch (err: Throwable) {
        err.printStackTrace()
    }

    return LogSession().getAuthenticateAnswer(username, password).let {
        AuthenticationInformation(
            meetSessionId = it.meetSessionId,
            csrftoken = it.csrftoken
        )
    }
}