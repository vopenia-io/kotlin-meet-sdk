package moe.tlaster.precompose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.foundation.layout.Box
import moe.tlaster.precompose.navigation.transition.NavTransition

private typealias SceneContent = @Composable (BackStackEntry) -> Unit

class NavGraphBuilder {
    internal val scenes = linkedMapOf<String, SceneContent>()

    fun scene(
        route: String,
        navTransition: NavTransition? = null,
        swipeProperties: SwipeProperties? = null,
        content: SceneContent
    ) {
        scenes[route] = content
    }
}

@Composable
fun NavHost(
    navigator: Navigator,
    initialRoute: String,
    modifier: Modifier = Modifier,
    navTransition: NavTransition? = null,
    content: NavGraphBuilder.() -> Unit
) {
    val graph = NavGraphBuilder().apply(content)
    val currentEntry by navigator.currentEntry.collectAsState()

    LaunchedEffect(navigator, initialRoute) {
        if (navigator.currentEntry.value == null) {
            navigator.navigate(initialRoute)
        }
    }

    Box(modifier = modifier) {
        val entry = currentEntry ?: return@Box
        graph.scenes[entry.route.route]?.invoke(entry)
    }
}
