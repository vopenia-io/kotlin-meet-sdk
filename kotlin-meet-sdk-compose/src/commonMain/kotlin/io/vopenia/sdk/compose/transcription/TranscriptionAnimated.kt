package io.vopenia.sdk.compose.transcription

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

@Composable
fun TranscriptionAnimated(
    modifier: Modifier,
    text: String,
    typingDelayInMs: Long = 50L,
    content: @Composable (Modifier, String) -> Unit
) {
    com.vopenia.livekit.compose.transcription.TranscriptionAnimated(
        modifier,
        text,
        typingDelayInMs,
        content
    )
}