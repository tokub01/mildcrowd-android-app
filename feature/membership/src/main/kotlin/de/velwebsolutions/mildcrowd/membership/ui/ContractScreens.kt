package de.velwebsolutions.mildcrowd.membership.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.BannerKind
import de.velwebsolutions.mildcrowd.design.components.CrowdCard
import de.velwebsolutions.mildcrowd.design.components.ErrorStateView
import de.velwebsolutions.mildcrowd.design.components.LoadingView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.membership.api.CancellationDetails
import de.velwebsolutions.mildcrowd.membership.api.MembershipFormat
import de.velwebsolutions.mildcrowd.membership.api.WithdrawalDetails
import java.io.File
import kotlinx.coroutines.launch

/** /app/mitgliedschaft/kuendigen (App\Livewire\Membership\Cancel): § 312k BGB confirmation with "Jetzt kündigen". */
@Composable
fun CancelScreen(viewModel: CancelViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    CancelContent(state, viewModel.retry(viewModel::load), viewModel::confirm, viewModel::prepareExport, viewModel::dismissError)
}

/** The cancel page without its view model, for the app and for tests. */
@Composable
fun CancelContent(
    state: ContractUiState<CancellationDetails>,
    onRetry: suspend () -> Unit,
    onConfirm: suspend () -> Unit,
    onExport: suspend () -> Unit,
    onDismissError: () -> Unit = {}
) {
    val navigator = LocalNavigator.current
    val openWebsite = rememberOpenWebsite(navigator)
    MildcrowdScreen("Mitgliedschaft kündigen", error = state.error, onDismissError = onDismissError) {
        MembershipPage {
            PageHeader("Mitgliedschaft kündigen", eyebrow = "Mitgliedschaft", lead = "Bestätigung deiner Kündigung gemäß § 312k BGB.")
            ContractState(state.details, onRetry) { details ->
                val date = details.effectiveDate?.let(MembershipFormat::day) ?: "Periodenende"
                CrowdCard(padding = 20) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ContractDetails(
                            listOf(
                                "Vertrag:" to "Mildcrowd Mitgliedschaft (Stufe: ${details.tierName})",
                                "Name:" to details.name,
                                "E-Mail:" to details.email,
                                "Art der Kündigung:" to "Ordentlich zum Ende der Abrechnungsperiode",
                                "Wirksam zum:" to date
                            )
                        )
                        Text("Was passiert nach der Kündigung?", style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
                        RichBullets(
                            listOf(
                                "Deine Mitgliedschaft bleibt bis zum $date vollständig aktiv.",
                                "Danach endet dein Zugang: Du kannst **nichts mehr lesen oder schreiben**.",
                                "Du hast anschließend **30 Tage Zeit**, um deine Daten herunterzuladen oder dein Konto zu löschen. Danach löschen wir dein Konto.",
                                "Wir senden dir sofort eine Eingangsbestätigung in Textform per E-Mail – mit Inhalt, Datum, Uhrzeit und dem Zeitpunkt, zu dem dein Vertrag endet."
                            )
                        )
                        ContractActions("Jetzt kündigen", "cancel.confirm", onConfirm)
                    }
                }
            }
            SideCard("Deine Daten mitnehmen", tone = PanelTone.Dune) {
                Text("Lade dir jederzeit eine Kopie deiner Daten herunter – vor oder nach der Kündigung.")
                DataExportLink(state.exportFile, onExport)
            }
            SideCard("Auch ohne Anmeldung", tone = PanelTone.Night) {
                Text("Kündigen kannst du ebenso über das öffentliche Formular „Verträge hier kündigen“.")
                InlineLink("Verträge hier kündigen", { openWebsite("/vertraege-kuendigen") }, color = MildcrowdColors.OnNight)
            }
        }
    }
}

/** /app/mitgliedschaft/widerrufen (App\Livewire\Membership\Withdraw): withdrawal within 14 days with "Jetzt widerrufen". */
@Composable
fun WithdrawScreen(viewModel: WithdrawViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    WithdrawContent(state, viewModel.retry(viewModel::load), viewModel::confirm, viewModel::prepareExport, viewModel::dismissError)
}

