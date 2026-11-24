package de.velwebsolutions.mildcrowd.network.ui.crowds

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.TierLoadableView
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.PaperBox
import de.velwebsolutions.mildcrowd.network.ui.components.StatGrid
import de.velwebsolutions.mildcrowd.network.ui.components.WeekChart
import de.velwebsolutions.mildcrowd.network.ui.components.WeekRow
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.membersLabel
import de.velwebsolutions.mildcrowd.network.ui.components.postsStayLine
import de.velwebsolutions.mildcrowd.network.ui.retry

/**
 * "„…“ schließen?" (/crowds/{slug}/schliessen): host-only confirmation with an explicit checkbox; Posts stay with their
 * authors. Like the web, closing leads to the crowd overview; the closed crowd's screens leave the tab they were opened
 * on (a crowd reached from the Start or a profile) as well.
 */
@Composable
fun CloseCrowdScreen(
    slug: String,
    viewModel: CloseCrowdViewModel = hiltViewModel<CloseCrowdViewModel, CloseCrowdViewModel.Factory>(creationCallback = { it.create(slug) })
) {
    val closing by viewModel.closing.collectAsStateWithLifecycle()
    val confirmed by viewModel.confirmed.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val closed by viewModel.closed.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(closed) {
        if (closed) {
            navigator.popToRoot()
            navigator.popToRoot(MainTab.Crowds)
            navigator.selectedTab = MainTab.Crowds
        }
    }
    MildcrowdScreen(title = "Crowd schließen") {
        LoadableView(closing, onRetry = viewModel.retry(viewModel::load)) { info ->
            NetworkPage {
                block("header") { PageHeader("„${info.name}“ schließen?") }
                block("form") {
                    PaperBox(spacing = 16) {
                        Bullets(
                            listOf(
                                "Die Crowd verschwindet für alle ${membersLabel(info.membersCount)}.",
                                postsStayLine(info.postsCount),
                                "Das lässt sich nicht rückgängig machen."
                            )
                        )
                        ConsentCheck(
                            "Ja, ich möchte die Crowd endgültig schließen.",
                            confirmed,
                            viewModel::setConfirmed,
                            Modifier.testTag("closeCrowd.confirm")
                        )
                        (error?.message("confirm") ?: error?.userMessage)?.let { FieldError(it) }
                        AsyncMildcrowdButton(
                            "Crowd schließen",
                            viewModel::close,
                            kind = MildcrowdButtonKind.Destructive,
                            fullWidth = false,
                            enabled = confirmed,
                            modifier = Modifier.testTag("closeCrowd.submit")
                        )
                    }
                }
            }
        }
    }
}

/** Host statistics (/crowds/{slug}/statistik): sums only, never lists of people. */
@Composable
fun CrowdStatisticsScreen(
    slug: String,
    viewModel: CrowdStatisticsViewModel =
        hiltViewModel<CrowdStatisticsViewModel, CrowdStatisticsViewModel.Factory>(creationCallback = { it.create(slug) })
) {
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.load() }
    MildcrowdScreen(title = "Statistik") {
        TierLoadableView(
            statistics,
            "Statistik deiner Crowd",
            onRetry = viewModel.retry(viewModel::load),
            feature = "Wie deine Crowd wächst und wie lebendig sie ist: neue Mitglieder und Posts pro Woche über zwölf Wochen, " +
                "nur für Gastgeber:innen. Wer was liest oder schreibt, zeigen die Zahlen nie."
        ) { stats ->
            NetworkPage {
                block("header") {
                    PageHeader(
                        "Statistik: ${stats.name}",
                        eyebrow = "Nur für Gastgeber:innen",
                        lead = "Nur Summen. Wer was liest oder schreibt, zeigen wir hier bewusst nicht."
                    )
                }
                block("tiles") {
                    StatGrid(
                        listOf(
                            stats.totals.members to "Mitglieder",
                            stats.totals.hosts to "Gastgeber:innen",
                            stats.totals.posts to "Posts insgesamt",
                            stats.totals.posts30Days to "Posts in 30 Tagen",
                            stats.totals.activeAuthors30Days to "Menschen, die in 30 Tagen geschrieben haben",
                            stats.totals.removed to "Von euch entfernte Posts"
                        )
                    )
                }
                block("weeks") {
                    WeekChart(
                        "Die letzten 12 Wochen",
                        listOf("Neue Mitglieder", "Posts"),
                        stats.weekly.map { WeekRow(it.week, listOf(it.joined, it.posts)) }
                    )
                }
            }
        }
    }
}
