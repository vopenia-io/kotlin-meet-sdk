package io.vopenia.api.rooms

import io.ktor.client.HttpClient
import io.ktor.http.HttpStatusCode
import io.ktor.http.encodeURLQueryComponent
import io.vopenia.api.AuthenticationInformation
import io.vopenia.api.rooms.models.ApiAccess
import io.vopenia.api.rooms.models.ApiMuteParticipantParam
import io.vopenia.api.rooms.models.ApiOperationResponse
import io.vopenia.api.rooms.models.ApiPatchRoomParam
import io.vopenia.api.rooms.models.ApiRecordingMode
import io.vopenia.api.rooms.models.ApiRemoveParticipantParam
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.ApiRoomAccessLevel
import io.vopenia.api.rooms.models.Livekit
import io.vopenia.api.rooms.models.RaiseHandParam
import io.vopenia.api.rooms.models.ApiStartRecordingParam
import io.vopenia.api.rooms.models.ApiUpdateParticipantParam
import io.vopenia.api.rooms.models.InviteEmails
import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.api.rooms.models.ApiRequestEntryAnswer
import io.vopenia.api.rooms.models.RequestEntryParameter
import io.vopenia.api.rooms.models.RoomEnterParameter
import io.vopenia.api.rooms.models.WaitingParticipants
import io.vopenia.api.utils.AbstractApi
import io.vopenia.api.utils.ApiException
import io.vopenia.api.utils.Page
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

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
    suspend fun room(slug: String, username: String? = null): ApiRoom? = try {
        // Pass the display name as ?username= so the backend mints the LiveKit
        // token with it (with_name(username or default)). Without it an
        // unauthenticated guest gets name="Anonymous". Mirrors Meet Web's
        // fetchRoom(`/rooms/${roomId}?username=`).
        val query = username
            ?.takeIf { it.isNotBlank() }
            ?.let { "?username=${it.encodeURLQueryComponent()}" }
            ?: ""
        // Decode into a LENIENT DTO. For an unregistered (ad-hoc) room the backend
        // returns 200 with {id:null, livekit:{room:slug,token,...}} and OMITS
        // name/slug/access_level/is_administrable (ALLOW_UNREGISTERED_ROOMS). A
        // strict ApiRoom (all those fields non-null) cannot decode that body and
        // would throw; normalize it into a complete, connectable ApiRoom — mirroring
        // Meet Web, which joins purely on the returned livekit token regardless of id.
        // Registered rooms come back fully populated and pass straight through.
        wrapper.get<ApiRoomResponse>("rooms/${slug}$query").toApiRoom(requestedSlug = slug)
    } catch (err: ApiException) {
        // A genuine 404 (room absent AND unregistered rooms disabled) -> null so the
        // caller may decide to create. Every OTHER failure (auth, transport, 5xx)
        // must propagate: the previous catch(Throwable){null} swallowed them all,
        // so a valid-but-unparsed token response looked like "not found" and callers
        // wrongly fell through to creating the room (which a guest cannot do).
        if (err.status == HttpStatusCode.NotFound) null else throw err
    }

    /**
     * Lenient mirror of the room GET response. All fields are optional so it can
     * decode BOTH the full registered-room body and the minimal unregistered-room
     * body {id:null, livekit:{...}}. Normalized into a complete [ApiRoom] by
     * [toApiRoom].
     */
    @Serializable
    internal data class ApiRoomResponse(
        val id: String? = null,
        val name: String? = null,
        val slug: String? = null,
        val configuration: JsonElement? = null,
        @SerialName("access_level")
        val accessLevel: ApiRoomAccessLevel? = null,
        val accesses: List<ApiAccess> = emptyList(),
        val livekit: Livekit? = null,
        @SerialName("is_administrable")
        val isAdministrable: Boolean = false,
    )

    private fun ApiRoomResponse.toApiRoom(requestedSlug: String): ApiRoom {
        // For an unregistered room the backend sets livekit.room to the slug and
        // leaves id/name/slug null. Use the slug we requested as the stable
        // identifier so the Room is connectable (connect() only needs livekit
        // url+token; id/slug are used by lobby/admin ops that don't apply to a
        // directly-connectable public room).
        val effectiveSlug = slug
            ?: livekit?.room?.takeIf { it.isNotBlank() }
            ?: requestedSlug
        return ApiRoom(
            id = id ?: effectiveSlug,
            name = name ?: effectiveSlug,
            slug = effectiveSlug,
            configuration = configuration,
            accessLevel = accessLevel ?: ApiRoomAccessLevel.Public,
            accesses = accesses,
            livekit = livekit,
            isAdministrable = isAdministrable,
        )
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
     * Partial update of an existing room — only the non-null fields of [param]
     * are sent to the backend. Used in particular to mutate `configuration`
     * (e.g. `can_publish_sources` for host commands) without re-asserting
     * `name` / `access_level`.
     */
    suspend fun patchRoom(id: String, param: ApiPatchRoomParam): ApiRoom =
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

    /**
     * Raise or lower the CURRENT participant's hand via the backend `toggle-hand`
     * endpoint, authenticated by the LiveKit [token] (the deployed backend revokes
     * `canUpdateOwnMetadata`, so clients cannot write the `handRaisedAt` attribute
     * directly). The participant is identified from the token claims server-side.
     * Mirrors Meet Web `updateRaiseHand.ts`.
     */
    suspend fun toggleHand(
        id: String,
        raised: Boolean,
        token: String
    ): ApiOperationResponse = wrapper.postWithBearer(
        "rooms/$id/toggle-hand/",
        RaiseHandParam(raised),
        token
    )

    @Serializable
    private object EmptyBody
}
