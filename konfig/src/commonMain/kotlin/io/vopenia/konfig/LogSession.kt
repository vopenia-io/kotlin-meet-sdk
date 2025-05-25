package io.vopenia.konfig

import eu.codlab.http.Configuration
import eu.codlab.http.createClient
import io.ktor.client.plugins.cookies.AcceptAllCookiesStorage
import io.ktor.client.request.forms.FormDataContent
import io.ktor.client.request.get
import io.ktor.client.request.post
import io.ktor.client.request.setBody
import io.ktor.client.statement.bodyAsText
import io.ktor.http.Parameters
import io.ktor.http.Url

class LogSession {
    private val cookieStorage = AcceptAllCookiesStorage()
    private val clientWithoutRedirects = createClient(
        Configuration(
            enableLogs = false,
            installCookies = true,
            followRedirects = false,
            cookieStorage = cookieStorage
        )
    ) {
        // nothing
    }
    private val clientWithRedirects = createClient(
        Configuration(
            enableLogs = false,
            installCookies = true,
            followRedirects = true,
            cookieStorage = cookieStorage
        )
    ) {
        // nothing
    }

    suspend fun getAuthenticateAnswer(
        username: String,
        password: String
    ): AuthenticationInformation {
        /**
         * GET THE ACTUAL REALM AUTHENTICATION URL
         */
        val url =
            "http://localhost:8071/api/v1.0/authenticate/?silent=false&returnTo=http%3A%2F%2Flocalhost%3A3000%2F"

        val answer = clientWithoutRedirects.get(url)

        // println(client.cookies("http://localhost"))

        /**
         * GET THE ACTUAL FORM URL WHICH WILL BE USED TO POST THE USERNAME/PASSWORD
         */
        val redirectRealms = answer.headers["location"]!!
        val redirectAnswer = clientWithoutRedirects.get(redirectRealms)

        val content = redirectAnswer.bodyAsText()

        println("redirectRealms -> $redirectRealms")
        println(content)

        val forms = content.split("<form")
        val action = forms[1].split("action=\"")
        val actualAction = action[1].split("\"")[0].replace("&amp;", "&")

        val postUserInformation = clientWithRedirects.post(actualAction) {
            setBody(
                FormDataContent(
                    Parameters.build {
                        append("username", username)
                        append("password", password)
                        append("credentialId", "")
                    }
                )
            )
        }

        val postActualRedirect = postUserInformation.headers["location"]!!
        // create the last cookie
        clientWithRedirects.get(postActualRedirect)

        val cookies = cookieStorage.get(Url("http://localhost"))

        return AuthenticationInformation(
            meetSessionId = cookies.find { it.name == "meet_sessionid" }!!.value,
            csrftoken = cookies.find { it.name == "csrftoken" }!!.value,
        )
    }
}