/** The withdraw page without its view model, for the app and for tests. */
@Composable
fun WithdrawContent(
    state: ContractUiState<WithdrawalDetails>,
    onRetry: suspend () -> Unit,
    onConfirm: suspend () -> Unit,
    onExport: suspend () -> Unit,
    onDismissError: () -> Unit = {}
) {
    val navigator = LocalNavigator.current
    val openWebsite = rememberOpenWebsite(navigator)
    MildcrowdScreen("Mitgliedschaft widerrufen", error = state.error, onDismissError = onDismissError) {
        MembershipPage {
            PageHeader(
                "Mitgliedschaft widerrufen",
                eyebrow = "Mitgliedschaft",
                lead = "Gesetzliches Widerrufsrecht innerhalb von 14 Tagen (§ 355 BGB)."
            )
            ContractState(state.details, onRetry) { details ->
                CrowdCard(padding = 20) {
                    Column(verticalArrangement = Arrangement.spacedBy(18.dp)) {
                        ContractDetails(
                            listOf(
                                "Vertrag:" to "Mildcrowd Mitgliedschaft (Stufe: ${details.tierName})",
                                "Name:" to details.name,
                                "E-Mail:" to details.email,
                                "Rechtsgrundlage:" to "14-tägiges gesetzliches Widerrufsrecht"
                            )
                        )
                        Text("Folgen des Widerrufs:", style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
                        RichBullets(
                            listOf(
                                "Deine Mitgliedschaft wird **mit sofortiger Wirkung** beendet.",
                                "Bereits gezahlte Beiträge erstatten wir **vollständig** an dein ursprüngliches Zahlungsmittel; einen Betrag für die Zeit bis zum Widerruf verlangen wir nicht.",
                                "Dein Zugang endet sofort. 30 Tage lang kannst du noch deine Daten herunterladen oder dein Konto löschen, danach löschen wir es."
                            )
                        )
                        ContractActions("Jetzt widerrufen", "withdraw.confirm", onConfirm)
                    }
                }
            }
            SideCard("Widerrufsbelehrung", tone = PanelTone.Dune) {
                Text("Alle Einzelheiten zu Frist, Folgen und Erstattung stehen in der Widerrufsbelehrung.")
                InlineLink("Widerrufsbelehrung lesen", { openWebsite("/widerrufsbelehrung") }, color = MildcrowdColors.Night)
            }
            SideCard("Deine Daten mitnehmen", tone = PanelTone.Night) {
                Text("Nach dem Widerruf hast du 30 Tage Zeit, deine Daten herunterzuladen. Das geht auch jetzt schon.")
                DataExportLink(state.exportFile, onExport, color = MildcrowdColors.OnNight)
            }
        }
    }
}

/** The page's contract: loading, the confirmation, or – when the page is not reachable – the server's reason. */
@Composable
private fun <D> ContractState(state: Loadable<D>, onRetry: suspend () -> Unit, content: @Composable (D) -> Unit) {
    when (state) {
        is Loadable.Failed -> NotAvailable(state.error, onRetry)
        is Loadable.Loaded -> content(state.value)
        else -> LoadingView()
    }
}

/** The confirming button and the way back; afterwards the overview shows the outcome. */
@Composable
private fun ContractActions(confirmTitle: String, tag: String, onConfirm: suspend () -> Unit) {
    val navigator = LocalNavigator.current
    AsyncMildcrowdButton(
        confirmTitle,
        onClick = {
            onConfirm()
            backToOverview(navigator)
        },
        modifier = Modifier.testTag(tag)
    )
    Box(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp).pressable({
            backToOverview(navigator)
        }),
        contentAlignment = Alignment.Center
    ) {
        Text(
            "Abbrechen und zurück zur Mitgliedschaft",
            style = MildcrowdType.SmallBold.copy(textDecoration = TextDecoration.Underline),
            color = MildcrowdColors.Muted
        )
    }
}

/** "Daten herunterladen" like the web's direct download: the ZIP is fetched, then handed to the share sheet. */
@Composable
private fun DataExportLink(file: File?, onExport: suspend () -> Unit, color: androidx.compose.ui.graphics.Color = MildcrowdColors.Night) {
    val context = LocalContext.current
    if (file != null) {
        Row(
            Modifier
                .defaultMinSize(minHeight = 44.dp)
                .pressable({ FileSharing.shareFile(context, file, FileSharing.mimeType(file), "ZIP-Datei sichern oder teilen") })
                .testTag("contract.exportShare"),
            horizontalArrangement = Arrangement.spacedBy(6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Outlined.Share, contentDescription = null, tint = color, modifier = Modifier.size(18.dp))
            Text("ZIP-Datei sichern oder teilen", style = MildcrowdType.SmallBold, color = color)
        }
    } else {
        val scope = rememberCoroutineScope()
        var running by remember { mutableStateOf(false) }
        Row(
            Modifier
                .defaultMinSize(minHeight = 44.dp)
                .pressable({
                    if (!running) {
                        running = true
                        scope.launch {
                            try {
                                onExport()
                            } finally {
                                running = false
                            }
                        }
                    }
                })
                .testTag("contract.export"),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Daten herunterladen", style = MildcrowdType.SmallBold.copy(textDecoration = TextDecoration.Underline), color = color)
            if (running) CircularProgressIndicator(Modifier.size(16.dp), color = color, strokeWidth = 2.dp)
        }
    }
}

/**
 * The web redirects to the overview with a flash message when the page is not reachable (no contract, period over);
 * the app shows that message with the way back.
 */
@Composable
private fun NotAvailable(error: ApiError, onRetry: suspend () -> Unit) {
    val navigator = LocalNavigator.current
    if (error is ApiError.Conflict) {
        CrowdCard(padding = 20) {
            Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Banner(error.userMessage, Modifier.testTag("membership.notAvailable"), kind = BannerKind.Info)
                MildcrowdButton("Zur Mitgliedschaft", { backToOverview(navigator) }, fullWidth = false)
            }
        }
    } else {
        ErrorStateView(error, onRetry)
    }
}
