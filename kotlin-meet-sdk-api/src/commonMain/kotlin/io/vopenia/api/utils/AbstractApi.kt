package io.vopenia.api.utils

import io.ktor.client.HttpClient
import io.ktor.client.call.body
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.delete
import io.ktor.client.request.get
import io.ktor.client.request.header
import io.ktor.client.request.patch
import io.ktor.client.request.post
import io.ktor.client.request.put
import io.ktor.client.request.setBody
import io.ktor.http.ContentType
import io.ktor.http.Url
import io.ktor.http.contentType
import io.vopenia.api.AuthenticationInformation

class AbstractApi(
    val client: HttpClient,
    val prefix: String,
    val getAuthent: suspend () -> AuthenticationInformation?
) {
    private val host = Url(prefix).let {
        "${it.protocol.name}://${it.host}"
    }

    fun HttpRequestBuilder.buildCookie(
        bearer: AuthenticationInformation?,
        optional: List<Pair<String, String>> = emptyList()
    ) {
        val reserved = listOf("csrftoken", "sessionid")
        val validCookies = optional.filter { !reserved.contains(it.first) }

        var cookies = listOf(
            "csrftoken" to bearer?.csrftoken,
            "sessionid" to bearer?.meetSessionId,
            "meet_sessionid" to bearer?.meetSessionId
        ).filter { it.second != null }
            .joinToString(";") { "${it.first}=${it.second}" }

        if (validCookies.isNotEmpty()) {
            cookies += ";" + validCookies.joinToString(";") { "${it.first}=${it.second}" }
        }

        header("Cookie", cookies)
        if (null != bearer) {
            header("x-csrftoken", bearer.csrftoken)
        }

        header("Referer", host)
    }

    suspend inline fun <reified T> get(
        endpoint: String
    ): T {
        val bearer = getAuthent()

        val request = client.get("$prefix/$endpoint") {
            buildCookie(bearer)
        }

        request.ensureSuccess(endpoint)

        return request.body()
    }

    suspend inline fun <reified R> postUnit(
        endpoint: String,
        body: R
    ) {
        val bearer = getAuthent()

        val request = client.post("$prefix/$endpoint") {
            buildCookie(bearer)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        request.ensureSuccess(endpoint)
    }

    suspend inline fun <reified R, reified T> post(
        endpoint: String,
        body: R,
        cookies: List<Pair<String, String>> = emptyList()
    ): T {
        val bearer = getAuthent()

        val request = client.post("$prefix/$endpoint") {
            buildCookie(bearer, cookies)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        request.ensureSuccess(endpoint)

        return request.body()
    }

    /**
     * POST authenticated by a LiveKit token in the `Authorization: Bearer` header
     * (consumed by the backend `LiveKitTokenAuthentication`), in addition to the
     * usual session cookies. Used by endpoints that authenticate the participant
     * via their LiveKit token rather than the Meet session (e.g. `toggle-hand`).
     */
    suspend inline fun <reified R, reified T> postWithBearer(
        endpoint: String,
        body: R,
        bearerToken: String
    ): T {
        val bearer = getAuthent()

        val request = client.post("$prefix/$endpoint") {
            buildCookie(bearer)
            header("Authorization", "Bearer $bearerToken")
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        request.ensureSuccess(endpoint)

        return request.body()
    }

    suspend inline fun <reified R, reified T> put(
        endpoint: String,
        body: R
    ): T {
        val bearer = getAuthent()

        val request = client.put("$prefix/$endpoint") {
            buildCookie(bearer)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        request.ensureSuccess(endpoint)

        return request.body()
    }

    suspend inline fun <reified R, reified T> patch(
        endpoint: String,
        body: R
    ): T {
        val bearer = getAuthent()

        val request = client.patch("$prefix/$endpoint") {
            buildCookie(bearer)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        request.ensureSuccess(endpoint)

        return request.body()
    }

    suspend fun delete(
        endpoint: String
    ) {
        val bearer = getAuthent()

        val request = client.delete("$prefix/$endpoint") {
            buildCookie(bearer)
            contentType(ContentType.Application.Json)
        }

        request.ensureSuccess(endpoint)
    }

    suspend inline fun <reified R, reified T> delete(
        endpoint: String,
        body: R
    ): T {
        val bearer = getAuthent()

        val request = client.delete("$prefix/$endpoint") {
            buildCookie(bearer)
            contentType(ContentType.Application.Json)
            setBody(body)
        }

        request.ensureSuccess(endpoint)

        return request.body()
    }
}
