package de.velwebsolutions.mildcrowd.staff.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.CrowdCard
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.StaffRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.api.Suspension
import de.velwebsolutions.mildcrowd.staff.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffDate
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffLoadable
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffPage
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffTitle
import de.velwebsolutions.mildcrowd.staff.ui.components.boldLead
import de.velwebsolutions.mildcrowd.staff.ui.retry
import kotlinx.coroutines.launch

/**
 * Locked accounts (web: livewire.moderation.suspensions): every lock is a human decision with a planned end (or a ban);
 * accusation, excerpt, the member's statement, the strikes of the last months and "Freigeben" before the end (a
 * permanent ban only by admins).
 */
@Composable
fun SuspensionsScreen(viewModel: SuspensionsViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SuspensionsContent(state, viewModel.retry(viewModel::load), viewModel::release, viewModel::dismissMessage, viewModel::dismissError)
}

/** The locks page without its view model, for the app and for tests. */
@Composable
fun SuspensionsContent(
    state: SuspensionsUiState,
    onRefresh: suspend () -> Unit,
    onRelease: suspend (Suspension) -> Unit,
    onDismissMessage: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    var confirmRelease by remember { mutableStateOf<Suspension?>(null) }
    MildcrowdScreen(
        "Sperren",
        message = state.message,
        error = state.error,
        onDismissMessage = onDismissMessage,
        onDismissError = onDismissError
    ) {
        StaffLoadable(state.list, onRefresh) { list ->
            StaffPage(onRefresh, spacing = 14) {
                StaffTitle("Sperren")
                Text(
                    "Konten, die gerade gesperrt sind – immer durch eine menschliche Entscheidung, nie durch die automatische Prüfung. " +
                        "Eine Sperre mit Dauer endet von selbst; hier kannst du sie vorher aufheben. Nach ${list.strikesForBan} " +
                        "bestätigten Verstößen innerhalb von ${list.strikeMonths} Monaten wird ein Konto dauerhaft gesperrt; das hebt " +
                        "nur ein Admin auf.",
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Ink2
                )
                if (list.active.isEmpty()) {
                    Text(
                        "Gerade ist kein Konto gesperrt.",
                        style = MildcrowdType.Body,
                        color = MildcrowdColors.Ink2,
                        modifier = Modifier.fillMaxWidth().background(
                            MildcrowdColors.Paper,
                            RoundedCornerShape(MildcrowdRadius.S.dp)
                        ).padding(18.dp)
                    )
                }
                list.active.forEach { lock -> LockCard(lock, list.strikesForBan) { confirmRelease = lock } }
                if (list.ended.isNotEmpty()) {
                    Text(
                        "Zuletzt beendet",
                        style = MildcrowdType.heading(19),
                        color = MildcrowdColors.Night,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                    list.ended.forEach { lock ->
                        Text(
                            boldLead(
                                lock.user.displayName,
                                " · ${lock.label} · ${lock.resultLabel ?: "beendet"} · ${StaffDate.withUhr(lock.endedAt)}"
                            ),
                            style = MildcrowdType.Small,
                            color = MildcrowdColors.Ink,
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.CHIP.dp))
                                .padding(horizontal = 14.dp, vertical = 8.dp)
                        )
                    }
                }
            }
        }
    }
    confirmRelease?.let { lock ->
        ConfirmDialog(
            "Konto von ${lock.user.displayName} freigeben?",
            "Freigeben",
            { scope.launch { onRelease(lock) } },
            { confirmRelease = null },
            confirmTag = "suspension.release.confirm"
        )
    }
}

/** One active lock: kind, start, planned end ("bis …" for a lock with a duration) and the strikes of the last months. */
@Composable
private fun LockCard(lock: Suspension, limit: Int, onRelease: () -> Unit) {
    val navigator = LocalNavigator.current
    CrowdCard(padding = 16) {
        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text(
                buildAnnotatedString {
                    withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(lock.user.displayName) }
                    withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append(" @${lock.user.username}") }
                },
                style = MildcrowdType.Body,
                color = MildcrowdColors.Ink
            )
            Text(
                "${lock.label} · seit ${StaffDate.withUhr(lock.startedAt)}" +
                    (lock.endsAtLabel?.let { " · bis $it" } ?: "") +
                    " · Verstöße ${lock.user.activeStrikes ?: 0} von $limit",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Ink2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                lock.caseId?.let { caseId ->
                    MildcrowdButton("Zum Fall", {
                        navigator.open(StaffRoute.ModerationCase(caseId))
                    }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
                }
                if (lock.canRelease == true) {
                    MildcrowdButton("Freigeben", onRelease, Modifier.testTag("suspension.release.${lock.id}"), fullWidth = false)
                }
            }
            Text(boldLead("Vorwurf: ", lock.accusation.orEmpty()), style = MildcrowdType.Small, color = MildcrowdColors.Ink)
            lock.excerpt?.let { excerpt ->
                Row(Modifier.height(IntrinsicSize.Min)) {
                    Box(Modifier.width(4.dp).fillMaxHeight().background(MildcrowdColors.Sun))
                    Text(
                        "„$excerpt“",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Ink2,
                        modifier = Modifier.padding(start = 10.dp)
                    )
                }
            }
            Column(
                Modifier.fillMaxWidth().background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.XS.dp)).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text("Stellungnahme des Mitglieds", style = MildcrowdType.SmallBold, color = MildcrowdColors.Night)
                val statement = lock.statement
                if (statement != null) {
                    Text(statement, style = MildcrowdType.Body, color = MildcrowdColors.Ink)
                    Text(StaffDate.withUhr(lock.statementAt), style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
                } else {
                    Text("Noch keine.", style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
                }
            }
        }
    }
}
