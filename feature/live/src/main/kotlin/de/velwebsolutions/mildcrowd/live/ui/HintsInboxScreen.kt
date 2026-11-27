package de.velwebsolutions.mildcrowd.live.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.ErrorStateView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.components.SkeletonView
import de.velwebsolutions.mildcrowd.design.navigation.DeepLink
import de.velwebsolutions.mildcrowd.design.navigation.LiveRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.WebRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.live.api.Hint
import kotlinx.coroutines.launch

/** Hinweise (/app/hinweise): what concerns the member, newest first, under "Heute", "Diese Woche" and "Früher". */
@Composable
internal fun HintsInboxScreen(viewModel: HintsInboxViewModel = hiltViewModel()) {
    val navigator = LocalNavigator.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val atTheBell by viewModel.unreadAtTheBell.collectAsStateWithLifecycle()
    MildcrowdScreen(
        title = "Hinweise",
        message = ui.message,
        error = ui.error,
        onDismissMessage = viewModel::dismissMessage,
        onDismissError = viewModel::dismissError
    ) {
        HintsInboxContent(
            ui = ui,
            unreadAtTheBell = atTheBell,
            onRefresh = viewModel::load,
            onMarkAllRead = viewModel::markAllRead,
            onLoadMore = viewModel::loadMore,
            // The hint's link is a web URL: an app screen when there is one, otherwise the browser.
            onOpen = { url -> navigator.open(DeepLink.route(url) ?: WebRoute(url)) },
            onSettings = { navigator.open(LiveRoute.Settings) }
        )
    }
}

/** The list itself, without view model (Robolectric tests). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun HintsInboxContent(
    ui: HintsInboxUiState,
    unreadAtTheBell: Int,
    onRefresh: suspend () -> Unit,
    onMarkAllRead: suspend () -> Unit,
    onLoadMore: () -> Unit,
    onOpen: (url: String) -> Unit,
    onSettings: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var refreshing by remember { mutableStateOf(false) }
    // Unread hints may wait on later pages: the bell counts them.
    val hasUnread = ui.showsUnread || unreadAtTheBell > 0
    val lastId = ui.state.valueOrNull?.lastOrNull()?.id
    PullToRefreshBox(
        isRefreshing = refreshing,
        onRefresh = {
            scope.launch {
                refreshing = true
                onRefresh()
                refreshing = false
            }
        },
        modifier = Modifier.fillMaxSize()
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize().testTag("hints.list"),
            contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item(key = "header") {
                Column(Modifier.widthIn(max = 720.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    PageHeader("Hinweise", eyebrow = "Für dich", lead = "Was dich betrifft, in Ruhe und der Reihe nach.")
                    if (hasUnread) {
                        AsyncMildcrowdButton(
                            "Alle als gelesen markieren",
                            onMarkAllRead,
                            kind = MildcrowdButtonKind.Secondary,
                            fullWidth = false,
                            modifier = Modifier.testTag("hints.readAll")
                        )
                    }
                }
            }
            when (val state = ui.state) {
                Loadable.Idle, Loadable.Loading -> item(key = "skeleton") {
                    SkeletonView(Modifier.heightIn(max = 640.dp), SkeletonLayout.Rows)
                }

                is Loadable.Failed -> item(key = "error") { ErrorStateView(state.error, onRefresh, Modifier.heightIn(max = 400.dp)) }

                is Loadable.Loaded -> {
                    if (state.value.isEmpty()) {
                        item(key = "empty") {
                            DashedNote(
                                "Hier gibt es gerade keine Hinweise für dich.",
                                Modifier.widthIn(max = 720.dp).testTag("hints.empty")
                            )
                        }
                    } else {
                        ui.groups.forEach { group ->
                            item(key = "group.${group.title}") {
                                Text(
                                    group.title,
                                    style = MildcrowdType.heading(),
                                    color = MildcrowdColors.Night,
                                    modifier = Modifier.widthIn(max = 720.dp).fillMaxWidth().semantics { heading() }
                                )
                            }
                            items(group.hints, key = { it.id }) { hint ->
                                HintRow(hint, onOpen)
                                if (hint.id == lastId) LaunchedEffect(hint.id) { onLoadMore() }
                            }
                        }
                    }
                    // Also under an empty first page: older pages may still hold hints (like the web's page links).
                    if (ui.isLoadingMore) {
                        item(key = "more") { CircularProgressIndicator(color = MildcrowdColors.Night) }
                    } else if (ui.offersMore) {
                        // A page failed, or several held nothing to show: scrolling alone would not go on.
                        item(key = "more.button") {
                            MildcrowdButton(
                                "Ältere Hinweise laden",
                                onLoadMore,
                                kind = MildcrowdButtonKind.Secondary,
                                fullWidth = false,
                                modifier = Modifier.testTag("hints.more")
                            )
                        }
                    }
                }
            }
            item(key = "side") {
                SideCard("Du entscheidest", Modifier.widthIn(max = 720.dp).padding(top = 8.dp), tone = PanelTone.Dune) {
                    Text("Wähle, welche Hinweise du erhalten möchtest. Eine tägliche E-Mail gibt es nur, wenn du sie einschaltest.")
                    MildcrowdTextButton("Hinweise einstellen", onSettings, Modifier.heightIn(min = 44.dp).testTag("hints.settings"))
                }
            }
        }
    }
}

/** One hint card: "Neu", the label as link, the excerpt and the time. */
@Composable
private fun HintRow(hint: Hint, onOpen: (url: String) -> Unit) {
    val shape = RoundedCornerShape(MildcrowdRadius.S.dp)
    Column(
        Modifier
            .widthIn(max = 720.dp)
            .fillMaxWidth()
            .background(if (hint.unread) MildcrowdColors.Sand else MildcrowdColors.Paper, shape)
            .border(1.dp, if (hint.unread) MildcrowdColors.SpringText else MildcrowdColors.PaperLine, shape)
            .padding(20.dp)
            .testTag("hint.${hint.id}"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        if (hint.unread) Pill("Neu", tone = PillTone.Sun)
        val url = hint.url
        if (url != null) {
            Box(
                Modifier.fillMaxWidth().heightIn(min = 44.dp).clickable(role = Role.Button) { onOpen(url) },
                contentAlignment = Alignment.CenterStart
            ) {
                Text(
                    hint.label,
                    style = MildcrowdType.BodyBold.copy(textDecoration = TextDecoration.Underline),
                    color = MildcrowdColors.Night
                )
            }
        } else {
            Text(hint.label, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night, modifier = Modifier.padding(vertical = 8.dp))
        }
        if (hint.excerpt.isNotEmpty()) Text(hint.excerpt, style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
        Text(GermanTime.format(hint.createdAt, "d. MMMM yyyy, HH:mm"), style = MildcrowdType.Small, color = MildcrowdColors.Muted)
    }
}
