package io.vopenia.app.utils.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material3.Card
import androidx.compose.material3.CardColors
import androidx.compose.material3.CardDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import de.drick.compose.hotpreview.HotPreview
import eu.codlab.compose.widgets.TextNormal
import io.vopenia.app.PreviewApp
import io.vopenia.app.theme.AppColor

@Composable
fun CardButton(
    modifier: Modifier = Modifier,
    leadingIcon: ButtonIcon? = null,
    text: String? = null,
    trailingIcon: ButtonIcon? = null,
    onClick: () -> Unit,
    colors: CardColors = CardDefaults.cardColors()
) {
    Card(
        modifier = modifier,
        onClick = onClick,
        colors = colors
    ) {
        Row(
            modifier = Modifier.padding(
                start = 16.dp,
                end = 16.dp,
                top = 8.dp,
                bottom = 8.dp
            ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            leadingIcon?.let {
                Image(
                    imageVector = it.imageVector,
                    contentDescription = it.contentDescription,
                    colorFilter = it.colorFilter
                )
            }

            text?.let {
                TextNormal(
                    text = it
                )
            }

            trailingIcon?.let {
                Image(
                    imageVector = it.imageVector,
                    contentDescription = it.contentDescription,
                    colorFilter = it.colorFilter
                )
            }
        }
    }
}

data class ButtonIcon(
    val imageVector: ImageVector,
    val colorFilter: ColorFilter,
    val contentDescription: String? = null,
)

@HotPreview(widthDp = 400, heightDp = 600, darkMode = true)
@HotPreview(widthDp = 400, heightDp = 600, darkMode = false)
@Composable
private fun CardButtonMicPreview() {
    PreviewApp {
        CardButton(
            modifier = Modifier.fillMaxSize(),
            leadingIcon = ButtonIcon(
                imageVector = Icons.Default.MicOff,
                colorFilter = ColorFilter.tint(Color.White)
            ),
            trailingIcon = ButtonIcon(
                imageVector = Icons.Default.Mic,
                colorFilter = ColorFilter.tint(Color.White)
            ),
            text = "Test",
            onClick = { /* */ },
            colors = CardDefaults.cardColors()
        )
    }
}

@HotPreview(widthDp = 400, heightDp = 600, darkMode = true)
@HotPreview(widthDp = 400, heightDp = 600, darkMode = false)
@Composable
private fun CardButtonCallPreview() {
    PreviewApp {
        CardButton(
            modifier = Modifier.fillMaxSize(),
            leadingIcon = ButtonIcon(
                imageVector = Icons.Default.CallEnd,
                colorFilter = ColorFilter.tint(Color.White)
            ),
            onClick = { /* */ },
            colors = CardDefaults.cardColors(
                containerColor = AppColor.Red,
            )
        )
    }
}
