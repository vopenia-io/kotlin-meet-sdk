package moe.tlaster.precompose

import androidx.compose.runtime.Composable

@Composable
fun PreComposeApp(
    content: @Composable () -> Unit
) {
    content()
}
