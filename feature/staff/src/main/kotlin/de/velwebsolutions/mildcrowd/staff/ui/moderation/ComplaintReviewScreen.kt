package de.velwebsolutions.mildcrowd.staff.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.StaffRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.api.ComplaintDetail
import de.velwebsolutions.mildcrowd.staff.api.Labelled
import de.velwebsolutions.mildcrowd.staff.api.StatementOfReasons
import de.velwebsolutions.mildcrowd.staff.api.durationLabel
import de.velwebsolutions.mildcrowd.staff.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.staff.ui.components.RadioList
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffDate
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffLoadable
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffPage
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffTitle
import de.velwebsolutions.mildcrowd.staff.ui.components.boldLead
import de.velwebsolutions.mildcrowd.staff.ui.retry
import kotlinx.coroutines.launch

/**
 * Review of a complaint (web: livewire.moderation.complaint-review). A different person than the original decider
 * decides it (DSA Art. 20, AGB § 12).
 */
@Composable
fun ComplaintReviewScreen(
    complaintId: Int,
    viewModel: ComplaintReviewViewModel = hiltViewModel<ComplaintReviewViewModel, ComplaintReviewViewModel.Factory>(
        key = "complaint-$complaintId",
        creationCallback = { it.create(complaintId) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ComplaintReviewContent(
        state,
        onRefresh = viewModel.retry(viewModel::load),
        onResult = viewModel::selectResult,
        onExplanation = viewModel::updateExplanation,
        onStatement = viewModel::updateStatement,
        onReview = viewModel::review,
        onDismissMessage = viewModel::dismissMessage,
        onDismissError = viewModel::dismissError
    )
}

/** The complaint page without its view model, for the app and for tests. */
@Composable
fun ComplaintReviewContent(
    state: ComplaintReviewUiState,
    onRefresh: suspend () -> Unit,
    onResult: (String) -> Unit,
    onExplanation: (String) -> Unit,
    onStatement: (StatementOfReasons) -> Unit,
    onReview: suspend () -> Unit,
    onDismissMessage: () -> Unit = {},
    onDismissError: () -> Unit = {}
) {
    val navigator = LocalNavigator.current
    val scope = rememberCoroutineScope()
    var confirmReview by remember { mutableStateOf<ComplaintDetail?>(null) }
    MildcrowdScreen(
        "Widerspruch",
        message = state.message,
        error = state.error,
        onDismissMessage = onDismissMessage,
        onDismissError = onDismissError
    ) {
        StaffLoadable(state.complaint, onRefresh) { complaint ->
            StaffPage(onRefresh) {
                Text(
                    "← Fall #${complaint.caseId}",
                    style = MildcrowdType.Small.copy(textDecoration = TextDecoration.Underline),
                    color = MildcrowdColors.Night,
                    modifier = Modifier.pressable({ navigator.open(StaffRoute.ModerationCase(complaint.caseId)) })
                )
                StaffTitle("Widerspruch")
                Text(
                    "${complaint.status.label} · eingegangen am ${StaffDate.short(complaint.submittedAt)}",
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Ink2
                )
                SectionCard("Ursprüngliche Entscheidung") {
                    val duration = complaint.decision.suspensionDays?.let { " (${durationLabel(it)})" }.orEmpty()
                    Text(
                        "${complaint.decision.outcome.label}$duration · ${complaint.decision.ground.label}",
                        style = MildcrowdType.BodyBold,
                        color = MildcrowdColors.Ink
                    )
                    Text(
                        "Entschieden von ${complaint.decision.deciderName ?: "Gelöschtes Konto"} am ${StaffDate.short(
                            complaint.decision.decidedAt
                        )}",
                        style = MildcrowdType.Caption,
                        color = MildcrowdColors.Ink2
                    )
                    Text(boldLead("Regel: ", complaint.decision.ruleReference), style = MildcrowdType.Small, color = MildcrowdColors.Ink)
                    Text(boldLead("Begründung: ", complaint.decision.explanation), style = MildcrowdType.Small, color = MildcrowdColors.Ink)
                }
                SectionCard("Begründung des Widerspruchs") {
                    SelectionContainer { Text(complaint.reason, style = MildcrowdType.Body, color = MildcrowdColors.Ink) }
                }
                val result = complaint.result
                when {
                    result != null -> SectionCard("Ergebnis") {
                        Text(
                            "${complaint.status.label} · geprüft von ${result.reviewerName ?: "Gelöschtes Konto"} am ${StaffDate.short(
                                result.reviewedAt
                            )}",
                            style = MildcrowdType.Small,
                            color = MildcrowdColors.Ink,
                            modifier = Modifier.testTag("complaint.result")
                        )
                        Text(result.explanation.orEmpty(), style = MildcrowdType.Body, color = MildcrowdColors.Ink)
                    }

                    complaint.canReview -> ReviewForm(complaint, state, onResult, onExplanation, onStatement) {
                        // A chosen result is confirmed first; without one (or a new decision without measure, ground or the duration of
                        // a lock) the server names what is missing.
                        val incomplete = state.result.isEmpty() ||
                            (
                                state.result == "overturned" &&
                                    (state.statement.outcome.isEmpty() || state.statement.ground.isEmpty() || state.statement.lacksDuration)
                                )
                        if (incomplete) onReview() else confirmReview = complaint
                    }

                    else -> Text(
                        "Diesen Widerspruch prüft eine andere Person aus dem Team, weil du die ursprüngliche Entscheidung getroffen hast.",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Ink,
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.XS.dp))
                            .padding(14.dp)
                            .testTag("complaint.otherReviewer")
                    )
                }
            }
        }
    }
    confirmReview?.let { complaint ->
        ConfirmDialog(
            "Ergebnis mitteilen?",
            "Speichern und mitteilen",
            { scope.launch { onReview() } },
            { confirmReview = null },
            message = reviewSummary(state, complaint),
            confirmTag = "complaint.save.confirm"
        )
    }
}

