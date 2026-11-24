package de.velwebsolutions.mildcrowd.network.ui.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ArrowCircleRight
import androidx.compose.material.icons.outlined.EditNote
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.onClick
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.auth.SessionState
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.Meetup
import de.velwebsolutions.mildcrowd.network.api.MemoryYear
import de.velwebsolutions.mildcrowd.network.api.SourceContext
import de.velwebsolutions.mildcrowd.network.ui.PostListState
import de.velwebsolutions.mildcrowd.network.ui.components.ComposeFab
import de.velwebsolutions.mildcrowd.network.ui.components.CrowdRow
import de.velwebsolutions.mildcrowd.network.ui.components.FabInset
import de.velwebsolutions.mildcrowd.network.ui.components.FeedHandlers
import de.velwebsolutions.mildcrowd.network.ui.components.PostCard
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.TextLink
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.feedSection
import de.velwebsolutions.mildcrowd.network.ui.components.handlers
import de.velwebsolutions.mildcrowd.network.ui.crowds.meetupWhen
import de.velwebsolutions.mildcrowd.network.ui.retry
import java.time.Instant

/**
 * "Start" (/app): own Posts and those of friends, followed people and crowds, newest first, with bundled replies – or
 * "Ganz Mildcrowd". No ranking and an honest end; the side blocks (Deine Woche, Deine Crowds, a running vote, inviting) follow
 * the feed like on a phone-sized web page. Switching the view keeps the page and swaps the cards in place.
 */
@Composable
fun SourceScreen(viewModel: SourceViewModel = hiltViewModel<SourceViewModel>()) {
    val state by viewModel.list.state.collectAsStateWithLifecycle()
    val session by viewModel.session.collectAsStateWithLifecycle()
    val scrollToTop by viewModel.scrollToTop.collectAsStateWithLifecycle()
    val showOlder by viewModel.showOlder.collectAsStateWithLifecycle()
    val memories by viewModel.memories.collectAsStateWithLifecycle()
    val meetups by viewModel.meetups.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val listState = rememberLazyListState()
    val me = (session as? SessionState.SignedIn)?.me

    val cardSnoozed by viewModel.cardSnoozed.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.list.loadIfNeeded() }
    LaunchedEffect(Unit) {
        viewModel.loadMemories()
        viewModel.loadMeetups()
    }
    LaunchedEffect(scrollToTop) { if (scrollToTop > 0) listState.animateScrollToItem(0) }

    MildcrowdScreen(
        title = "Start",
        message = state.status,
        error = state.actionError?.userMessage,
        onDismissMessage = viewModel.list::dismissStatus,
        onDismissError = viewModel.list::dismissError
    ) {
        val canPost = state.context?.canPost != false
        Box(Modifier.fillMaxSize()) {
            LoadableView(state.state, onRetry = viewModel.retry(viewModel.list::reload), skeleton = SkeletonLayout.Cards) {
                val handlers = viewModel.list.handlers(viewModel::launch)
                FeedPage(listState, handlers, bottomInset = if (canPost) FabInset else 0.dp) {
                    sourceContent(
                        SeenCut.of(state, showOlder),
                        me?.displayName.orEmpty(),
                        handlers,
                        onSelectView = viewModel::selectView,
                        extras = SourceExtras(
                            memories,
                            viewModel::dismissMemories,
                            viewModel::showOlderPosts,
                            meetups,
                            // „Mach deine Startseite lebendig“ until the tour is done, unless „Später“ hides it.
                            onSnoozeOnboarding = viewModel::snoozeOnboardingCard.takeIf { me?.onboarded == false && !cardSnoozed }
                        )
                    )
                }
            }
            if (state.state.valueOrNull != null && canPost) {
                ComposeFab(listState, onClick = { navigator.present(NetworkRoute.Compose()) }, Modifier.testTag("source.compose"))
            }
        }
    }
}

