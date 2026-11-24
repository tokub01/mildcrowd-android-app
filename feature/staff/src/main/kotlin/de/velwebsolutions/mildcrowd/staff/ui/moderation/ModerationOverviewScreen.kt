package de.velwebsolutions.mildcrowd.staff.ui.moderation

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.MildcrowdKeyboard
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.RelativeTimeText
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.SettingToggle
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.StaffRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.api.CaseSummary
import de.velwebsolutions.mildcrowd.staff.api.ModerationOverview
import de.velwebsolutions.mildcrowd.staff.api.QueueFilter
import de.velwebsolutions.mildcrowd.staff.ui.components.LoadMoreButton
import de.velwebsolutions.mildcrowd.staff.ui.components.OptionPicker
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffArea
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffAreaLinks
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffDate
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffHeading
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffLoadable
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffPage
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffTitle
import de.velwebsolutions.mildcrowd.staff.ui.components.staffBox
import de.velwebsolutions.mildcrowd.staff.ui.retry

/** The moderation start page (web: livewire.moderation.overview with the embedded queue and the staff navigation). */
@Composable
fun ModerationOverviewScreen(viewModel: ModerationOverviewViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    ModerationOverviewContent(state, viewModel.retry(viewModel::load), viewModel::apply, viewModel::loadMore, viewModel::dismissError)
}

/** The start page without its view model, for the app and for tests. */
@Composable
fun ModerationOverviewContent(
    state: ModerationOverviewUiState,
    onRefresh: suspend () -> Unit,
    onFilter: (QueueFilter) -> Unit,
    onLoadMore: suspend () -> Unit,
    onDismissError: () -> Unit = {}
) {
    MildcrowdScreen("Moderation", error = state.error, onDismissError = onDismissError) {
        StaffLoadable(state.overview, onRefresh) { overview ->
            StaffPage(onRefresh, spacing = 20) {
                Header(overview.figures)
                FigureGrid(overview.figures, onFilter)
                StaffAreas(overview.navigation)
                QueueSection(state, onFilter, onLoadMore)
                ActivitySection(overview.activity)
            }
        }
    }
}

@Composable
private fun Header(figures: ModerationOverview.Figures) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        StaffTitle("Moderation")
        Text(
            "Meldungen nach Art. 16 DSA. Jede Entscheidung braucht eine Begründung, die die Betroffenen verstehen.",
            style = MildcrowdType.Body,
            color = MildcrowdColors.Ink2
        )
        Text(mutedLead("Älteste wartende Meldung: ", figures.oldestWaitingLabel), style = MildcrowdType.Small)
        Text(mutedLead("Median bis zur Entscheidung (30 Tage): ", figures.medianLabel), style = MildcrowdType.Small)
    }
}

private fun mutedLead(lead: String, value: String) = buildAnnotatedString {
    withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append(lead) }
    withStyle(SpanStyle(fontWeight = FontWeight.Bold, color = MildcrowdColors.Ink)) { append(value) }
}

/** The six figure cards; each opens the queue with the matching filter like the web's links. */
@Composable
private fun FigureGrid(figures: ModerationOverview.Figures, onFilter: (QueueFilter) -> Unit) {
    data class Card(val label: String, val value: Int, val hint: String, val filter: QueueFilter, val highlight: Boolean)
    val cards = listOf(
        Card("Aktive Fälle", figures.active, "offen, in Prüfung oder mit Beschwerde", QueueFilter(), false),
        Card("Ohne Zuständigkeit", figures.unassigned, "noch niemandem zugewiesen", QueueFilter(status = "open"), true),
        Card("Dringend", figures.urgent, "zuerst bearbeiten", QueueFilter(priority = "urgent"), true),
        Card("Meine Fälle", figures.mine, "dir zugewiesen", QueueFilter(mine = true), false),
        Card(
            "Offene Beschwerden",
            figures.complaints,
            "Art. 20 DSA, zweite Person entscheidet",
            QueueFilter(status = "complaint_open"),
            true
        ),
        Card("Entschieden", figures.decidedWeek, "in den letzten 7 Tagen", QueueFilter(status = "decided"), false)
    )
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        cards.chunked(2).forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                row.forEach { card ->
                    val alert = card.highlight && card.value > 0
                    val foreground = if (alert) MildcrowdColors.SunInk else MildcrowdColors.Ink
                    Column(
                        Modifier
                            .weight(1f)
                            .defaultMinSize(minHeight = 104.dp)
                            .background(if (alert) MildcrowdColors.Sun else MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.S.dp))
                            .semantics(mergeDescendants = true) {}
                            .clickable(onClickLabel = "Zeigt diese Fälle in der Warteschlange", role = Role.Button) {
                                onFilter(card.filter)
                            }
                            .padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("${card.value}", style = MildcrowdType.heading(28), color = foreground)
                        Text(card.label, style = MildcrowdType.SmallBold, color = foreground)
                        Text(card.hint, style = MildcrowdType.Caption, color = if (alert) MildcrowdColors.SunInk else MildcrowdColors.Muted)
                    }
                }
            }
        }
    }
}

