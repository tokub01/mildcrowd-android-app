package de.velwebsolutions.mildcrowd.app.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import de.velwebsolutions.mildcrowd.account.ConfirmPhoneScreen
import de.velwebsolutions.mildcrowd.account.SignedOutScreen
import de.velwebsolutions.mildcrowd.account.UpdateNotesPrompt
import de.velwebsolutions.mildcrowd.account.VerifyEmailScreen
import de.velwebsolutions.mildcrowd.app.ui.AppDestination
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import de.velwebsolutions.mildcrowd.live.ChatToastOverlay
import de.velwebsolutions.mildcrowd.live.HintsButton
import de.velwebsolutions.mildcrowd.live.QuietModeBanner

/**
 * The feature screens the shell places: the real ones in the app, stand-ins in the shell's UI tests (the features have
 * their own tests).
 */
interface AppScreens {
    @Composable
    fun SignedOut()

    @Composable
    fun VerifyEmail()

    @Composable
    fun ConfirmPhone()

    /** Takes the account links from mails (confirmation, reset) while somebody is signed in. */
    @Composable
    fun AccountLinks()

    @Composable
    fun Destination(route: AppRoute)

    @Composable
    fun Bell()

    @Composable
    fun ChatNotice()

    /** The calm banner of the Ruhemodus above every tab's screens. */
    @Composable
    fun QuietBanner()

    /** „Neu in Mildcrowd …“ once after an update. */
    @Composable
    fun UpdateNotes()
}

/** The feature modules' screens. */
object FeatureScreens : AppScreens {
    @Composable
    override fun SignedOut() = SignedOutScreen()

    @Composable
    override fun VerifyEmail() = VerifyEmailScreen()

    @Composable
    override fun ConfirmPhone() = ConfirmPhoneScreen()

    @Composable
    override fun AccountLinks() = de.velwebsolutions.mildcrowd.account.AccountLinks()

    @Composable
    override fun Destination(route: AppRoute) = AppDestination(route)

    @Composable
    override fun Bell() = HintsButton()

    @Composable
    override fun ChatNotice() = ChatToastOverlay()

    @Composable
    override fun QuietBanner() = QuietModeBanner()

    @Composable
    override fun UpdateNotes() = UpdateNotesPrompt()
}

/** The screens of the running app. */
val LocalAppScreens = staticCompositionLocalOf<AppScreens> { FeatureScreens }