/** „Heute vor einem Jahr“ and „Alles Neue gesehen“ on the Start. */
class SourceExtras(
    val memories: List<MemoryYear>,
    val onDismissMemories: suspend () -> Unit,
    val onShowOlder: () -> Unit,
    /** „Nächste Treffen“. */
    val meetups: List<Meetup> = emptyList(),
    /** „Später“ of the card „Mach deine Startseite lebendig“; null while the card does not show. */
    val onSnoozeOnboarding: (() -> Unit)? = null
) {
    companion object {
        val None = SourceExtras(emptyList(), {}, {})
    }
}

/** The Start's page: greeting, view switch, composer entry, next steps, memories, the feed and the side blocks. */
@OptIn(ExperimentalMaterial3Api::class)
fun LazyListScope.sourceContent(
    cut: SeenCut,
    displayName: String,
    feedHandlers: FeedHandlers,
    onSelectView: (String) -> Unit,
    extras: SourceExtras = SourceExtras.None
) {
    val state = cut.state
    // Older Posts wait for „Ältere Posts“ after „Du hast alles Neue gesehen.“
    val handlers = if (cut.seen) FeedHandlers(feedHandlers.perform, {}, feedHandlers.reload) else feedHandlers
    val context = state.context
    val showAll = context?.showAll ?: false
    block("header") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("Start")
            Text(
                "Hallo $displayName.",
                style = MildcrowdType.display(28),
                color = MildcrowdColors.Night,
                modifier = Modifier.semantics {
                    heading()
                }
            )
            Text(
                if (showAll) {
                    "Hier siehst du der Reihe nach, was alle auf Mildcrowd teilen – ohne Algorithmus."
                } else {
                    "Hier siehst du der Reihe nach, was deine Freund:innen und die Menschen und Crowds teilen, denen du folgst."
                },
                style = MildcrowdType.Body,
                color = MildcrowdColors.Ink2
            )
        }
    }
    block("view") {
        val navigator = LocalNavigator.current
        // The switch shows the chosen view at once; until the member chooses, what the server chose (members who follow
        // nobody start with Ganz Mildcrowd).
        val selection = state.filter ?: if (showAll) "alle" else "gefolgt"
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            SingleChoiceSegmentedButtonRow(Modifier.weight(1f).testTag("source.view")) {
                listOf("gefolgt" to "Gefolgt", "alle" to "Ganz Mildcrowd").forEachIndexed { index, (value, label) ->
                    SegmentedButton(
                        selected = selection == value,
                        onClick = { if (selection != value) onSelectView(value) },
                        shape = SegmentedButtonDefaults.itemShape(index, 2),
                        colors = SegmentedButtonDefaults.colors(
                            activeContainerColor = MildcrowdColors.Night,
                            activeContentColor = MildcrowdColors.OnNight,
                            inactiveContainerColor = MildcrowdColors.Paper,
                            inactiveContentColor = MildcrowdColors.Night,
                            activeBorderColor = MildcrowdColors.Night,
                            inactiveBorderColor = MildcrowdColors.DuneLine
                        ),
                        icon = {}
                    ) { Text(label, style = MildcrowdType.SmallBold) }
                }
            }
            MildcrowdButton(
                "Durchblättern",
                { navigator.open(NetworkRoute.Swipe) },
                kind = MildcrowdButtonKind.Accent,
                fullWidth = false,
                modifier = Modifier.testTag("source.swipe")
            )
        }
    }
    block("compose") {
        val navigator = LocalNavigator.current
        if (context?.canPost == false) {
            Panel(tone = PanelTone.Dune, spacing = 6) {
                Text("Gerade kannst du nichts veröffentlichen. Prüf deine Mitgliedschaft.", style = MildcrowdType.Body)
                UnderlinedLink("Mitglied werden", { navigator.open(MembershipRoute.Overview) })
            }
        } else {
            ComposeEntry(
                "Was möchtest du teilen?",
                Modifier.testTag("source.composeEntry"),
                accessibilityLabel = "Dein Post: Was möchtest du teilen?"
            ) {
                navigator.present(NetworkRoute.Compose())
            }
        }
    }
    extras.onSnoozeOnboarding?.let { onSnooze -> onboardingCard(onSnooze) }
    if (context?.followsAnyone == false) {
        block("steps") {
            Panel {
                SectionTitle("Nächste Schritte")
                StepLink("Entdecken, wer schon da ist", NetworkRoute.Discover)
                StepLink("Einer Crowd folgen", NetworkRoute.Crowds)
                StepLink("Profil vervollständigen", NetworkRoute.EditProfile)
                StepLink("Menschen einladen", NetworkRoute.Invitations)
            }
        }
    }
    if (extras.memories.isNotEmpty()) memoriesBlock(extras, handlers)
    feedSection(
        state,
        handlers,
        dimmed = state.isReloading,
        empty = if (cut.seen) {
            null
        } else if (showAll) {
            "Noch hat niemand etwas geteilt. Schreib den ersten Post!"
        } else {
            "Noch keine Posts. Folge Menschen oder Crowds, dann erscheinen ihre Posts hier – oder schau unter „Ganz Mildcrowd“, was alle teilen."
        }
    )
    if (cut.seen) {
        block("source.seen") {
            Panel(tone = PanelTone.Dune, spacing = 10, modifier = Modifier.testTag("source.seen")) {
                Text("Du hast alles Neue gesehen.", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                MildcrowdButton(
                    "Ältere Posts",
                    extras.onShowOlder,
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false,
                    modifier = Modifier.testTag("source.older")
                )
            }
        }
    }
    if (cut.seen || state.reachedEnd || state.posts.isEmpty()) sideBlocks(context, extras.meetups)
}

