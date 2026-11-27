package de.velwebsolutions.mildcrowd.app.ui

import androidx.compose.runtime.Composable
import de.velwebsolutions.mildcrowd.account.AccountScreen
import de.velwebsolutions.mildcrowd.chat.ChatScreen
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import de.velwebsolutions.mildcrowd.design.navigation.ChatRoute
import de.velwebsolutions.mildcrowd.design.navigation.LiveRoute
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.navigation.StaffRoute
import de.velwebsolutions.mildcrowd.live.LiveScreen
import de.velwebsolutions.mildcrowd.membership.MembershipScreen
import de.velwebsolutions.mildcrowd.network.NetworkScreen
import de.velwebsolutions.mildcrowd.staff.StaffScreen

/** Maps every route to the screen of the module that owns it. */
@Composable
fun AppDestination(route: AppRoute) {
    when (route) {
        is NetworkRoute -> NetworkScreen(route)

        is ChatRoute -> ChatScreen(route)

        is AccountRoute -> AccountScreen(route)

        is LiveRoute -> LiveScreen(route)

        is MembershipRoute -> MembershipScreen(route)

        is StaffRoute -> StaffScreen(route)

        // WebRoute: websites open in a Custom Tab (Navigator.openWeb) and are never pushed.
        else -> Unit
    }
}
