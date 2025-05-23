package io.vopenia.api.users

import io.ktor.client.HttpClient
import io.vopenia.api.users.models.User
import io.vopenia.api.users.models.UserPage
import io.vopenia.api.utils.AbstractApi

class ApiUsers(
    client: HttpClient,
    prefix: String,
    getBearer: suspend () -> String
) {
    private val wrapper = AbstractApi(client, prefix, getBearer)

    /**
     * Return information on currently logged user
     */
    suspend fun users() = wrapper.get<UserPage>("users")

    /**
     * Return information on currently logged user
     */
    suspend fun me() = wrapper.get<User>("users/me")
}
