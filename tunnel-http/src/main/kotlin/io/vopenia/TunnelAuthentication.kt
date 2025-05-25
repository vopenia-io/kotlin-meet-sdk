package io.vopenia

import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respond
import io.ktor.server.routing.Route
import io.ktor.server.routing.get
import io.vopenia.konfig.LogSession

fun Route.tokens() {
    get("/tokens") {
        try {
            val username = call.request.queryParameters["username"]!!
            val password = call.request.queryParameters["password"]!!

            val tokens = LogSession().getAuthenticateAnswer(username, password)

            call.respond(tokens)
        } catch (err: Throwable) {
            err.printStackTrace()
            println(err)
            call.respond(HttpStatusCode.NotFound)
        }
    }
}
