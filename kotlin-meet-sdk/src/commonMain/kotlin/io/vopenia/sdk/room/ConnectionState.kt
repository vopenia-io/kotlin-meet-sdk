package io.vopenia.sdk.room

import com.vopenia.livekit.events.ConnectionState as CS

sealed class ConnectionState {
    data object Default : ConnectionState()

    data object Connecting : ConnectionState()

    data object Connected : ConnectionState()

    data object Disconnected : ConnectionState()

    class ConnectionError(val error: Throwable) : ConnectionState()
}

fun com.vopenia.livekit.events.ConnectionState.to() = when (this) {
    CS.Connected -> ConnectionState.Connected
    CS.Connecting -> ConnectionState.Connecting
    CS.Default -> ConnectionState.Default
    CS.Disconnected -> ConnectionState.Disconnected
    is CS.ConnectionError -> ConnectionState.ConnectionError(
        this.error
    )
}
