package io.vopenia.app.http

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.vopenia.konfig.Konfig
import io.vopenia.konfig.LogSession
import io.vopenia.sdk.utils.AuthenticationInformation

class BackendConnection {
    private val client = createClient(
        configuration = Configuration(
            enableLogs = true
        )
    )

    suspend fun token(username: String, password: String): AuthenticationInformation {
        try {
            return client.get(
                "${Konfig.tunnelEndpointTokenForwarder}?username=$username&password=$password"
            ).body<io.vopenia.konfig.AuthenticationInformation>()
                .let {
                    AuthenticationInformation(
                        csrftoken = it.csrftoken,
                        meetSessionId = it.meetSessionId
                    )
                }
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
}
