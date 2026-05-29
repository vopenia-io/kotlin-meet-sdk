package io.vopenia.sdk.room

import io.vopenia.api.rooms.models.ApiRequestEntryAnswer
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.ApiUpdateParticipantParam
import io.vopenia.api.rooms.models.Livekit
import io.vopenia.api.rooms.models.NewRoomParam
import io.vopenia.livekit.participant.transcription.TranscriptionSegment
import io.vopenia.sdk.Session
import io.vopenia.sdk.room.chat.ChatMessage
import io.vopenia.sdk.room.chat.toFacade
import io.vopenia.sdk.room.reactions.MeetNotificationEnvelope
import io.vopenia.sdk.room.reactions.MeetReactionData
import io.vopenia.sdk.room.reactions.MeetReactionEmoji
import io.vopenia.sdk.room.reactions.Reaction
import io.vopenia.sdk.room.recording.RecordingHandle
import io.vopenia.sdk.room.recording.RecordingMode
import io.vopenia.sdk.room.recording.RecordingState
import io.vopenia.sdk.room.transcription.TranscriptionHandle
import io.vopenia.sdk.utils.Dispatchers
import io.vopenia.sdk.utils.map
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.transform
import kotlinx.serialization.json.Json

// Meet Web (and the LiveKit Components convention) uses `handRaisedAt` whose
// value is an ISO-8601 timestamp string when the hand is raised, and an empty
// string when lowered. Mirror this exactly for Web ↔ Mobile interop.
private const val HAND_RAISED_ATTRIBUTE = "handRaisedAt"

// Meet Web sends and receives reactions / other notifications as JSON
// payloads on the default LiveKit data channel (no topic), with a top-level
// `type` discriminator. The topic field of `publishData` is left null.
private const val MEET_REACTION_TYPE = "reactionReceived"