/** What the result does, before it goes out to the complainant and the affected person. */
internal fun reviewSummary(state: ComplaintReviewUiState, complaint: ComplaintDetail): String {
    if (state.result != "overturned") {
        return "Die Entscheidung bleibt bestehen. Die Person, die widersprochen hat, bekommt die Begründung per E-Mail."
    }
    val outcome = state.statement.outcomeLabel(complaint.outcomes)
    val ground = complaint.grounds.firstOrNull { it.value == state.statement.ground }?.label.orEmpty()
    return "Die Entscheidung wird geändert: $outcome · $ground. Die Betroffenen bekommen die Begründung per E-Mail."
}

@Composable
private fun ReviewForm(
    complaint: ComplaintDetail,
    state: ComplaintReviewUiState,
    onResult: (String) -> Unit,
    onExplanation: (String) -> Unit,
    onStatement: (StatementOfReasons) -> Unit,
    onSave: suspend () -> Unit
) {
    SectionCard("Prüfung") {
        RadioList(
            "Ergebnis",
            listOf(Labelled("upheld", "Entscheidung bleibt bestehen"), Labelled("overturned", "Entscheidung wird geändert")),
            state.result,
            onResult,
            "complaint.result",
            state.fieldErrors["result"]
        )
        if (state.result == "overturned") {
            Column(
                Modifier.fillMaxWidth().border(1.dp, MildcrowdColors.DuneLine, RoundedCornerShape(MildcrowdRadius.XS.dp)).padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text("Neue Entscheidung", style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
                Text(
                    "Die Begründung für die Betroffenen ist zugleich die Begründung des Ergebnisses.",
                    style = MildcrowdType.Caption,
                    color = MildcrowdColors.Muted
                )
                DecisionFields(
                    state.statement,
                    onStatement,
                    complaint.outcomes,
                    complaint.suspension,
                    complaint.grounds,
                    state.fieldErrors,
                    "review"
                )
            }
        } else {
            MildcrowdTextField(
                "Begründung des Ergebnisses",
                state.explanation,
                onExplanation,
                Modifier.testTag("complaint.explanation"),
                error = state.fieldErrors["explanation"],
                lines = 5..12
            )
        }
        AsyncMildcrowdButton("Ergebnis speichern und mitteilen", onSave, Modifier.testTag("complaint.save"))
    }
}
