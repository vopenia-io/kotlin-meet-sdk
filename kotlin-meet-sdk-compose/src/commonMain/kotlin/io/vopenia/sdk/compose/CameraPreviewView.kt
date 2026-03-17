package io.vopenia.sdk.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import io.vopenia.livekit.compose.CameraPreviewView as CPV

@Composable
fun CameraPreviewView(
    modifier: Modifier,
    scaleType: ScaleType,
    isMirror: Boolean = false,
) = CPV(
    modifier,
    scaleType = when (scaleType) {
        ScaleType.Fill -> io.vopenia.livekit.compose.ScaleType.Fill
        ScaleType.Fit -> io.vopenia.livekit.compose.ScaleType.Fit
    },
    isMirror = isMirror
)