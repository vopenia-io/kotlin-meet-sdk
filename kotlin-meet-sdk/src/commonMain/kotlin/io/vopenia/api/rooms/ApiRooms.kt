package io.vopenia.api.rooms

import io.ktor.client.HttpClient
import io.vopenia.api.rooms.models.InviteEmails
import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.api.rooms.models.RequestEntryAnswer
import io.vopenia.api.rooms.models.RequestEntryParameter
import io.vopenia.api.rooms.models.Room
import io.vopenia.api.rooms.models.RoomEnterParameter
import io.vopenia.api.rooms.models.WaitingParticipants
import io.vopenia.api.utils.AbstractApi
import io.vopenia.api.utils.Page
import io.vopenia.client.AuthenticationInformation

class ApiRooms(
    client: HttpClient,
    prefix: String,
    getAuthent: suspend () -> AuthenticationInformation
) {
    private val wrapper = AbstractApi(client, prefix, getAuthent)

    /**
     * API endpoints to access and perform actions on rooms. Create a new room
     */
    suspend fun createRoom(param: NewRoomParam): Room = wrapper.post("rooms/", param)

    /**
     * API endpoints to access and perform actions on rooms. Create a new room with a specific id
     */
    suspend fun createRoom(id: String, param: NewRoomParam): Room =
        wrapper.put("rooms/$id", param)

    /**
     * API endpoints to access and perform actions on rooms. Update an existing room
     */
    suspend fun updateRoom(id: String, param: NewRoomParam): Room =
        wrapper.patch("rooms/$id/", param)

    /**
     * API endpoints to access and perform actions on rooms. Update an existing room
     */
    suspend fun deleteRoom(id: String) = wrapper.delete("rooms/$id/")

    /**
     * Limit listed rooms to the ones related to the authenticated user.
     */
    suspend fun rooms(): Page<Room> = wrapper.get("rooms/")

    /**
     * Invite specific participants given their email
     */
    suspend fun invite(
        id: String,
        emails: List<String>
    ) = wrapper.postUnit(
        "rooms/$id/invite/",
        InviteEmails(emails)
    )

    /**
     * Invite specific participants given their email
     */
    suspend fun invite(
        id: String,
        vararg emails: String
    ) = wrapper.postUnit(
        "rooms/$id/invite/",
        InviteEmails(emails.toList())
    )

    /**
     * Validate a specific participant entry. allowEntry will enable or deny said participant's entry
     *
     * Note : named /entry/ in the original product
     */
    suspend fun validateParticipantEntry(
        id: String,
        participantId: String,
        allowEntry: Boolean
    ) = wrapper.postUnit(
        "rooms/$id/enter/",
        RoomEnterParameter(participantId, allowEntry)
    )

    /**
     * Send a request to enter the room after an initial request
     */
    suspend fun requestEntry(
        id: String,
        previousRequest: RequestEntryAnswer
    ): RequestEntryAnswer = wrapper.post(
        "rooms/$id/request-entry/",
        RequestEntryParameter(previousRequest.username),
        listOf("lobbyParticipantId" to previousRequest.id)
    )

    /**
     * Send a request to enter the room. To make sure to be able to track the request,
     * use this method and then switch to requestEntry(id, requestEntryAnswer)
     */
    suspend fun requestEntry(
        id: String,
        userName: String
    ): RequestEntryAnswer = wrapper.post(
        "rooms/$id/request-entry/",
        RequestEntryParameter(userName)
    )

    /**
     * Get the list of waiting participants
     */
    suspend fun waitingParticipants(
        id: String
    ): WaitingParticipants = wrapper.get(
        "rooms/$id/waiting-participants/"
    )
}
