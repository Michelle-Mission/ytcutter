package app.ytcutter.core.navigation

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RoutesTest {

    @Test
    fun topLevelKeepsBottomBar() {
        assertFalse(Routes.hidesBottomBar(Route.Home.path))
        assertFalse(Routes.hidesBottomBar(Route.Downloads.path))
        assertFalse(Routes.hidesBottomBar(Route.Settings.path))
    }

    @Test
    fun detailRoutesHideBottomBar() {
        assertTrue(Routes.hidesBottomBar(Route.Components.path))
        assertTrue(Routes.hidesBottomBar(Route.About.path))
        assertTrue(Routes.hidesBottomBar(Route.Cookies.path))
        assertTrue(Routes.hidesBottomBar(null))
    }
}
