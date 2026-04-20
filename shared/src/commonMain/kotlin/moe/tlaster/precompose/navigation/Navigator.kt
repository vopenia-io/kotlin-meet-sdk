package moe.tlaster.precompose.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class Navigator(
    private val name: String? = null
) {
    private val backStack = mutableListOf<BackStackEntry>()
    private val _currentEntry = MutableStateFlow<BackStackEntry?>(null)
    private val _canGoBack = MutableStateFlow(false)

    val currentEntry: StateFlow<BackStackEntry?> = _currentEntry.asStateFlow()
    val canGoBack: StateFlow<Boolean> = _canGoBack.asStateFlow()

    fun navigate(
        route: String,
        options: NavOptions = NavOptions()
    ) {
        applyPopUpTo(options.popUpTo)

        val currentRoute = backStack.lastOrNull()?.route?.route
        if (options.launchSingleTop && currentRoute == route) {
            _currentEntry.value = backStack.lastOrNull()
            return
        }

        val newEntry = BackStackEntry(RouteDefinition(route))
        backStack += newEntry
        publishState(newEntry)
    }

    fun popBackStack(): Boolean {
        if (backStack.isEmpty()) {
            return false
        }

        backStack.removeLast()
        publishState(backStack.lastOrNull())
        return true
    }

    fun goBack(): Boolean = popBackStack()

    private fun applyPopUpTo(popUpTo: PopUpTo?) {
        when (popUpTo) {
            is PopUpTo.First -> {
                if (backStack.isEmpty()) {
                    return
                }

                val retained = if (popUpTo.inclusive) {
                    emptyList()
                } else {
                    listOf(backStack.first())
                }

                backStack.clear()
                backStack.addAll(retained)
                publishState(backStack.lastOrNull())
            }

            null -> Unit
        }
    }

    private fun publishState(entry: BackStackEntry?) {
        _currentEntry.value = entry
        _canGoBack.value = backStack.size > 1
    }
}

@Composable
fun rememberNavigator(name: String? = null): Navigator = remember(name) {
    Navigator(name)
}
