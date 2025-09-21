package io.vopenia.sdk.room

import com.vopenia.sdk.utils.Dispatchers
import com.vopenia.sdk.utils.map
import io.vopenia.api.rooms.models.ApiRequestEntryAnswer
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.Livekit
import io.vopenia.sdk.Session
import kotlinx.coroutines.CoroutineScope

data class Room(
    private val session: Session,
    val originalRoom: ApiRoom,
    val id: String
) {
    private val scope = CoroutineScope(Dispatchers.IO)
    internal val liveKitRoom = com.vopenia.livekit.Room()
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

    //val configuration: JsonElement
    //    get() = internalRoom.configuration

    val accessLevel: RoomAccessLevel
        get() = internalRoom.accessLevel.to()

    // val language: String
    // get() = internalRoom.id

    val accesses: List<Access>
        get() = internalRoom.accesses.map { it.to() }

    private val livekit: Livekit?
        get() = currentRequestEntryManager?.currentApiRequestEntryStatus?.livekit
            ?: internalRoom.livekit

    val isAdministrable: Boolean
        get() = internalRoom.isAadministrable

    suspend fun connect() {
        if (null == livekit) throw IllegalStateException("Can't connect without livekit credentials")
        livekit?.let {
            liveKitRoom.connect(
                it.url,
                it.token
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
    )

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