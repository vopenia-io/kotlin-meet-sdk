package io.vopenia.sdk.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.vopenia.livekit.compose.CameraPreviewView as CPV

@Composable
fun CameraPreviewView(
    modifier: Modifier,
    scaleType: ScaleType,
    isMirror: Boolean = false,
) = CPV(
    modifier,
    scaleType = when (scaleType) {
        ScaleType.Fill -> com.vopenia.livekit.compose.ScaleType.Fill
        ScaleType.Fit -> com.vopenia.livekit.compose.ScaleType.Fit
    },
    isMirror = isMirror
)