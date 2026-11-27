package de.velwebsolutions.mildcrowd.account

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewmodel.compose.viewModel
import de.velwebsolutions.mildcrowd.account.ui.auth.ConfirmPhoneContentScreen
import de.velwebsolutions.mildcrowd.account.ui.auth.SignedOutFlow
import de.velwebsolutions.mildcrowd.account.ui.auth.VerifyEmailContentScreen
import de.velwebsolutions.mildcrowd.account.ui.components.ScopedViewModels
import de.velwebsolutions.mildcrowd.account.ui.member.ComplaintScreen
import de.velwebsolutions.mildcrowd.account.ui.member.GlossaryScreen
import de.velwebsolutions.mildcrowd.account.ui.member.SuspendedScreen
import de.velwebsolutions.mildcrowd.account.ui.privacy.HiddenWordsScreen
import de.velwebsolutions.mildcrowd.account.ui.privacy.PrivacyCenterScreen
import de.velwebsolutions.mildcrowd.account.ui.privacy.PrivacyCheckupScreen
import de.velwebsolutions.mildcrowd.account.ui.privacy.ScreenTimeScreen
import de.velwebsolutions.mildcrowd.account.ui.releases.ReleasesScreen
import de.velwebsolutions.mildcrowd.account.ui.releases.UpdateNotesDialog
import de.velwebsolutions.mildcrowd.account.ui.safety.DevicesScreen
import de.velwebsolutions.mildcrowd.account.ui.safety.ReportScreen
import de.velwebsolutions.mildcrowd.account.ui.safety.SafetyScreen
import de.velwebsolutions.mildcrowd.account.ui.settings.SettingsScreen
import de.velwebsolutions.mildcrowd.account.ui.welcome.WelcomeScreen
import de.velwebsolutions.mildcrowd.design.components.LocalScreenChrome
import de.velwebsolutions.mildcrowd.design.components.ScreenChrome
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.Navigator

/** Entry point of the Account module: the screen for each [AccountRoute] (mapped in the app). */
@Composable
fun AccountScreen(route: AccountRoute) {
    when (route) {
        AccountRoute.Settings -> SettingsScreen()

        AccountRoute.Privacy -> PrivacyCenterScreen()

        AccountRoute.PrivacyCheckup -> PrivacyCheckupScreen()

        AccountRoute.HiddenWords -> HiddenWordsScreen()

        AccountRoute.Devices -> DevicesScreen()

        AccountRoute.ScreenTime -> ScreenTimeScreen()

        AccountRoute.Welcome -> WelcomeScreen()

        AccountRoute.Safety -> SafetyScreen()

        is AccountRoute.Report -> ReportScreen(route.url)

        AccountRoute.Glossary -> GlossaryScreen()

        AccountRoute.Releases -> ReleasesScreen()

        is AccountRoute.Complaint -> ComplaintScreen(route.decisionId)

        // The remaining route: Gesperrt.
        else -> SuspendedScreen()
    }
}

/** „Neu in Mildcrowd …“ once after an update (placed by the app over the tabs). */
@Composable
fun UpdateNotesPrompt() {
    UpdateNotesDialog()
}

/** Sign-in, registration and password reset while nobody is signed in. */
@Composable
fun SignedOutScreen() {
    SignedOutFlow()
}

/**
 * The e-mail confirmation screen after registration. Its link to the settings (to correct the address or delete the
 * account) opens on a stack of its own, since the tabs are not there yet; the account pages pushed from there show on
 * it too. The stack is kept with the screen's view models, so a rotation does not close the open settings.
 */
@Composable
fun VerifyEmailScreen() {
    val app = LocalNavigator.current
    ScopedViewModels {
        val local = viewModel { LocalStack() }.navigator
        local.openWeb = { url -> app.openWeb(url) }
        val stack = local.stack(local.selectedTab)
        BackHandler(enabled = stack.isNotEmpty()) { local.back() }
        CompositionLocalProvider(LocalNavigator provides local) {
            val top = stack.lastOrNull()
            if (top is AccountRoute) {
                CompositionLocalProvider(LocalScreenChrome provides ScreenChrome(onBack = { local.back() })) {
                    AccountScreen(top)
                }
            } else {
                CompositionLocalProvider(LocalScreenChrome provides ScreenChrome()) {
                    VerifyEmailContentScreen(onSettings = { local.open(AccountRoute.Settings) })
                }
            }
        }
    }
}

/** The own stack of [VerifyEmailScreen], kept like a view model. */
internal class LocalStack : ViewModel() {
    val navigator = Navigator()
}

/** The mobile-number confirmation after the e-mail address (web: /app/handynummer); signing out is its only way out. */
@Composable
fun ConfirmPhoneScreen() {
    ScopedViewModels {
        CompositionLocalProvider(LocalScreenChrome provides ScreenChrome()) {
            ConfirmPhoneContentScreen()
        }
    }
}
