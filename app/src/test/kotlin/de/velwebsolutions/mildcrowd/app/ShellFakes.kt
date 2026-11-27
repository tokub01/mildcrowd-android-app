package de.velwebsolutions.mildcrowd.app

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import de.velwebsolutions.mildcrowd.app.shell.AppScreens
import de.velwebsolutions.mildcrowd.app.shell.LiveShell
import de.velwebsolutions.mildcrowd.app.shell.PurchaseObserver
import de.velwebsolutions.mildcrowd.core.auth.AuthApi
import de.velwebsolutions.mildcrowd.core.auth.LoginCodeChannel
import de.velwebsolutions.mildcrowd.core.auth.PasskeyCeremony
import de.velwebsolutions.mildcrowd.core.auth.Registration
import de.velwebsolutions.mildcrowd.core.auth.RegistrationForm
import de.velwebsolutions.mildcrowd.core.auth.SignInMethods
import de.velwebsolutions.mildcrowd.core.auth.SignInResult
import de.velwebsolutions.mildcrowd.core.model.Me
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.serialization.json.JsonObject

val mira = Me(7, "mira", "Mira Sol", "MS", "mira@example.org")

/** The live module as the shell sees it, recording what the shell asks for. */
class FakeLive : LiveShell {
    override val hints = MutableStateFlow(0)
    override val chats = MutableStateFlow(0)
    val started = mutableListOf<Int>()
    var foregrounds = 0
    var showingChats: Boolean? = null
    var stops = 0

    override suspend fun start(userId: Int) {
        started += userId
    }

    override suspend fun foreground() {
        foregrounds++
    }

    override fun setShowingChats(showing: Boolean) {
        showingChats = showing
    }

    override fun stop() {
        stops++
    }
}

/** Counts how often the shell listens for purchases. */
class FakePurchases : PurchaseObserver {
    var observed = 0

    override suspend fun observe() {
        observed++
    }
}

/** Answers `me` with [me] (or fails with [error]). */
class FakeAuth(var me: Me = mira, var error: Exception? = null) : AuthApi {
    var loggedOut = false

    override suspend fun login(login: String, password: String, deviceName: String) = SignInResult.SignedIn("t", me())

    override suspend fun twoFactor(challenge: String, code: String?, recoveryCode: String?) = login("", "", "")

    override suspend fun registrationForm() = RegistrationForm("hp", "tf", "t")

    override suspend fun register(registration: Registration, form: RegistrationForm, deviceName: String) = login("", "", "")

    override suspend fun usernameAvailable(username: String) = de.velwebsolutions.mildcrowd.core.auth.UsernameAvailability(true)

    override suspend fun forgotPassword(email: String) = "ok"

    override suspend fun resetPassword(token: String, email: String, password: String, passwordConfirmation: String) = "ok"

    override suspend fun me(): Me = error?.let { throw it } ?: me

    override suspend fun logout() {
        loggedOut = true
    }

    override suspend fun resendVerification() = Unit

    override suspend fun methods() = SignInMethods()

    override suspend fun requestLoginCode(login: String, channel: LoginCodeChannel) = Unit

    override suspend fun verifyLoginCode(login: String, code: String, deviceName: String) = login("", "", "")

    override suspend fun passkeyOptions() = PasskeyCeremony("c", JsonObject(emptyMap()))

    override suspend fun passkeySignIn(challenge: String, credential: JsonObject, deviceName: String) = login("", "", "")
}

/** Stand-ins for the feature screens: each shows its name, so the shell's routing can be read off the screen. */
object FakeScreens : AppScreens {
    @Composable
    override fun SignedOut() = Text("Anmelden-Screen")

    @Composable
    override fun VerifyEmail() = Text("E-Mail-Screen")

    @Composable
    override fun ConfirmPhone() = Text("Handy-Screen")

    @Composable
    override fun AccountLinks() = Unit

    @Composable
    override fun Destination(route: AppRoute) = MildcrowdScreen(title = "Screen $route") { Text("Inhalt $route") }

    @Composable
    override fun Bell() = Text("Glocke")

    @Composable
    override fun ChatNotice() = Text("Chat-Hinweis")

    @Composable
    override fun QuietBanner() = Text("Ruhemodus-Banner")

    @Composable
    override fun UpdateNotes() = Text("Update-Hinweis")
}
