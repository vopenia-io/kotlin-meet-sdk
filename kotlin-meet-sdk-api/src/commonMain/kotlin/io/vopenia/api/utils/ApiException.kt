package io.vopenia.api.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

class ApiException(
    val status: HttpStatusCode,
    val endpoint: String,
    val body: String? = null,
) : Exception("Issue with $endpoint, answer $status${body?.takeIf { it.isNotBlank() }?.let { " — $it" } ?: ""}") {
    val isUnauthorized: Boolean get() = status == HttpStatusCode.Unauthorized
    val isForbidden: Boolean get() = status == HttpStatusCode.Forbidden
}

suspend fun HttpResponse.ensureSuccess(endpoint: String) {
    if (!status.isSuccess()) {
        // Surface the backend's error body (e.g. Django field-validation JSON)
        // so a 4xx is diagnosable instead of an opaque status code.
        val body = runCatching { bodyAsText() }.getOrNull()
        throw ApiException(status, endpoint, body)
    }
}
