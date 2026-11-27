package de.velwebsolutions.mildcrowd.membership.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowForward
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.membership.api.MembershipFormat
import de.velwebsolutions.mildcrowd.membership.api.MembershipPayment

/**
 * /app/mitgliedschaft/portal (App\Livewire\Membership\Portal) in the app: the paid contributions of a web membership
 * with their invoices (view, print and "Teilen"). The payment method is changed on the web, not in the app.
 */
@Composable
fun PortalScreen(viewModel: PortalViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    PortalContent(state, viewModel.retry(viewModel::load), viewModel::openInvoice, viewModel::dismissMessage, viewModel::dismissError)
    state.invoiceFile?.let { InvoiceSheet(it, viewModel::closeInvoice) }
}

/** The invoices page without its view model, for the app and for tests. */
@Composable
fun PortalContent(
    state: PortalUiState,
    onRefresh: suspend () -> Unit,
    onOpenInvoice: (MembershipPayment) -> Unit,
    onDismissMessage: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val navigator = LocalNavigator.current
    val openWebsite = rememberOpenWebsite(navigator)
    MildcrowdScreen(
        "Rechnungen",
        message = state.message,
        error = state.error,
        onDismissMessage = onDismissMessage,
        onDismissError = onDismissError
    ) {
        MembershipPage(onRefresh) {
            PageHeader("Rechnungen", eyebrow = "Mitgliedschaft", lead = "Lade die Rechnungen zu deinen Beiträgen herunter.")
            SectionCard("Bezahlte Beiträge") {
                LoadableView(state.payments, onRetry = onRefresh) { payments ->
                    if (payments.isEmpty()) {
                        Text(
                            "Noch keine bezahlten Beiträge vorhanden.",
                            style = MildcrowdType.Small,
                            color = MildcrowdColors.Ink2,
                            modifier = Modifier.testTag("portal.empty")
                        )
                    } else {
                        Column {
                            payments.forEachIndexed { index, payment ->
                                PaymentRow(payment, loading = state.loadingInvoice == payment.id) { onOpenInvoice(payment) }
                                if (index != payments.lastIndex) HorizontalDivider(color = MildcrowdColors.PaperLine)
                            }
                        }
                    }
                }
            }
            InlineLink("Zur Mitgliedschaft", { backToOverview(navigator) }, color = MildcrowdColors.Night)
            SideCard("Fragen zur Rechnung?", tone = PanelTone.Dune) {
                Text("Jede Rechnung kannst du einzeln herunterladen. Bei Fragen hilft dir unsere Hilfe-Seite weiter.")
                InlineLink("Hilfe & Kontakt", { openWebsite("/hilfe") }, color = MildcrowdColors.Night)
            }
        }
    }
}

@Composable
private fun PaymentRow(payment: MembershipPayment, loading: Boolean, onOpen: () -> Unit) {
    Row(
        Modifier.padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(MembershipFormat.day(payment.paidAt), style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
            Text(
                payment.amountLabel + if (payment.refunded) " · erstattet" else "",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Ink2
            )
        }
        Row(
            Modifier.defaultMinSize(minHeight = 44.dp).pressable(onOpen, enabled = !loading).testTag("portal.invoice.${payment.id}"),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(Modifier.size(18.dp), color = MildcrowdColors.Night, strokeWidth = 2.dp)
            } else {
                Text(
                    "Rechnung ${payment.invoiceNumber}",
                    style = MildcrowdType.SmallBold.copy(textDecoration = TextDecoration.Underline),
                    color = MildcrowdColors.Night
                )
                Icon(
                    Icons.AutoMirrored.Outlined.ArrowForward,
                    contentDescription = null,
                    tint = MildcrowdColors.Night,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

/** /app/mitgliedschaft/rechnungen/{id} opened from a link: downloads the invoice and shows it with "Teilen". */
@Composable
fun InvoiceScreen(
    paymentId: Int,
    viewModel: InvoiceViewModel = hiltViewModel<InvoiceViewModel, InvoiceViewModel.Factory>(
        key = "invoice-$paymentId",
        creationCallback = { it.create(paymentId) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    InvoiceContent(paymentId, state, viewModel.retry(viewModel::load), viewModel::showPreview, viewModel::closePreview)
}

/** The invoice page: loading, the server's error, or the invoice with "Rechnung ansehen" and "Teilen". */
@Composable
fun InvoiceContent(paymentId: Int, state: InvoiceUiState, onRetry: suspend () -> Unit, onShow: () -> Unit, onClose: () -> Unit) {
    val context = LocalContext.current
    MildcrowdScreen("Rechnung") {
        LoadableView(state.file, onRetry = onRetry) { file ->
            Column(
                Modifier.fillMaxSize().padding(32.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Icon(Icons.Outlined.Description, contentDescription = null, tint = MildcrowdColors.Spring, modifier = Modifier.size(40.dp))
                Text("Rechnung MC-$paymentId", style = MildcrowdType.heading(), color = MildcrowdColors.Night)
                MildcrowdButton("Rechnung ansehen", onShow, Modifier.testTag("invoice.view"), fullWidth = false)
                MildcrowdButton(
                    "Teilen",
                    { FileSharing.shareFile(context, file, FileSharing.mimeType(file), "Teilen") },
                    Modifier.testTag("invoice.pageShare"),
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false
                )
            }
        }
    }
    state.preview?.let { InvoiceSheet(it, onClose) }
}
