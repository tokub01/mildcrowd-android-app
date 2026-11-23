package de.velwebsolutions.mildcrowd.network.ui.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.DiscoverContext
import de.velwebsolutions.mildcrowd.network.ui.PostListState
import de.velwebsolutions.mildcrowd.network.ui.components.CrowdMark
import de.velwebsolutions.mildcrowd.network.ui.components.FeedHandlers
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.TextLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.feedSection
import de.velwebsolutions.mildcrowd.network.ui.components.handlers
import de.velwebsolutions.mildcrowd.network.ui.components.membersLabel
import de.velwebsolutions.mildcrowd.network.ui.retry

/**
 * A list page around its content: card skeletons, the error with retry, then the page with pull to refresh and the
 * status toast. After a Post was written the view model puts it on top (and the page scrolls up) or loads again.
 */
@Composable
fun <C> FeedScaffold(
    viewModel: FeedViewModel<C>,
    title: String,
    actions: @Composable RowScope.() -> Unit = {},
    content: LazyListScope.(PostListState<C>, FeedHandlers) -> Unit
) {
    val state by viewModel.list.state.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val handlers = viewModel.list.handlers(viewModel::launch)
    LaunchedEffect(Unit) { viewModel.list.loadIfNeeded() }
    MildcrowdScreen(
        title = title,
        actions = actions,
        message = state.status,
        error = state.actionError?.userMessage,
        onDismissMessage = viewModel.list::dismissStatus,
        onDismissError = viewModel.list::dismissError
    ) {
        LoadableView(state.state, onRetry = viewModel.retry(viewModel.list::reload), skeleton = SkeletonLayout.Cards) {
            FeedPage(listState, handlers) { content(state, handlers) }
        }
    }
}

/** The scrolling page of a feed with pull to refresh. */
@Composable
fun FeedPage(listState: LazyListState, handlers: FeedHandlers, bottomInset: Dp = 0.dp, content: LazyListScope.() -> Unit) {
    NetworkPage(state = listState, onRefresh = handlers.reload, bottomInset = bottomInset, content = content)
}

/** "Entdecken" (/app/entdecken): public Posts of public profiles, newest first, and the largest crowds. No ranking. */
@Composable
fun DiscoverScreen(viewModel: DiscoverViewModel = hiltViewModel<DiscoverViewModel>()) {
    FeedScaffold(viewModel, "Entdecken") { state, handlers ->
        discoverContent(state, handlers)
    }
}

/** Entdecken's page. */
fun LazyListScope.discoverContent(state: PostListState<DiscoverContext>, handlers: FeedHandlers) {
    block("header") {
        PageHeader(
            "Wer schon da ist",
            eyebrow = "Entdecken",
            lead = "Posts aus der Gemeinschaft, der Reihe nach. Keine Empfehlungen nach Klicks."
        )
    }
    block("buttons") {
        val navigator = LocalNavigator.current
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MildcrowdButton(
                "Suchen",
                { navigator.open(NetworkRoute.Search()) },
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                leading = {
                    Icon(Icons.Outlined.Search, contentDescription = null, tint = MildcrowdColors.Night, modifier = Modifier.size(18.dp))
                }
            )
            MildcrowdButton("Durchblättern", { navigator.open(NetworkRoute.Swipe) }, kind = MildcrowdButtonKind.Accent, fullWidth = false)
        }
    }
    val crowds = state.context?.crowds.orEmpty()
    if (crowds.isNotEmpty()) {
        block("crowds") {
            val navigator = LocalNavigator.current
            LazyRow(Modifier.semantics { contentDescription = "Crowds" }, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                items(crowds, key = { it.id }) { crowd ->
                    Row(
                        Modifier
                            .background(MildcrowdColors.Paper, CircleShape)
                            .pressable({ navigator.open(NetworkRoute.Crowd(crowd.slug)) })
                            .padding(horizontal = 12.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CrowdMark(crowd)
                        Column {
                            Text(
                                crowd.name,
                                style = MildcrowdType.SmallBold,
                                color = MildcrowdColors.Night,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                crowd.membersCount?.let(::membersLabel).orEmpty(),
                                style = MildcrowdType.Caption,
                                color = MildcrowdColors.Muted
                            )
                        }
                    }
                }
            }
        }
    }
    feedSection(state, handlers, empty = "Hier gibt es noch keine Posts.")
    if (state.reachedEnd || state.posts.isEmpty()) {
        block("side.how") {
            SideCard("So funktioniert Entdecken", tone = PanelTone.Dune) {
                Bullets(
                    listOf(
                        "Hier stehen nur Posts von Mitgliedern, die ihr Profil für alle geöffnet haben. Sichtbar ist das nur für Mitglieder.",
                        "Neueste zuerst – niemand bezahlt dafür, weiter oben zu stehen.",
                        "Folge Menschen, dann landen ihre Posts auf deiner Startseite."
                    )
                )
                TextLink("Alle Crowds", NetworkRoute.Crowds)
            }
        }
    }
}

