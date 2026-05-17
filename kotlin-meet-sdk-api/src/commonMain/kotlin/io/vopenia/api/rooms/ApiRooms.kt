package io.vopenia.api.rooms

import io.ktor.client.HttpClient
import io.vopenia.api.AuthenticationInformation
import io.vopenia.api.rooms.models.ApiMuteParticipantParam
import io.vopenia.api.rooms.models.ApiOperationResponse
import io.vopenia.api.rooms.models.ApiRecordingMode
import io.vopenia.api.rooms.models.ApiRemoveParticipantParam
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.ApiStartRecordingParam
import io.vopenia.api.rooms.models.ApiUpdateParticipantParam
import io.vopenia.api.rooms.models.InviteEmails
import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.api.rooms.models.ApiRequestEntryAnswer
import io.vopenia.api.rooms.models.RequestEntryParameter
import io.vopenia.api.rooms.models.RoomEnterParameter
import io.vopenia.api.rooms.models.WaitingParticipants
import io.vopenia.api.utils.AbstractApi
import io.vopenia.api.utils.Page
import kotlinx.serialization.Serializable

class ApiRooms(
    client: HttpClient,
    prefix: String,
    getAuthent: suspend () -> AuthenticationInformation?
) {
    private val wrapper = AbstractApi(client, prefix, getAuthent)

    /**
     * API endpoints to access and perform actions on rooms. Create a new room
     */
    suspend fun createRoom(param: NewRoomParam): ApiRoom = wrapper.post("rooms/", param)

    /**
     * API endpoints to access and perform actions on rooms. Create a new room
     */
    suspend fun room(slug: String): ApiRoom? = try {
        wrapper.get("rooms/${slug}")
    } catch (err: Throwable) {
        null
    }

    /**
     * API endpoints to access and perform actions on rooms. Create a new room with a specific id
     */
    suspend fun createRoom(id: String, param: NewRoomParam): ApiRoom =
        wrapper.put("rooms/$id", param)

    /**
     * API endpoints to access and perform actions on rooms. Update an existing room
     */
    suspend fun updateRoom(id: String, param: NewRoomParam): ApiRoom =
        wrapper.patch("rooms/$id/", param)

    /**
     * API endpoints to access and perform actions on rooms. Update an existing room
     */
    suspend fun deleteRoom(id: String) = wrapper.delete("rooms/$id/")

    /**
     * Limit listed rooms to the ones related to the authenticated user.
     */
    suspend fun rooms(): Page<ApiRoom> = wrapper.get("rooms/")

    /**
     * Limit listed rooms to the ones related to the authenticated user.
     */
    suspend fun rooms(page: Int): Page<ApiRoom> = wrapper.get("rooms/?page=$page")

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
        previousRequest: ApiRequestEntryAnswer
    ): ApiRequestEntryAnswer = wrapper.post(
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
    ): ApiRequestEntryAnswer = wrapper.post(
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

    /**
     * Start an Egress recording for the room.
     *
     * @param mode determines the produced artefact:
     *   - [ApiRecordingMode.SCREEN_RECORDING] produces an MP4 of the composited video.
     *   - [ApiRecordingMode.TRANSCRIPT] captures audio for offline ASR/transcript.
     *
     * Requires the caller to be an admin/owner of the room. The backend returns a
     * status message; the resulting recording can later be retrieved via
     * `ApiRecordings.recordings()` once the Egress reports completion.
     */
    suspend fun startRecording(id: String, mode: ApiRecordingMode): ApiOperationResponse =
        wrapper.post(
            "rooms/$id/start-recording/",
            ApiStartRecordingParam(mode)
        )

    /**
     * Stop the currently active recording on the room.
     */
    suspend fun stopRecording(id: String): ApiOperationResponse =
        wrapper.post(
            "rooms/$id/stop-recording/",
            EmptyBody
        )

    /**
     * Trigger live subtitle / transcription dispatching for the room. The backend
     * dispatches a LiveKit ASR agent; segments arrive on each participant's
     * LiveKit transcription channel and are exposed via `Room.transcription` flow.
     *
     * No explicit stop endpoint is provided — the agent terminates with the room.
     */
    suspend fun startSubtitle(id: String): ApiOperationResponse =
        wrapper.post(
            "rooms/$id/start-subtitle/",
            EmptyBody
        )

    /**
     * Server-side mute of a single track for a specific participant.
     * Admin/owner only.
     */
    suspend fun muteParticipant(
        id: String,
        participantIdentity: String,
        trackSid: String
    ): ApiOperationResponse = wrapper.post(
        "rooms/$id/mute-participant/",
        ApiMuteParticipantParam(participantIdentity, trackSid)
    )

    /**
     * Update server-side participant attributes / metadata / name.
     * Admin/owner only. Only the provided keys are updated.
     */
    suspend fun updateParticipant(
        id: String,
        param: ApiUpdateParticipantParam
    ): ApiOperationResponse = wrapper.post(
        "rooms/$id/update-participant/",
        param
    )

    /**
     * Disconnect a participant from the LiveKit room. Admin/owner only.
     */
    suspend fun removeParticipant(
        id: String,
        participantIdentity: String
    ): ApiOperationResponse = wrapper.post(
        "rooms/$id/remove-participant/",
        ApiRemoveParticipantParam(participantIdentity)
    )

    @Serializable
    private object EmptyBody
}
