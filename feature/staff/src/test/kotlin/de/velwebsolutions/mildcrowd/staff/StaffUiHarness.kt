package de.velwebsolutions.mildcrowd.staff

import android.app.Application
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.test.core.app.ApplicationProvider
import com.github.takahirom.roborazzi.captureRoboImage
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme

/** What the staff UI tests share: a navigator on the profile tab that records opened websites, and screenshots. */
class StaffUiHarness(private val compose: ComposeContentTestRule) {
    val context: Application = ApplicationProvider.getApplicationContext()
    val web = mutableListOf<String>()
    val navigator = Navigator().apply {
        selectedTab = MainTab.Profile
        openWeb = { web += it }
    }

    /** The last route opened on the profile tab. */
    val top: AppRoute? get() = navigator.stack(MainTab.Profile).lastOrNull()

    fun show(content: @Composable () -> Unit) {
        compose.setContent { MildcrowdTheme { CompositionLocalProvider(LocalNavigator provides navigator) { content() } } }
    }

    fun shot(name: String) = compose.onRoot().captureRoboImage("build/outputs/roborazzi/staff-$name.png")

    fun shotDialog(name: String) = compose.onAllNodes(isRoot())[1].captureRoboImage("build/outputs/roborazzi/staff-$name.png")
}
