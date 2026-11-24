package de.velwebsolutions.mildcrowd.account.ui.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.account.platform.rememberPasskeyAuthenticator
import de.velwebsolutions.mildcrowd.account.ui.components.AuthCard
import de.velwebsolutions.mildcrowd.account.ui.components.LinkText
import de.velwebsolutions.mildcrowd.core.auth.LoginCodeChannel
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.BannerKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdKeyboard
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import kotlinx.coroutines.launch

/**
 * Sign-in with e-mail or username and password, like /anmelden; without a password by code or passkey. Which of those
 * the server offers is asked each time the form shows (also back from another step or another app) until it answered.
 */
@Composable
internal fun SignInScreen(
    onTwoFactor: (String) -> Unit,
    onForgotPassword: () -> Unit,
    onRegister: () -> Unit,
    onLoginCode: (LoginCodeChannel, String) -> Unit,
    onLegal: (String) -> Unit
) {
    val viewModel: SignInViewModel = hiltViewModel()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val passkeys = rememberPasskeyAuthenticator()
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { viewModel.loadMethodsIfMissing() } }
    MildcrowdScreen(title = null, error = ui.notice, onDismissError = viewModel::dismissNotice) {
        SignInContent(
            ui = ui,
            actions = SignInActions(
                onLogin = viewModel::setLogin,
                onPassword = viewModel::setPassword,
                onSignIn = { viewModel.signIn()?.let(onTwoFactor) },
                onForgotPassword = onForgotPassword,
                onRegister = onRegister,
                onLoginCode = { channel -> onLoginCode(channel, ui.login) },
                onPasskey = if (ui.offersPasskeys && passkeys.isAvailable) ({ viewModel.signInWithPasskey(passkeys) }) else null,
                onLegal = onLegal
            )
        )
    }
}

/**
 * What the sign-in form can do; [onPasskey] is null while passkeys are not on offer. [onLegal] opens a page of the website
 * (`impressum`, `datenschutz`): § 5 DDG wants both reachable before anyone signs in.
 */
internal class SignInActions(
    val onLogin: (String) -> Unit,
    val onPassword: (String) -> Unit,
    val onSignIn: suspend () -> Unit,
    val onForgotPassword: () -> Unit,
    val onRegister: () -> Unit,
    val onLoginCode: (LoginCodeChannel) -> Unit,
    val onPasskey: (suspend () -> Unit)?,
    val onLegal: (String) -> Unit = {}
)

@Composable
internal fun SignInContent(ui: SignInUiState, actions: SignInActions) {
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val error = ui.error
    AuthCard("Willkommen zurück.", "Schön, dass du wieder da bist.") {
        if (error != null && error.message("login") == null && error.message("password") == null) Banner(error.userMessage)
        MildcrowdTextField(
            "E-Mail oder Benutzername",
            ui.login,
            actions.onLogin,
            modifier = Modifier.testTag("login.field"),
            error = error?.message("login"),
            keyboardOptions = MildcrowdKeyboard.Username.copy(imeAction = ImeAction.Next),
            keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
        )
        MildcrowdTextField(
            "Passwort",
            ui.password,
            actions.onPassword,
            modifier = Modifier.testTag("password.field"),
            error = error?.message("password"),
            isSecure = true,
            keyboardOptions = MildcrowdKeyboard.Password.copy(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { scope.launch { actions.onSignIn() } })
        )
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.CheckCircle, contentDescription = null, tint = MildcrowdColors.Muted, modifier = Modifier.size(16.dp))
            Text(
                "Du bleibst auf diesem Gerät angemeldet, bis du dich abmeldest oder die App 90 Tage lang nicht nutzt.",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Muted
            )
        }
        AsyncMildcrowdButton("Anmelden", actions.onSignIn, modifier = Modifier.testTag("signin.button"), enabled = ui.canSubmit)
        if (ui.passwordRefused) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.S.dp))
                    .padding(horizontal = 16.dp, vertical = 8.dp)
                    .testTag("signin.codeHint")
            ) {
                Text("Passwort vergessen?", style = MildcrowdType.Body, color = MildcrowdColors.Ink)
                LinkText("Melde dich mit einem Code per E-Mail an.", { actions.onLoginCode(LoginCodeChannel.Email) })
            }
        }
        LinkText("Passwort vergessen?", actions.onForgotPassword, Modifier.testTag("forgot.button"))
        PasswordlessWays(ui, actions)
        HorizontalDivider(color = MildcrowdColors.PaperLine)
        Text("Noch kein Konto?", style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
        MildcrowdButton(
            "Mitglied werden",
            actions.onRegister,
            modifier = Modifier.testTag("register.button"),
            kind = MildcrowdButtonKind.Accent
        )
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally)) {
            LinkText("Impressum", { actions.onLegal("impressum") }, Modifier.testTag("signin.impressum"), bold = false)
            LinkText("Datenschutz", { actions.onLegal("datenschutz") }, Modifier.testTag("signin.datenschutz"), bold = false)
        }
    }
}