/**
 * "Gemerkt" (/app/gemerkt): the private bookmark list, newest bookmark first. A Post leaves it once it is no longer
 * bookmarked.
 */
@Composable
fun BookmarksScreen(viewModel: BookmarksViewModel = hiltViewModel<BookmarksViewModel>()) {
    FeedScaffold(viewModel, "Gemerkt") { state, handlers ->
        block("header") {
            PageHeader(
                "Gemerkt",
                eyebrow = "Merkliste",
                lead = "Deine private Liste gespeicherter Posts. Niemand sieht, wer was gemerkt hat – auch Autor:innen nicht."
            )
        }
        feedSection(
            state,
            handlers,
            empty = "Du hast dir noch keine Posts gemerkt. In jeder Post-Karte findest du den Knopf „Merken“."
        )
        block("side.quiet") {
            SideCard("Privat und leise", tone = PanelTone.Dune) {
                Text(
                    "Lesezeichen sind ausschließlich für dich sichtbar. Es gibt keine öffentlichen Listen oder Zähler für gemerkte Posts."
                )
                Text(
                    "Posts, die gelöscht oder aus einer Crowd entfernt wurden, oder deren Autor:innen blockiert sind, verschwinden " +
                        "automatisch aus deiner Liste."
                )
            }
        }
        block("side.safety") {
            val navigator = LocalNavigator.current
            SideCard("Sicherheit") {
                Text("Verwalte blockierte, stummgeschaltete und eingeschränkte Konten.")
                MildcrowdButton("Konten verwalten", {
                    navigator.open(AccountRoute.Safety)
                }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            }
        }
    }
}

/** A topic page (/themen/{tag}): every readable Post with #tag, newest first. */
@Composable
fun TagScreen(
    tag: String,
    viewModel: TagViewModel = hiltViewModel<TagViewModel, TagViewModel.Factory>(creationCallback = { it.create(tag.lowercase()) })
) {
    val lowered = tag.lowercase()
    FeedScaffold(viewModel, "#$lowered") { state, handlers ->
        block("header") { PageHeader("#$lowered", eyebrow = "Thema", lead = "Alle Posts mit #$lowered, neueste zuerst.") }
        if (state.posts.isEmpty()) {
            block("empty") {
                val navigator = LocalNavigator.current
                DashedNote {
                    Text("Noch keine Posts zu #$lowered.", style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
                    if (state.context?.canPost == true) {
                        MildcrowdButton(
                            "Den ersten Post schreiben",
                            { navigator.present(NetworkRoute.Compose()) },
                            fullWidth = false,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    } else {
                        MildcrowdButton(
                            "Etwas anderes suchen",
                            { navigator.open(NetworkRoute.Search()) },
                            kind = MildcrowdButtonKind.Secondary,
                            fullWidth = false,
                            modifier = Modifier.padding(top = 8.dp)
                        )
                    }
                }
            }
        } else {
            feedSection(state, handlers)
        }
        block("side.topics") {
            SideCard("Themen auf Mildcrowd", tone = PanelTone.Dune) {
                Text(
                    "Schreib #$lowered in deinen Posts, dann erscheint er hier. Es gibt keine „Trends“ – nur die Reihenfolge, in der geschrieben wurde."
                )
                TextLink("Zur Suche", NetworkRoute.Search())
            }
        }
    }
}