/**
 * „Mach deine Startseite lebendig“ (docs/mildcrowd-accounts.md, „Nach der Registrierung“): the welcome tour is no hurdle after
 * registering any more, this card offers it until it is done; „Später“ hides the card for 14 days on this device.
 */
private fun LazyListScope.onboardingCard(onSnooze: () -> Unit) {
    block("source.onboarding") {
        val navigator = LocalNavigator.current
        Panel(tone = PanelTone.Dune, spacing = 10, modifier = Modifier.testTag("source.onboarding")) {
            SectionTitle("Mach deine Startseite lebendig")
            Text(
                "Wähl ein paar Crowds und Menschen, dann füllt sich deine Startseite.",
                style = MildcrowdType.Body,
                color = MildcrowdColors.Ink2
            )
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                MildcrowdButton(
                    "Los geht’s",
                    { navigator.present(AccountRoute.Welcome) },
                    kind = MildcrowdButtonKind.Accent,
                    fullWidth = false,
                    modifier = Modifier.testTag("source.onboarding.start")
                )
                MildcrowdButton(
                    "Später",
                    onSnooze,
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false,
                    modifier = Modifier.testTag("source.onboarding.later")
                )
            }
        }
    }
}

/** „Erinnerungen“ („Nur für dich“): the member's own Posts of this day in earlier years, with „Ausblenden“. */
private fun LazyListScope.memoriesBlock(extras: SourceExtras, handlers: FeedHandlers) {
    block("source.memories") {
        Panel(tone = PanelTone.Dune, spacing = 12, modifier = Modifier.testTag("source.memories")) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("Nur für dich")
                SectionTitle("Erinnerungen")
            }
            extras.memories.forEach { year ->
                Text(year.label, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                year.posts.forEach { post -> PostCard(post, onAction = handlers.perform) }
            }
            AsyncMildcrowdButton(
                "Ausblenden",
                extras.onDismissMemories,
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("source.memories.dismiss")
            )
        }
    }
}