/** „Mit Passkey anmelden“, „Ohne Passwort: Code per E-Mail“ and – while SMS is set up – „Code per SMS“. */
@Composable
private fun PasswordlessWays(ui: SignInUiState, actions: SignInActions) {
    HorizontalDivider(color = MildcrowdColors.PaperLine)
    actions.onPasskey?.let { passkey ->
        AsyncMildcrowdButton(
            "Mit Passkey anmelden",
            passkey,
            modifier = Modifier.testTag("signin.passkey"),
            kind = MildcrowdButtonKind.Secondary
        )
    }
    Column {
        LinkText("Ohne Passwort: Code per E-Mail", { actions.onLoginCode(LoginCodeChannel.Email) }, Modifier.testTag("signin.codeEmail"))
        if (ui.offersSms) LinkText("Code per SMS", { actions.onLoginCode(LoginCodeChannel.Sms) }, Modifier.testTag("signin.codeSms"))
    }
}

/** Second factor (/zwei-faktor-anmeldung): the six-digit code from the authenticator app, or one recovery code. */
@Composable
internal fun TwoFactorScreen(challenge: String, onLost: () -> Unit) {
    val viewModel: TwoFactorViewModel = hiltViewModel(key = "two-factor:$challenge")
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    MildcrowdScreen(title = "Anmelden") {
        TwoFactorContent(ui, viewModel::setCode, viewModel::setRecoveryCode, { viewModel.submit(challenge) }, onLost)
    }
}

@Composable
internal fun TwoFactorContent(
    ui: TwoFactorUiState,
    onCode: (String) -> Unit,
    onRecoveryCode: (String) -> Unit,
    onSubmit: suspend () -> Unit,
    onLost: () -> Unit
) {
    val scope = rememberCoroutineScope()
    val error = ui.error
    AuthCard("Noch ein Schritt", "Gib den Code aus deiner Authenticator-App ein.") {
        val challengeError = error?.message("challenge")
        if (challengeError != null) {
            Banner(challengeError)
        } else if (error != null && error.message("code") == null && error.message("recovery_code") == null) {
            Banner(error.userMessage)
        }
        MildcrowdTextField(
            "Sechsstelliger Code",
            ui.code,
            onCode,
            modifier = Modifier.testTag("twofactor.code"),
            error = error?.message("code"),
            keyboardOptions = MildcrowdKeyboard.OneTimeCode.copy(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { scope.launch { onSubmit() } })
        )
        MildcrowdTextField(
            "Oder ein Wiederherstellungscode (optional)",
            ui.recoveryCode,
            onRecoveryCode,
            modifier = Modifier.testTag("twofactor.recovery"),
            error = error?.message("recovery_code"),
            keyboardOptions = MildcrowdKeyboard.NoCapitalization.copy(imeAction = ImeAction.Go),
            keyboardActions = KeyboardActions(onGo = { scope.launch { onSubmit() } })
        )
        AsyncMildcrowdButton("Anmelden", onSubmit, modifier = Modifier.testTag("twofactor.submit"), enabled = ui.canSubmit)
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LinkText("Kein Zugriff auf deine Codes?", onLost, Modifier.testTag("twofactor.lost"))
        }
    }
}

