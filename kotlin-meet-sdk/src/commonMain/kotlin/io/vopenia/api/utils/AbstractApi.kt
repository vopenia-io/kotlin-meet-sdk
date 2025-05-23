package io.vopenia.api.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.http.isSuccess

class AbstractApi(
    val client: HttpClient,
    val prefix: String,
    val getBearer: suspend () -> String
) {
    suspend inline fun <reified T> get(
        endpoint: String
    ): T {
        val bearer = getBearer()

        val request = client.get("$prefix/$endpoint") {
            header("Cookie", "meet_sessionid=$bearer")
        }

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }

        return request.body()
    }
}
