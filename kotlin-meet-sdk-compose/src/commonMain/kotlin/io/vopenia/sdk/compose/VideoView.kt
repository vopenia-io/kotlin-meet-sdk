package io.vopenia.sdk.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vopenia.livekit.participant.track.IVideoTrack
import io.vopenia.sdk.room.Room
import io.vopenia.sdk.utils.RoomProxyAccessor

@Composable
fun VideoView(
    modifier: Modifier,
    room: Room,
    track: IVideoTrack,
    scaleType: ScaleType,
    isMirror: Boolean = false,
) {
    com.vopenia.livekit.compose.VideoView(
        modifier = modifier,
        room = RoomProxyAccessor.getRoom(room) as com.vopenia.livekit.Room,
        track = track,
        scaleType = when (scaleType) {
            ScaleType.Fill -> com.vopenia.livekit.compose.ScaleType.Fill
            ScaleType.Fit -> com.vopenia.livekit.compose.ScaleType.Fit
        },
        isMirror = isMirror
    )
}