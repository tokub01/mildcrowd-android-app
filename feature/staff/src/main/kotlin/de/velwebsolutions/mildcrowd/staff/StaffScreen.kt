package de.velwebsolutions.mildcrowd.staff

import androidx.compose.runtime.Composable
import de.velwebsolutions.mildcrowd.design.navigation.StaffRoute
import de.velwebsolutions.mildcrowd.staff.ui.admin.AdminDashboardScreen
import de.velwebsolutions.mildcrowd.staff.ui.admin.AdminIdeasScreen
import de.velwebsolutions.mildcrowd.staff.ui.admin.AdminPaymentsScreen
import de.velwebsolutions.mildcrowd.staff.ui.admin.DataBreachesScreen
import de.velwebsolutions.mildcrowd.staff.ui.admin.SystemScreen
import de.velwebsolutions.mildcrowd.staff.ui.moderation.CaseDetailScreen
import de.velwebsolutions.mildcrowd.staff.ui.moderation.ComplaintReviewScreen
import de.velwebsolutions.mildcrowd.staff.ui.moderation.ModerationOverviewScreen
import de.velwebsolutions.mildcrowd.staff.ui.moderation.RolesScreen
import de.velwebsolutions.mildcrowd.staff.ui.moderation.SuspensionsScreen

/**
 * Entry point of the Staff module: the screen for each [StaffRoute] (mapped in the app). The staff API itself decides
 * who may enter (staff role with confirmed two-factor sign-in, admins for the admin pages), so every screen shows the
 * server's answer – including the two-factor requirement with a way to the settings.
 */
@Composable
fun StaffScreen(route: StaffRoute) {
    when (route) {
        StaffRoute.Moderation -> ModerationOverviewScreen()

        is StaffRoute.ModerationCase -> CaseDetailScreen(route.id)

        is StaffRoute.ComplaintReview -> ComplaintReviewScreen(route.id)

        StaffRoute.Roles -> RolesScreen()

        StaffRoute.Suspensions -> SuspensionsScreen()

        StaffRoute.AdminDashboard -> AdminDashboardScreen()

        StaffRoute.AdminIdeas -> AdminIdeasScreen()

        StaffRoute.AdminPayments -> AdminPaymentsScreen()

        StaffRoute.AdminBreaches -> DataBreachesScreen()

        // StaffRoute.AdminSystem, the last route (an `else` instead of a check that could never fail).
        else -> SystemScreen()
    }
}