/** The staff navigation of the web layout: Sperren, Rollen and the admin pages. */
@Composable
private fun StaffAreas(navigation: ModerationOverview.Navigation) {
    val areas = listOf(StaffArea.Suspensions) +
        (if (navigation.canManageRoles) listOf(StaffArea.Roles) else emptyList()) +
        (if (navigation.canAdministrate) listOf(StaffArea.Admin) + StaffArea.adminPages else emptyList())
    Panel(spacing = 6) {
        Text("${navigation.displayName} · ${navigation.role.label}", style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
        StaffAreaLinks(areas)
    }
}

/** Filters and the queue (web: livewire.moderation.queue). */
@Composable
private fun QueueSection(state: ModerationOverviewUiState, onFilter: (QueueFilter) -> Unit, onLoadMore: suspend () -> Unit) {
    val navigator = LocalNavigator.current
    val filter = state.filter
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        StaffHeading("Warteschlange")
        Panel {
            OptionPicker("Status", QueueFilter.statuses, filter.status, {
                onFilter(filter.copy(status = it))
            }, Modifier.testTag("queue.status"))
            OptionPicker("Priorität", QueueFilter.priorities, filter.priority, { onFilter(filter.copy(priority = it)) })
            MildcrowdTextField(
                "Gemeldeter Link enthält",
                filter.search,
                { onFilter(filter.copy(search = it)) },
                keyboardOptions = MildcrowdKeyboard.NoCapitalization
            )
            SettingToggle("Nur meine Fälle", filter.mine, { onFilter(filter.copy(mine = it)) })
        }
        if (state.cases.isEmpty() && !state.isLoadingQueue) {
            Text(
                "Keine Fälle für diese Auswahl. Gute Arbeit.",
                style = MildcrowdType.Body,
                color = MildcrowdColors.Ink2,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().background(
                    MildcrowdColors.Paper,
                    RoundedCornerShape(MildcrowdRadius.S.dp)
                ).padding(28.dp)
            )
        }
        state.cases.forEach { item ->
            CaseRow(item, Modifier.pressable({ navigator.open(StaffRoute.ModerationCase(item.id)) }).testTag("queue.case.${item.id}"))
        }
        if (state.hasMore) LoadMoreButton("Weitere Fälle laden", onLoadMore, Modifier.testTag("queue.more"))
    }
}

/** One queue row: case number with badges, reported link with the reason, status, assignee, arrival. */
@Composable
private fun CaseRow(item: CaseSummary, modifier: Modifier) {
    Column(
        modifier.staffBox().padding(14.dp).semantics(mergeDescendants = true) {},
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
            Text(
                "#${item.id}",
                style = MildcrowdType.BodyBold.copy(textDecoration = TextDecoration.Underline),
                color = MildcrowdColors.Night
            )
            item.escalation?.let { Pill(it.label, tone = PillTone.Night) }
            if (item.priority.value == "urgent") Pill(item.priority.label, tone = PillTone.Sun)
            Box(Modifier.weight(1f))
            Text(StaffDate.short(item.createdAt), style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
        }
        Text(
            item.subjectUrl,
            style = MildcrowdType.SmallBold,
            color = MildcrowdColors.Night,
            maxLines = 1,
            overflow = TextOverflow.MiddleEllipsis
        )
        item.reasonExcerpt?.let { Text(it, style = MildcrowdType.Small, color = MildcrowdColors.Ink2) }
        Text("${item.status.label} · Zuständig: ${item.assigneeName ?: "–"}", style = MildcrowdType.Caption, color = MildcrowdColors.Ink2)
    }
}

/** "Letzte Aktivität". */
@Composable
private fun ActivitySection(activity: List<ModerationOverview.Activity>) {
    val navigator = LocalNavigator.current
    SectionCard("Letzte Aktivität") {
        if (activity.isEmpty()) Text("Noch keine Aktivität.", style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
        activity.forEach { event ->
            Column(
                Modifier.fillMaxWidth().pressable({ navigator.open(StaffRoute.ModerationCase(event.caseId)) }),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text("Fall #${event.caseId} · ${event.type.label}", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                Row {
                    Text("${event.actorName ?: "System"} · ", style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
                    RelativeTimeText(event.createdAt)
                }
            }
        }
    }
}
