package io.vopenia.api.utils

import io.ktor.client.statement.HttpResponse
import io.ktor.http.HttpStatusCode
import io.ktor.http.isSuccess

class ApiException(
    val status: HttpStatusCode,
    val endpoint: String
) : Exception("Issue with $endpoint, answer $status") {
    val isUnauthorized: Boolean get() = status == HttpStatusCode.Unauthorized
    val isForbidden: Boolean get() = status == HttpStatusCode.Forbidden
}

fun HttpResponse.ensureSuccess(endpoint: String) {
    if (!status.isSuccess()) throw ApiException(status, endpoint)
}
