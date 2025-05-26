package io.vopenia.app.content.navigation

enum class PossibleRoutes(val impl: Route) {
    Initialize(RouteInitialize()),
    Main(RouteMain()),
    Room(RouteRoom()),
    Settings(RouteSettings())
    ;

    companion object {
        fun fromRoute(route: String) = entries.firstOrNull { it.impl.route == route }
    }
}
