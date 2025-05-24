package io.vopenia.api.users

import io.ktor.client.HttpClient
import io.vopenia.api.users.models.User
import io.vopenia.api.utils.AbstractApi
import io.vopenia.api.utils.Page
import io.vopenia.client.AuthenticationInformation

class ApiUsers(
    client: HttpClient,
    prefix: String,
    getAuthent: suspend () -> AuthenticationInformation
) {
    private val wrapper = AbstractApi(client, prefix, getAuthent)

    /**
     * Return information on currently logged user
     */
    suspend fun users() = wrapper.get<Page<User>>("users")

    /**
     * Return information on currently logged user
     */
    suspend fun me() = wrapper.get<User>("users/me")
}
