package moe.tlaster.precompose.navigation

data class RouteDefinition(
    val route: String
)

data class BackStackEntry(
    val route: RouteDefinition
)

class SwipeProperties

sealed class PopUpTo {
    data class First(
        val inclusive: Boolean
    ) : PopUpTo()
}

data class NavOptions(
    val launchSingleTop: Boolean = false,
    val popUpTo: PopUpTo? = null
)
