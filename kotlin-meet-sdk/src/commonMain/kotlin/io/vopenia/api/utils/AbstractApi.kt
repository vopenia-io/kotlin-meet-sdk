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
import io.ktor.http.contentType
import io.ktor.http.isSuccess
import io.vopenia.client.AuthenticationInformation

class AbstractApi(
    val client: HttpClient,
    val prefix: String,
    val getAuthent: suspend () -> AuthenticationInformation
) {
    fun HttpRequestBuilder.buildCookie(
        bearer: AuthenticationInformation,
        optional: List<Pair<String, String>> = emptyList()
    ) {
        val reserved = listOf("csrftoken", "meet_sessionid")
        val validCookies = optional.filter { !reserved.contains(it.first) }

        var cookies = "csrftoken=${bearer.csrftoken};meet_sessionid=${bearer.meetSessionId}"

        if (validCookies.isNotEmpty()) {
            cookies += ";" + validCookies.joinToString(";") { "${it.first}=${it.second}" }
        }

        header("Cookie", cookies)
        header("x-csrftoken", bearer.csrftoken)
    }

    suspend inline fun <reified T> get(
        endpoint: String
    ): T {
        val bearer = getAuthent()

        val request = client.get("$prefix/$endpoint") {
            buildCookie(bearer)
        }

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }

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

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }
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

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }

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

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }

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

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }

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

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }
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

        if (!request.status.isSuccess()) {
            throw IllegalStateException("Issue with $endpoint, answer ${request.status}")
        }

        return request.body()
    }
}
