package de.velwebsolutions.mildcrowd.membership

import androidx.compose.runtime.Composable
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.membership.ui.CancelScreen
import de.velwebsolutions.mildcrowd.membership.ui.CompanionLinkScreen
import de.velwebsolutions.mildcrowd.membership.ui.InvoiceScreen
import de.velwebsolutions.mildcrowd.membership.ui.OverviewScreen
import de.velwebsolutions.mildcrowd.membership.ui.PerksScreen
import de.velwebsolutions.mildcrowd.membership.ui.PlayCheckoutScreen
import de.velwebsolutions.mildcrowd.membership.ui.PortalScreen
import de.velwebsolutions.mildcrowd.membership.ui.TransparencyScreen
import de.velwebsolutions.mildcrowd.membership.ui.WithdrawScreen

/** Entry point of the Membership module: the screen for each [MembershipRoute] (mapped in the app). */
@Composable
fun MembershipScreen(route: MembershipRoute) {
    when (route) {
        MembershipRoute.Overview -> OverviewScreen()

        MembershipRoute.Portal -> PortalScreen()

        is MembershipRoute.Invoice -> InvoiceScreen(route.paymentId)

        MembershipRoute.Cancel -> CancelScreen()

        MembershipRoute.Withdraw -> WithdrawScreen()

        MembershipRoute.Perks -> PerksScreen()

        is MembershipRoute.CompanionLink -> CompanionLinkScreen(route.token)

        MembershipRoute.Transparency -> TransparencyScreen()

        // MembershipRoute.Checkout, the last route (an `else` instead of a check that could never fail).
        else -> PlayCheckoutScreen()
    }
}