/** „Zwei-Faktor verloren“ (/zwei-faktor-hilfe), step 1 in the app: e-mail and password. */
@Composable
internal fun TwoFactorRecoveryScreen() {
    val viewModel: TwoFactorRecoveryViewModel = hiltViewModel()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val focus = LocalFocusManager.current
    val scope = rememberCoroutineScope()
    val error = ui.error
    MildcrowdScreen(title = "Zwei-Faktor-Hilfe") {
        AuthCard(
            "Kein Zugriff auf deine Codes?",
            "Du hast dein Handy und deine Wiederherstellungscodes verloren? So bekommst du dein Konto zurück."
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "1. Gib E-Mail-Adresse und Passwort deines Kontos ein.",
                    "2. Wir schicken dir einen Link. Öffne ihn, um zu bestätigen, dass dir die Adresse gehört.",
                    "3. Aus Sicherheitsgründen warten wir danach 72 Stunden. In dieser Zeit kannst du alles abbrechen.",
                    "4. Danach schaltest du die Zwei-Faktor-Anmeldung über den Link ab und richtest sie neu ein."
                ).forEach { Text(it, style = MildcrowdType.Small, color = MildcrowdColors.Ink2) }
            }
            ui.status?.let { Banner(it, kind = BannerKind.Success) }
            val formError = error?.message("form")
            if (formError != null) {
                Banner(formError)
            } else if (error != null && error.message("email") == null && error.message("password") == null) {
                Banner(error.userMessage)
            }
            MildcrowdTextField(
                "E-Mail-Adresse",
                ui.email,
                viewModel::setEmail,
                modifier = Modifier.testTag("recovery.email"),
                error = error?.message("email"),
                keyboardOptions = MildcrowdKeyboard.Email.copy(imeAction = ImeAction.Next),
                keyboardActions = KeyboardActions(onNext = { focus.moveFocus(FocusDirection.Down) })
            )
            MildcrowdTextField(
                "Passwort",
                ui.password,
                viewModel::setPassword,
                modifier = Modifier.testTag("recovery.password"),
                error = error?.message("password"),
                isSecure = true,
                keyboardOptions = MildcrowdKeyboard.Password.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { scope.launch { viewModel.submit() } })
            )
            AsyncMildcrowdButton(
                "Link anfordern",
                viewModel::submit,
                modifier = Modifier.testTag("recovery.submit"),
                enabled = ui.canSubmit
            )
        }
    }
}

/** „Passwort vergessen?“ – always the same neutral answer, like the web. */
@Composable
internal fun ForgotPasswordScreen(onBackToSignIn: () -> Unit) {
    val viewModel: ForgotPasswordViewModel = hiltViewModel()
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val error = ui.error
    MildcrowdScreen(title = "Passwort") {
        AuthCard("Passwort vergessen?", "Wir schicken dir einen Link, mit dem du ein neues Passwort setzt.") {
            ui.status?.let { Banner(it, kind = BannerKind.Success) }
            if (error != null && error.message("email") == null) Banner(error.userMessage)
            MildcrowdTextField(
                "E-Mail-Adresse deines Kontos",
                ui.email,
                viewModel::setEmail,
                modifier = Modifier.testTag("forgot.email"),
                error = error?.message("email"),
                keyboardOptions = MildcrowdKeyboard.Email.copy(imeAction = ImeAction.Send),
                keyboardActions = KeyboardActions(onSend = { scope.launch { viewModel.send() } })
            )
            AsyncMildcrowdButton(
                "Link schicken",
                viewModel::send,
                modifier = Modifier.testTag("forgot.submit"),
                enabled = ui.email.isNotEmpty()
            )
            Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
                LinkText("Zurück zur Anmeldung", onBackToSignIn)
            }
        }
    }
}