private fun LazyListScope.sideBlocks(context: SourceContext?, meetups: List<Meetup>) {
    context?.week?.let { week ->
        block("side.week") {
            val navigator = LocalNavigator.current
            Panel(tone = PanelTone.Night, spacing = 16) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Eyebrow("Nur für dich", onNight = true)
                    SectionTitle("Deine Woche", onNight = true)
                }
                Row(verticalAlignment = Alignment.Top) {
                    WeekNumber(week.reactions, "Reaktionen", Modifier.weight(1f))
                    WeekNumber(week.followers, "neu dabei", Modifier.weight(1f))
                    WeekNumber(week.karawane, "Follower", Modifier.weight(1f))
                }
                UnderlinedLink("Alle deine Zahlen", { navigator.open(NetworkRoute.Statistics) }, color = MildcrowdColors.OnNight2)
            }
        }
    }
    context?.crowds?.let { crowds ->
        block("side.crowds") {
            SideCard("Deine Crowds") {
                if (crowds.isEmpty()) Text("Du folgst noch keiner Crowd.")
                crowds.forEach { CrowdRow(it) }
                TextLink("Alle Crowds", NetworkRoute.Crowds)
            }
        }
    }
    if (meetups.isNotEmpty()) {
        block("side.meetups") {
            val navigator = LocalNavigator.current
            SideCard("Nächste Treffen", modifier = Modifier.testTag("source.meetups")) {
                meetups.take(3).forEach { meetup ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .pressable({ navigator.open(NetworkRoute.Crowd(meetup.crowd.slug, meetups = true)) })
                            .padding(vertical = 6.dp)
                    ) {
                        Text(meetup.title, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                        Text(
                            "${meetupWhen(meetup.startsAt)} · ${meetup.crowd.name}",
                            style = MildcrowdType.Small,
                            color = MildcrowdColors.Muted
                        )
                        meetup.myAnswer?.let {
                            Text("Deine Antwort: ${Meetup.answerLabel(it)}", style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
                        }
                    }
                }
            }
        }
    }
    block("side.invite") {
        val navigator = LocalNavigator.current
        SideCard("Gemeinsam ist schöner", tone = PanelTone.Dune) {
            Text("Lade Menschen ein, mit denen du gern redest. Ihr folgt euch dann automatisch.")
            MildcrowdButton("Einladen", {
                navigator.open(NetworkRoute.Invitations)
            }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
        }
    }
}

@Composable
private fun WeekNumber(value: Int, label: String, modifier: Modifier = Modifier) {
    Column(modifier.clearAndSetSemantics { contentDescription = "$value $label" }, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("$value", style = MildcrowdType.figure(), color = MildcrowdColors.OnNight)
        Text(label, style = MildcrowdType.Small, color = MildcrowdColors.OnNightMuted)
    }
}

@Composable
private fun StepLink(title: String, route: AppRoute) {
    val navigator = LocalNavigator.current
    Row(
        Modifier.fillMaxWidth().defaultMinSize(minHeight = 44.dp).clickable(role = Role.Button) { navigator.open(route) },
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.ArrowCircleRight, contentDescription = null, tint = MildcrowdColors.Night)
        Text(title, style = MildcrowdType.Body.copy(textDecoration = TextDecoration.Underline), color = MildcrowdColors.Night)
    }
}

/** "3. November". */
fun dayMonth(instant: Instant): String = GermanTime.format(instant, "d. MMMM")

/** The entry to the composer at the top of the Start and a crowd: a field-like button that opens the composer sheet. */
@Composable
fun ComposeEntry(prompt: String, modifier: Modifier = Modifier, accessibilityLabel: String? = null, onClick: () -> Unit) {
    val shape = RoundedCornerShape(MildcrowdRadius.M.dp)
    Row(
        modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 56.dp)
            .background(MildcrowdColors.Paper, shape)
            .border(1.dp, MildcrowdColors.PaperLine, shape)
            .pressable(onClick)
            // The tap comes from pressable(); TalkBack reads the label.
            .clearAndSetSemantics {
                contentDescription = accessibilityLabel ?: prompt
                role = Role.Button
            }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(prompt, style = MildcrowdType.Body, color = MildcrowdColors.Muted, modifier = Modifier.weight(1f))
        Icon(Icons.Outlined.EditNote, contentDescription = null, tint = MildcrowdColors.Night)
    }
}
