package de.velwebsolutions.mildcrowd.staff.ui.admin

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.api.AdminDashboard
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffArea
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffAreaLinks
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffDate
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffLoadable
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffPage
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffTitle
import de.velwebsolutions.mildcrowd.staff.ui.components.TileGrid
import de.velwebsolutions.mildcrowd.staff.ui.components.boldLead
import de.velwebsolutions.mildcrowd.staff.ui.retry
import kotlinx.coroutines.launch

/** Admin statistics (web: livewire.admin.dashboard). Sums only, never people or content. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminDashboardScreen(viewModel: AdminDashboardViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    MildcrowdScreen("Statistik", error = state.error, onDismissError = viewModel::dismissError) {
        StaffLoadable(state.dashboard, viewModel.retry(viewModel::load)) { stats ->
            StaffPage(viewModel::load, spacing = 22) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("Admin")
                    StaffTitle("Statistik")
                    Text(
                        "Nur Summen. Zeitraumwerte beziehen sich auf die letzten ${stats.days} Tage.",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Ink2
                    )
                    SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth().testTag("admin.period")) {
                        stats.periods.forEachIndexed { index, period ->
                            SegmentedButton(
                                selected = state.days == period,
                                onClick = { scope.launch { viewModel.selectDays(period) } },
                                shape = SegmentedButtonDefaults.itemShape(index, stats.periods.size),
                                colors = SegmentedButtonDefaults.colors(
                                    activeContainerColor = MildcrowdColors.Night,
                                    activeContentColor = MildcrowdColors.OnNight,
                                    inactiveContainerColor = MildcrowdColors.Paper,
                                    inactiveContentColor = MildcrowdColors.Night
                                )
                            ) { Text("$period Tage", style = MildcrowdType.Caption) }
                        }
                    }
                }
                Panel { StaffAreaLinks(StaffArea.adminPages) }
                RevenueCard(stats.revenue) { month -> scope.launch { viewModel.showMonth(month) } }
                DashboardSection("KI-Prüfung (Mistral)", "(dieser Monat, nur diese Installation)") {
                    TileGrid(stats.ai.tiles)
                    stats.ai.note?.let { Text(it, style = MildcrowdType.Caption, color = MildcrowdColors.Ink2) }
                }
                DashboardSection("Mitglieder und Beiträge") {
                    TileGrid(stats.members.tiles)
                    if (stats.members.byTier.isNotEmpty()) {
                        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            stats.members.byTier.forEach { tier ->
                                Text(
                                    boldLead(tier.name, ": ${tier.count}"),
                                    style = MildcrowdType.Small,
                                    color = MildcrowdColors.Ink,
                                    modifier = Modifier.background(
                                        MildcrowdColors.Paper,
                                        CircleShape
                                    ).padding(horizontal = 14.dp, vertical = 6.dp)
                                )
                            }
                        }
                    }
                }
                DashboardSection("Netzwerk") {
                    TileGrid(stats.network.tiles)
                    WeekChart(stats.network.weeks)
                }
                DashboardSection("Empfehlungen") { TileGrid(stats.referrals.tiles) }
                DashboardSection("Chat", "(nur Metadaten – Inhalte sind verschlüsselt)") { TileGrid(stats.chat.tiles) }
                DashboardSection("Moderation") { TileGrid(stats.moderation.tiles) }
            }
        }
    }
}

@Composable
private fun DashboardSection(title: String, subtitle: String? = null, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(MildcrowdType.heading().toSpanStyle().copy(color = MildcrowdColors.Night)) { append(title) }
                if (subtitle !=
                    null
                ) {
                    withStyle(MildcrowdType.Small.toSpanStyle().copy(color = MildcrowdColors.Muted)) { append(" $subtitle") }
                }
            },
            modifier = Modifier.semantics { heading() }
        )
        content()
    }
}

/** The dark revenue card with month browsing (never into the future). */
@Composable
private fun RevenueCard(revenue: AdminDashboard.Revenue, onShow: (String) -> Unit) {
    Column(
        Modifier.fillMaxWidth().background(MildcrowdColors.Night, RoundedCornerShape(MildcrowdRadius.M.dp)).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(revenue.title, style = MildcrowdType.heading(), color = MildcrowdColors.OnNight, modifier = Modifier.semantics { heading() })
        Text(revenue.period, style = MildcrowdType.Caption, color = MildcrowdColors.OnNightMuted)
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            MildcrowdButton("‹ Vormonat", {
                onShow(revenue.previousMonth)
            }, Modifier.testTag("revenue.previous"), kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            revenue.nextMonth?.let { next ->
                MildcrowdButton("Nächster Monat ›", { onShow(next) }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
                MildcrowdButton("Dieser Monat", { onShow("") }, kind = MildcrowdButtonKind.Accent, fullWidth = false)
            }
        }
        Text(
            revenue.amount,
            style = MildcrowdType.display(44),
            color = MildcrowdColors.OnNight,
            modifier = Modifier.testTag("revenue.amount")
        )
        Text(revenue.payments, style = MildcrowdType.Small, color = MildcrowdColors.OnNightMuted)
        Text(revenue.fees, style = MildcrowdType.Caption, color = MildcrowdColors.OnNightMuted)
        Text(revenue.comparison, style = MildcrowdType.Caption, color = MildcrowdColors.OnNightMuted)
    }
}

/** "Die letzten 12 Wochen": new accounts and Posts per week as bars, read out per week by TalkBack. */
@Composable
private fun WeekChart(weeks: List<AdminDashboard.Week>) {
    val maximum = maxOf(1, weeks.maxOfOrNull { maxOf(it.registered, it.posts) } ?: 1)
    Column(
        Modifier.fillMaxWidth().background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.M.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text("Die letzten 12 Wochen", style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Legend("Neue Konten", MildcrowdColors.Spring)
            Legend("Posts", MildcrowdColors.Sun)
        }
        weeks.forEach { week ->
            val posts = if (week.posts == 1) "1 Post" else "${week.posts} Posts"
            Row(
                Modifier.clearAndSetSemantics {
                    contentDescription = "Woche ab ${StaffDate.dayMonth(week.week)}: ${week.registered} neue Konten, $posts"
                },
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    StaffDate.dayMonth(week.week),
                    style = MildcrowdType.Caption,
                    color = MildcrowdColors.Ink,
                    modifier = Modifier.width(64.dp)
                )
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Bar(week.registered, maximum, MildcrowdColors.Spring)
                    Bar(week.posts, maximum, MildcrowdColors.Sun)
                }
            }
        }
    }
}

@Composable
private fun Legend(label: String, color: Color) {
    Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(10.dp).background(color, RoundedCornerShape(2.dp)))
        Text(label, style = MildcrowdType.Caption, color = color)
    }
}

@Composable
private fun Bar(value: Int, maximum: Int, color: Color) {
    BoxWithConstraints(Modifier.fillMaxWidth().height(12.dp)) {
        val width = ((maxWidth - 30.dp) * (value.toFloat() / maximum)).coerceAtLeast(2.dp)
        Row(horizontalArrangement = Arrangement.spacedBy(4.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.width(width).height(10.dp).background(color, CircleShape))
            Text("$value", style = MildcrowdType.Caption, color = MildcrowdColors.Ink)
        }
    }
}
