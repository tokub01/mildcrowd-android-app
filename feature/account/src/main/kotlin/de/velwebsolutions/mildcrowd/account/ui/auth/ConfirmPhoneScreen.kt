package de.velwebsolutions.mildcrowd.account.ui.auth

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.input.KeyboardType
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.account.api.PhoneNumberStatus
import de.velwebsolutions.mildcrowd.account.platform.SmsCodeConsent
import de.velwebsolutions.mildcrowd.account.ui.components.AuthCard
import de.velwebsolutions.mildcrowd.account.ui.components.LinkText
import de.velwebsolutions.mildcrowd.account.ui.components.OneTimeCodeField
import de.velwebsolutions.mildcrowd.account.ui.retry
import de.velwebsolutions.mildcrowd.account.ui.settings.PhoneNumberUiState
import de.velwebsolutions.mildcrowd.account.ui.settings.PhoneNumberViewModel
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.BannerKind
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import kotlinx.coroutines.launch

/** „Handynummer bestätigen“ (/app/handynummer): the step between the e-mail confirmation and "Mitglied werden". */
@Composable
internal fun ConfirmPhoneContentScreen(viewModel: PhoneNumberViewModel = hiltViewModel()) {
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    MildcrowdScreen(title = "Handynummer bestätigen") {
        LoadableView(ui.state, onRetry = viewModel.retry(viewModel::load)) { status ->
            ConfirmPhoneContent(
                ui,
                status,
                ConfirmPhoneActions(
                    onNumber = viewModel::setNumber,
                    onCode = viewModel::setCode,
                    onSend = viewModel::sendCode,
                    onConfirm = viewModel::confirm,
                    onSignOut = viewModel::signOut
                )
            )
        }
    }
}

/** What the phone step can do; the screen connects it to the view model. */
internal class ConfirmPhoneActions(
    val onNumber: (String) -> Unit,
    val onCode: (String) -> Unit,
    val onSend: suspend () -> Unit,
    val onConfirm: suspend () -> Unit,
    val onSignOut: suspend () -> Unit
)

/**
 * The web's ConfirmPhone: a number, then the six digits of its SMS. While a code waits, the number field stays for
 * another number or a new code, with an outlined button.
 */
@Composable
internal fun ConfirmPhoneContent(ui: PhoneNumberUiState, status: PhoneNumberStatus, actions: ConfirmPhoneActions) {
    val scope = rememberCoroutineScope()
    val pending = status.pending
    AuthCard("Noch ein Schritt.", "Bitte bestätige deine Handynummer.") {
        Text(
            "Jedes Mildcrowd-Konto gehört zu einer Handynummer. So bleibt Mildcrowd frei von Zweitkonten und Bots. Wir schicken dir " +
                "einen Code per SMS. Deine Nummer zeigen wir keinem anderen Mitglied; unser SMS-Dienstleister erhält sie nur für den " +
                "Code (Datenschutzerklärung).",
            style = MildcrowdType.Body,
            color = MildcrowdColors.Ink2
        )
        ui.message?.let { Banner(it, kind = BannerKind.Success) }
        ui.error?.let { Banner(it) }
        if (pending != null) {
            OneTimeCodeField(
                "Code aus der SMS an $pending",
                ui.code,
                actions.onCode,
                onDone = { scope.launch { actions.onConfirm() } },
                modifier = Modifier.testTag("confirmphone.code"),
                error = ui.fieldErrors["code"]
            )
            SmsCodeConsent(active = true, onCode = actions.onCode)
            AsyncMildcrowdButton(
                "Bestätigen",
                actions.onConfirm,
                modifier = Modifier.testTag("confirmphone.confirm"),
                enabled = ui.code.isNotBlank()
            )
        }
        if (status.smsAvailable) {
            MildcrowdTextField(
                if (pending != null) "Andere Nummer oder neuer Code" else "Handynummer",
                ui.number,
                actions.onNumber,
                modifier = Modifier.testTag("confirmphone.number"),
                error = ui.fieldErrors["phone_number"],
                hint = "Zum Beispiel 0176 12345678.",
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Phone)
            )
            AsyncMildcrowdButton(
                "Code per SMS schicken",
                actions.onSend,
                modifier = Modifier.testTag("confirmphone.send"),
                kind = if (pending != null) MildcrowdButtonKind.Secondary else MildcrowdButtonKind.Primary,
                enabled = ui.number.isNotBlank()
            )
        } else {
            Banner("SMS sind gerade nicht verfügbar. Bitte versuche es später noch einmal.", kind = BannerKind.Info)
        }
        Column(Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
            LinkText("Abmelden", { scope.launch { actions.onSignOut() } }, Modifier.testTag("confirmphone.signout"), bold = false)
        }
    }
}