@OptIn(ExperimentalCoroutinesApi::class)
data class Room(
    private val session: Session,
    val originalRoom: ApiRoom,
    val id: String
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    internal val liveKitRoom = io.vopenia.livekit.Room()
    private var currentRequestEntryManager: RequestEntryManagement? = null

    val connectionState = liveKitRoom.connectionState.map(scope) { it.to() }
    val localParticipant = liveKitRoom.localParticipant
    val remoteParticipant = liveKitRoom.remoteParticipants

    val overriddenUsername: String?
        get() = currentRequestEntryManager?.currentRequestEntryStatus?.username

    internal var internalRoom: ApiRoom = originalRoom
        set(value) {
            // TODO event ?
            field = value
        }

    val name: String
        get() = internalRoom.name

    val slug: String
        get() = internalRoom.slug

    val accessLevel: RoomAccessLevel
        get() = internalRoom.accessLevel.to()

    val accesses: List<Access>
        get() = internalRoom.accesses.map { it.to() }

    private val livekit: Livekit?
        get() = currentRequestEntryManager?.currentApiRequestEntryStatus?.livekit
            ?: internalRoom.livekit

    val isAdministrable: Boolean
        get() = internalRoom.isAdministrable

    // -- Chat ----------------------------------------------------------------

    /**
     * Live chat messages flowing through the room. Aggregates messages emitted
     * locally (via [sendChatMessage]) and received from remote participants on
     * LiveKit's reserved `lk-chat-topic` data channel. No persistence — consumers
     * maintain their own history if needed (same model as the Meet Web frontend).
     */
    val chatMessages: Flow<ChatMessage> = merge(
        localParticipant.chatMessages.map { it.toFacade() },
        remoteParticipant.flatMapLatest { participants ->
            if (participants.isEmpty()) flowOf()
            else merge(*participants.map { it.chatMessages.map { msg -> msg.toFacade() } }.toTypedArray())
        }
    )

    /**
     * Send a chat message visible to all participants in the room.
     * Returns the locally-built [ChatMessage] (also emitted on [chatMessages]).
     */
    suspend fun sendChatMessage(text: String): ChatMessage =
        localParticipant.sendChatMessage(text).toFacade()

    // -- Reactions -----------------------------------------------------------

    /**
     * Ephemeral reactions flowing through the room. Decoded from the default
     * LiveKit data channel (no topic), filtering on the Meet `{type, data}`
     * notification envelope.
     */
    val reactions: Flow<Reaction> = merge(
        localParticipant.dataReceived,
        remoteParticipant.flatMapLatest { participants ->
            if (participants.isEmpty()) flowOf()
            else merge(*participants.map { it.dataReceived }.toTypedArray())
        }
    ).transform { packet ->
        // Meet Web publishes reactions with no topic — be permissive so we
        // accept both null and any (older sdks may send with topic).
        val decoded = runCatching {
            Json.decodeFromString(MeetNotificationEnvelope.serializer(), packet.payload.decodeToString())
        }.getOrNull() ?: return@transform
        if (decoded.type != MEET_REACTION_TYPE) return@transform
        val emoji = decoded.data?.emoji ?: return@transform
        emit(Reaction(packet.senderIdentity, emoji, currentTimeMillis()))
    }

    /**
     * Broadcast a reaction using one of the typed [MeetReactionEmoji] entries.
     * Prefer this overload over [sendReaction] (String) when possible — it
     * guarantees the wire value is one that Meet Web will display.
     */
    suspend fun sendReaction(emoji: MeetReactionEmoji) = sendReaction(emoji.key)

    /**
     * Broadcast an emoji reaction in the format Meet Web understands:
     * a JSON envelope `{"type":"reactionReceived","data":{"emoji":"..."}}`
     * published on LiveKit's reliable data channel without a topic.
     *
     * The [emoji] string should be one of [MeetReactionEmoji]'s `key` values
     * if Web interop matters — Meet Web silently drops unknown keys.
     */
    suspend fun sendReaction(emoji: String) {
        val envelope = MeetNotificationEnvelope(
            type = MEET_REACTION_TYPE,
            data = MeetReactionData(emoji = emoji)
        )
        val bytes = Json.encodeToString(MeetNotificationEnvelope.serializer(), envelope)
            .encodeToByteArray()
        localParticipant.publishData(bytes, reliable = true, topic = null)
    }

    // -- Raise hand ----------------------------------------------------------

    /**
     * Map of `participantIdentity -> handRaised` aggregated from LiveKit
     * participant attributes (`handRaised` key). The local participant is
     * included. Use the value as the source of truth for the UI.
     */
    val handStates: StateFlow<Map<String, Boolean>> = combine(
        localParticipant.state,
        remoteParticipant.flatMapLatest { participants ->
            if (participants.isEmpty()) flowOf(emptyList())
            else combine(participants.map { remote ->
                remote.state.map { state -> remote.identity to state.attributes }
            }) { it.toList() }
        }
    ) { localState, remoteList ->
        buildMap {
            localParticipant.identity?.let { put(it, localState.attributes.isHandRaised()) }
            remoteList.forEach { (identity, attrs) ->
                if (identity != null) put(identity, attrs.isHandRaised())
            }
        }
    }.stateIn(scope, SharingStarted.Eagerly, emptyMap())

    private fun Map<String, String>.isHandRaised(): Boolean {
        val value = this[HAND_RAISED_ATTRIBUTE] ?: return false
        // Meet Web convention: presence of a non-empty ISO timestamp = raised.
        return value.isNotEmpty()
    }

    /**
     * Raise or lower the local participant's hand. Backed by the LiveKit
     * participant attribute `handRaisedAt`, whose value is the ISO-8601
     * timestamp of the raise event (or empty string when lowered) — matching
     * the convention used by the Meet Web frontend (`hooks/useRaisedHand.ts`).
     */
    suspend fun raiseHand(raised: Boolean) {
        val value = if (raised) currentTimeMillisToIso() else ""
        localParticipant.updateAttributes(mapOf(HAND_RAISED_ATTRIBUTE to value))
    }

    // -- Recording -----------------------------------------------------------

    private val recordingStateFlow: MutableStateFlow<RecordingState> =
        MutableStateFlow(RecordingState.Idle)

    val recordingState: StateFlow<RecordingState> = recordingStateFlow.asStateFlow()

    /**
     * Start an Egress recording on the room. Returns a [RecordingHandle] tracking
     * the in-flight recording. Admin/owner only.
     */
    suspend fun startRecording(
        mode: RecordingMode = RecordingMode.ScreenRecording
    ): RecordingHandle {
        try {
            session.api.rooms.startRecording(id, mode.toApi())
        } catch (err: Throwable) {
            recordingStateFlow.value = RecordingState.Failed(err)
            throw err
        }
        val handle = RecordingHandle(this, mode, currentTimeMillis())
        recordingStateFlow.value = RecordingState.Recording(handle)
        return handle
    }

    /**
     * Stop the recording currently in flight on the room.
     */
    suspend fun stopRecording() {
        val previous = recordingStateFlow.value
        recordingStateFlow.value = RecordingState.Stopping
        try {
            session.api.rooms.stopRecording(id)
            recordingStateFlow.value = RecordingState.Idle
        } catch (err: Throwable) {
            recordingStateFlow.value = previous
            throw err
        }
    }

    // -- Transcription -------------------------------------------------------

    /**
     * Live transcription segments emitted by the LiveKit ASR agent. Aggregates
     * segments from every participant (local + remote). Triggered server-side
     * by [startTranscription].
     */
    val transcription: Flow<TranscriptionSegment> = merge(
        localParticipant.transcripts,
        remoteParticipant.flatMapLatest { participants ->
            if (participants.isEmpty()) flowOf()
            else merge(*participants.map { it.transcripts }.toTypedArray())
        }
    )

    /**
     * Dispatch the backend transcription agent for the room. The `language`
     * argument is preserved in the returned handle but not yet propagated to the
     * backend — the agent's language is configured server-side. Admin/owner only.
     */
    suspend fun startTranscription(language: String): TranscriptionHandle {
        session.api.rooms.startSubtitle(id)
        return TranscriptionHandle(language)
    }

    // -- Admin actions -------------------------------------------------------

    /**
     * Server-side mute of a single track for a remote participant.
     * Admin/owner only.
     */
    suspend fun muteParticipantTrack(participantIdentity: String, trackSid: String) {
        session.api.rooms.muteParticipant(id, participantIdentity, trackSid)
    }

    /**
     * Update server-side attributes for a remote participant. Admin/owner only.
     */
    suspend fun updateParticipantAttributes(
        participantIdentity: String,
        attributes: Map<String, String>
    ) {
        session.api.rooms.updateParticipant(
            id,
            ApiUpdateParticipantParam(participantIdentity, attributes = attributes)
        )
    }

    /**
     * Forcibly disconnect a remote participant from the room. Admin/owner only.
     */
    suspend fun removeParticipant(participantIdentity: String) {
        session.api.rooms.removeParticipant(id, participantIdentity)
    }

    /**
     * Change the room access level (`public` / `trusted` / `restricted`).
     * Admin/owner only. Persisted server-side via `PATCH rooms/{id}/` and applies
     * to future occurrences of the meeting. Updates [accessLevel] on success.
     *
     * Reuses [io.vopenia.api.rooms.ApiRooms.updateRoom] (the proven PATCH path),
     * re-posting the current [name] alongside the new level — `NewRoomParam`
     * requires a name. A dedicated partial-patch param (also carrying
     * `configuration.can_publish_sources`) is the documented next step.
     */
    suspend fun setAccessLevel(level: RoomAccessLevel) {
        internalRoom = session.api.rooms.updateRoom(
            id,
            NewRoomParam(name = internalRoom.name, accessLevel = level.toApi())
        )
    }

    // -- Connection lifecycle ------------------------------------------------

    suspend fun connect(enableMicrophone: Boolean = true) {
        if (null == livekit) throw IllegalStateException("Can't connect without livekit credentials")
        livekit?.let {
            liveKitRoom.connect(
                url = it.url,
                token = it.token,
                enableMicrophone = enableMicrophone
            )
        }
    }

    fun disconnect() {
        if (null == livekit) throw IllegalStateException("Can't disconnect without livekit credentials")

        liveKitRoom.disconnect()
    }

    suspend fun requestEntry(userName: String) = RequestEntryManagement(
        this,
        session,
        session.api.rooms.requestEntry(id, userName)
    ).also { currentRequestEntryManager = it }

    private suspend fun requestEntry(previousRequest: ApiRequestEntryAnswer) =
        session.api.rooms.requestEntry(id, previousRequest)

    suspend fun acceptWaitingParticipant(participantId: String, accept: Boolean) {
        session.api.rooms.validateParticipantEntry(id, participantId, accept)
    }

    suspend fun waitingParticipants(): List<RequestEntryAnswer> {
        return try {
            session.api.rooms.waitingParticipants(id).participants.map { it.to() }
        } catch (err: Throwable) {
            // nothing
            emptyList()
        }
    }

    class RequestEntryManagement(
        private val room: Room,
        private val session: Session,
        originalRequestEntry: ApiRequestEntryAnswer
    ) {
        internal var currentApiRequestEntryStatus: ApiRequestEntryAnswer = originalRequestEntry
            private set

        var currentRequestEntryStatus: RequestEntryAnswer = originalRequestEntry.to()
            private set

        suspend fun checkEntry(): RequestEntryAnswer {
            return session.api.rooms.requestEntry(room.id, currentApiRequestEntryStatus).let {
                currentApiRequestEntryStatus = it
                currentRequestEntryStatus = it.to()
                it.to()
            }
        }
    }
}
