package io.vopenia.sdk.room

import io.vopenia.api.rooms.models.Access
import io.vopenia.api.rooms.models.ApiRoom
import io.vopenia.api.rooms.models.Livekit
import io.vopenia.api.rooms.models.RequestEntryAnswer
import io.vopenia.api.rooms.models.RoomAccessLevel
import io.vopenia.sdk.Session

data class Room(
    private val session: Session,
    val originalRoom: ApiRoom,
    val id: String
) {
    private val liveKitRoom = com.vopenia.sdk.Room()
    private var currentRequestEntryManager: RequestEntryManagement? = null

    val connectionState = liveKitRoom.connectionState
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
        get() = internalRoom.accessLevel

    // val language: String
    // get() = internalRoom.id

    val accesses: List<Access>
        get() = internalRoom.accesses

    val livekit: Livekit?
        get() = currentRequestEntryManager?.currentRequestEntryStatus?.livekit
            ?: internalRoom.livekit

    val isAadministrable: Boolean
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

    private suspend fun requestEntry(previousRequest: RequestEntryAnswer) =
        session.api.rooms.requestEntry(id, previousRequest)

    suspend fun acceptWaitingParticipant(participantId: String, accept: Boolean) {
        session.api.rooms.validateParticipantEntry(id, participantId, accept)
    }

    suspend fun waitingParticipants(): List<RequestEntryAnswer> {
        try {
            return session.api.rooms.waitingParticipants(id).participants
        } catch (err: Throwable) {
            // nothing
            return emptyList()
        }
    }

    class RequestEntryManagement(
        private val room: Room,
        private val session: Session,
        originalRequestEntry: RequestEntryAnswer
    ) {
        var currentRequestEntryStatus: RequestEntryAnswer = originalRequestEntry
            private set

        suspend fun checkEntry(): RequestEntryAnswer {
            return session.api.rooms.requestEntry(room.id, currentRequestEntryStatus).also {
                currentRequestEntryStatus = it
            }
        }
    }
}