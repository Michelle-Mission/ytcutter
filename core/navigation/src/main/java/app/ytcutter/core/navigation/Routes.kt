package app.ytcutter.core.navigation

sealed class Route(val path: String) {
    data object Home : Route("home")
    data object Downloads : Route("downloads")
    data object Settings : Route("settings")
    data object Components : Route("settings/components")
    data object About : Route("settings/about")
    data object Cookies : Route("cookies")
    data object Changelog : Route("settings/changelog")
}

object Routes {
    val topLevel = listOf(Route.Home, Route.Downloads, Route.Settings)

    private val topLevelPaths = topLevel.map { it.path }.toSet()

    fun hidesBottomBar(route: String?): Boolean = route !in topLevelPaths

    const val ARG_URL = "url"

    fun cookiesFromUrl(url: String): String = "cookies/url?url=$url"
    fun cookiesFromUrlSave(url: String): String = "cookies/url/save?url=$url"
}