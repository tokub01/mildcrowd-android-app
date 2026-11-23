package de.velwebsolutions.mildcrowd.network.ui.crowds

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.ChatRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.CrowdDetail
import de.velwebsolutions.mildcrowd.network.api.CrowdMembers
import de.velwebsolutions.mildcrowd.network.api.NoContext
import de.velwebsolutions.mildcrowd.network.ui.PostListState
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ComposeFab
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.Disclosure
import de.velwebsolutions.mildcrowd.network.ui.components.FabInset
import de.velwebsolutions.mildcrowd.network.ui.components.FeedHandlers
import de.velwebsolutions.mildcrowd.network.ui.components.MemberRow
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.OnNightButton
import de.velwebsolutions.mildcrowd.network.ui.components.OnNightKind
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.feedSection
import de.velwebsolutions.mildcrowd.network.ui.components.handlers
import de.velwebsolutions.mildcrowd.network.ui.components.membersLabel
import de.velwebsolutions.mildcrowd.network.ui.feeds.ComposeEntry
import de.velwebsolutions.mildcrowd.network.ui.retry
import kotlinx.coroutines.delay

/**
 * One crowd (/crowds/{slug}): banner, description, following, house rules, the composer for members, the Posts, hosts,
 * the member list for members and the host tools.
 */
@Composable
fun CrowdDetailScreen(
    slug: String,
    viewModel: CrowdViewModel = hiltViewModel<CrowdViewModel, CrowdViewModel.Factory>(creationCallback = { it.create(slug) }),
    /** Opened from a meetup hint (`#treffen`): the page scrolls to „Treffen“ once they are there. */
    atMeetups: Boolean = false,
    /** Opened from „In Sammlung: …“: that collection is chosen. */
    atCollection: Int? = null
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val posts by viewModel.posts.state.collectAsStateWithLifecycle()
    val members by viewModel.members.collectAsStateWithLifecycle()
    val memberQuery by viewModel.memberQuery.collectAsStateWithLifecycle()
    val meetups by viewModel.meetups.collectAsStateWithLifecycle()
    val meetupForm by viewModel.meetupForm.collectAsStateWithLifecycle()
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val shownCollection by viewModel.shownCollection.collectAsStateWithLifecycle()
    val shownCollectionId by viewModel.shownCollectionId.collectAsStateWithLifecycle()
    val collectionForm by viewModel.collectionForm.collectAsStateWithLifecycle()
    val shownPosts = shownCollection?.state?.collectAsStateWithLifecycle()?.value
    val status by viewModel.status.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    var confirmUnfollow by remember { mutableStateOf(false) }
    val loaded = detail.valueOrNull
    val compose = { crowd: CrowdDetail -> navigator.present(NetworkRoute.Compose(crowdSlug = crowd.crowd.slug)) }
    val listState = rememberLazyListState()

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    LaunchedEffect(atCollection) { atCollection?.let { viewModel.chooseCollection(it) } }
    // Opened from a meetup hint: once the meetups are there, the page shows them.
    val meetupsRow = if (atMeetups && meetups != null) loaded?.let(::meetupsIndex) else null
    LaunchedEffect(meetupsRow) { meetupsRow?.let { listState.animateScrollToItem(it) } }
    LaunchedEffect(memberQuery) {
        if (viewModel.members.value != null) {
            delay(300)
            viewModel.loadMembers()
        }
    }

    MildcrowdScreen(
        title = loaded?.crowd?.name ?: "Crowd",
        message = status ?: posts.status,
        error = (error ?: posts.actionError)?.userMessage,
        onDismissMessage = {
            viewModel.dismissStatus()
            viewModel.posts.dismissStatus()
        },
        onDismissError = {
            viewModel.dismissError()
            viewModel.posts.dismissError()
        }
    ) {
        Box(Modifier.fillMaxSize()) {
            LoadableView(detail, onRetry = viewModel.retry(viewModel::load), skeleton = SkeletonLayout.Cards) { crowd ->
                val extras = CrowdMeetupsUi(meetups, meetupForm, viewModel.meetupActions())
                val collectionsUi = CrowdCollectionsUi(
                    collections,
                    shownCollectionId,
                    shownPosts,
                    shownCollection?.handlers(viewModel::launch),
                    collectionForm,
                    viewModel.collectionActions()
                )
                CrowdPage(crowd, listState, viewModel, posts, members, memberQuery, extras, collectionsUi, onUnfollow = {
                    confirmUnfollow =
                        true
                }) {
                    compose(crowd)
                }
            }
            // iOS: the pen in the toolbar; on Android the crowd's main action is the floating button.
            if (loaded?.canPost == true) {
                ComposeFab(
                    listState,
                    onClick = { compose(loaded) },
                    Modifier.testTag("crowd.compose"),
                    description = "Post in ${loaded.crowd.name} schreiben"
                )
            }
        }
    }
    if (confirmUnfollow) {
        ConfirmDialog(
            title = "Der Crowd „${loaded?.crowd?.name.orEmpty()}“ nicht mehr folgen?",
            confirmLabel = "Nicht mehr folgen",
            onConfirm = { viewModel.launch { viewModel.setFollowing(false) } },
            onDismiss = { confirmUnfollow = false }
        )
    }
}

@Composable
private fun CrowdPage(
    crowd: CrowdDetail,
    listState: LazyListState,
    viewModel: CrowdViewModel,
    posts: PostListState<NoContext>,
    members: CrowdMembers?,
    memberQuery: String,
    extras: CrowdMeetupsUi,
    collections: CrowdCollectionsUi,
    onUnfollow: () -> Unit,
    onCompose: () -> Unit
) {
    val handlers = viewModel.posts.handlers(viewModel::launch)
    NetworkPage(state = listState, onRefresh = viewModel::load, bottomInset = if (crowd.canPost) FabInset else 0.dp) {
        crowdContent(
            detail = crowd,
            posts = posts,
            handlers = handlers,
            members = members,
            memberQuery = memberQuery,
            onMemberQuery = viewModel::updateMemberQuery,
            onFollow = { viewModel.setFollowing(true) },
            onUnfollow = onUnfollow,
            onCompose = onCompose,
            extras = extras,
            collections = collections
        )
    }
}

/** The view model's meetup and Willkommenspaten steps for the page. */
internal fun CrowdViewModel.meetupActions() = CrowdMeetupActions(
    onOpenForm = { openMeetupForm(it) },
    onEditForm = ::editMeetupForm,
    onCloseForm = ::closeMeetupForm,
    onSave = ::saveMeetup,
    onAnswer = { meetup, answer -> launch { answerMeetup(meetup, answer) } },
    onCancel = { meetup -> launch { cancelMeetup(meetup) } },
    onAllowMembers = { allowed -> launch { setMembersCreateMeetups(allowed) } },
    onVolunteer = { volunteer -> launch { setWelcomeVolunteer(volunteer) } }
)

/** The view model's collection steps for the page. */
internal fun CrowdViewModel.collectionActions() = CrowdCollectionActions(
    onChoose = { id -> launch { chooseCollection(id) } },
    onOpenForm = ::openCollectionForm,
    onEditForm = ::editCollectionForm,
    onCloseForm = ::closeCollectionForm,
    onSave = ::saveCollection,
    onDelete = { collection -> launch { deleteCollection(collection) } },
    onAllowMembers = { allowed -> launch { setMembersCreateCollections(allowed) } }
)

/** The row of „Treffen“ on the page: after the header, the pate, the house rules and the composer entry. */
internal fun meetupsIndex(detail: CrowdDetail): Int = 2 + (if (detail.welcome?.pate != null) 1 else 0) + (if (detail.canPost) 1 else 0)

/** The crowd page's content. */
fun LazyListScope.crowdContent(
    detail: CrowdDetail,
    posts: PostListState<NoContext>,
    handlers: FeedHandlers,
    members: CrowdMembers?,
    memberQuery: String,
    onMemberQuery: (String) -> Unit,
    onFollow: suspend () -> Unit,
    onUnfollow: () -> Unit,
    onCompose: () -> Unit,
    extras: CrowdMeetupsUi = CrowdMeetupsUi.None,
    collections: CrowdCollectionsUi = CrowdCollectionsUi.None
) {
    block("header") { Header(detail, onFollow, onUnfollow) }
    detail.welcome?.pate?.let { pate -> block("pate") { PateCard(pate) } }
    block("rules") {
        var showRules by rememberSaveable { mutableStateOf(false) }
        Disclosure("Hausregeln dieser Crowd", showRules, {
            showRules = !showRules
        }, Modifier.testTag("crowd.rules"), border = MildcrowdColors.DuneLine) {
            HouseRules(detail.houseRules)
        }
    }
    if (detail.canPost) block("compose") { ComposeEntry("Dein Post in ${detail.crowd.name}", onClick = onCompose) }
    // Only members get meetups and Willkommenspaten from the server.
    extras.meetups?.let { meetups ->
        block("meetups") { MeetupsSection(meetups, extras.form, detail.isHost, extras.actions) }
    }
    collectionsSection(collections, detail.isHost)
    block("posts.title") { SectionTitle("Posts") }
    feedSection(posts, handlers, empty = "In dieser Crowd gibt es noch keine Posts.")
    block("side.hosts") { SideCard("Gastgeber:innen") { detail.hosts.forEach { MemberRow(it) } } }
    if (detail.isMember && members != null) block("side.members") { MemberListCard(members, memberQuery, onMemberQuery) }
    detail.welcome?.let { welcome -> block("side.welcome") { VolunteerCard(welcome, extras.actions.onVolunteer) } }
    if (detail.isHost) {
        block("side.host") {
            val navigator = LocalNavigator.current
            SideCard("Du bist Gastgeber:in", tone = PanelTone.Night) {
                Text("Bearbeite Texte und Rollen, oder sieh nach, wie es der Crowd geht.")
                ButtonFlow {
                    OnNightButton("Bearbeiten", {
                        navigator.open(NetworkRoute.EditCrowd(detail.crowd.slug))
                    }, Modifier.testTag("crowd.edit"))
                    OnNightButton(
                        "Statistik",
                        { navigator.open(NetworkRoute.CrowdStatistics(detail.crowd.slug)) },
                        Modifier.testTag("crowd.statistics"),
                        kind = OnNightKind.Outline
                    )
                }
            }
        }
    }
}

@Composable
private fun Header(detail: CrowdDetail, onFollow: suspend () -> Unit, onUnfollow: () -> Unit) {
    val navigator = LocalNavigator.current
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(MildcrowdRadius.M.dp)).background(MildcrowdColors.Paper)) {
        CrowdBanner(detail.crowd, height = if (detail.crowd.bannerUrl == null) 128.dp else 120.dp)
        Column(Modifier.padding(24.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Eyebrow("Crowd")
                Text(
                    detail.crowd.name,
                    style = MildcrowdType.display(28),
                    color = MildcrowdColors.Night,
                    modifier = Modifier.semantics {
                        heading()
                    }
                )
            }
            Text(detail.crowd.description, style = MildcrowdType.Body, color = MildcrowdColors.Ink)
            if (detail.crowd.showsHeldTexts) CrowdHeldNote()
            detail.crowd.region?.let { region ->
                Row(
                    Modifier
                        .clickable(role = Role.Button) { navigator.open(NetworkRoute.CrowdMap(region.code)) }
                        .heightIn(min = 44.dp)
                        .testTag("crowd.region"),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.Place, contentDescription = null, tint = MildcrowdColors.Ink2, modifier = Modifier.size(20.dp))
                    Text(detail.crowd.place.orEmpty(), style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.People, contentDescription = null, tint = MildcrowdColors.Ink2, modifier = Modifier.size(20.dp))
                Text(membersLabel(detail.crowd.membersCount ?: 0), style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                if (detail.isMember) {
                    MildcrowdButton(
                        "Gefolgt ✓",
                        onUnfollow,
                        fullWidth = false,
                        modifier = Modifier.semantics { contentDescription = "Gefolgt ✓, Nicht mehr folgen" }.testTag("crowd.unfollow")
                    )
                } else if (detail.canFollow) {
                    AsyncMildcrowdButton(
                        "Folgen",
                        onFollow,
                        kind = MildcrowdButtonKind.Accent,
                        fullWidth = false,
                        modifier = Modifier.testTag("crowd.follow")
                    )
                }
                if (!detail.isHost) {
                    UnderlinedLink("Melden", {
                        navigator.open(AccountRoute.Report(detail.url))
                    }, style = MildcrowdType.Small, color = MildcrowdColors.SpringText)
                }
            }
        }
    }
}

/** House rules one per line plus the hint to the Community-Regeln (`partials/house-rules`). */
@Composable
fun HouseRules(rules: List<String>) {
    Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (rules.size > 1) {
            Bullets(rules)
        } else {
            rules.firstOrNull()?.let { Text(it, style = MildcrowdType.Body, color = MildcrowdColors.Ink2) }
        }
        Text("Zusätzlich gelten die Community-Regeln von Mildcrowd.", style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
    }
}

/** The member list for members of the crowd, with "Schreiben" where the person accepts messages. */
@Composable
private fun MemberListCard(members: CrowdMembers, query: String, onQuery: (String) -> Unit) {
    val navigator = LocalNavigator.current
    SideCard("Mitglieder") {
        val shape = RoundedCornerShape(MildcrowdRadius.XS.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .background(MildcrowdColors.Field, shape)
                .border(1.dp, MildcrowdColors.FieldLine, shape)
                .padding(horizontal = 14.dp, vertical = 10.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            if (query.isEmpty()) Text("Namen suchen …", style = MildcrowdType.Body, color = MildcrowdColors.Muted)
            BasicTextField(
                query,
                onQuery,
                singleLine = true,
                textStyle = MildcrowdType.Body.copy(color = MildcrowdColors.Ink),
                cursorBrush = SolidColor(MildcrowdColors.Night),
                modifier = Modifier.fillMaxWidth().semantics { contentDescription = "Mitglieder nach Namen suchen" }
            )
        }
        if (members.members.isEmpty()) {
            Text(if (query.isBlank()) "Außer dir ist hier noch niemand." else "Niemand mit diesem Namen.", color = MildcrowdColors.Muted)
        }
        members.members.forEach { entry ->
            MemberRow(entry.member, size = 36.dp) {
                if (entry.canWrite) {
                    MildcrowdButton(
                        "Schreiben",
                        { navigator.open(ChatRoute.NewConversationWith(entry.username)) },
                        kind = MildcrowdButtonKind.Secondary,
                        fullWidth = false,
                        modifier = Modifier.semantics { contentDescription = "${entry.displayName} schreiben" }
                    )
                }
            }
        }
        if (members.hiddenMembers > 0) {
            val who = if (members.hiddenMembers ==
                1
            ) {
                "Ein weiteres Mitglied zeigt sich"
            } else {
                "${members.hiddenMembers} weitere Mitglieder zeigen sich"
            }
            Text("$who hier nur Freund:innen und Menschen, denen sie folgen.")
        }
        Text(
            "„Schreiben“ erscheint, wenn die Person den Chat eingerichtet hat und Nachrichten von dir annimmt.",
            style = MildcrowdType.Caption,
            color = MildcrowdColors.Muted
        )
        members.ownListAudience?.let { audience ->
            Column(
                Modifier.fillMaxWidth().background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.CHIP.dp)).padding(10.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    if (audience == "nobody") {
                        "Du bist in Mitgliederlisten für niemanden sichtbar."
                    } else {
                        "Zu deinem eigenen Schutz ist aktuell eingestellt, dass dich in dieser Liste nur Freund:innen und Menschen sehen, denen du folgst."
                    },
                    style = MildcrowdType.Caption,
                    color = MildcrowdColors.Ink
                )
                UnderlinedLink("Umstellen", { navigator.open(AccountRoute.Privacy) }, style = MildcrowdType.Caption)
            }
        }
    }
}

/**
 * „Zurückgehalten und dem Moderationsteam gemeldet“ for the founder, the hosts and staff of a crowd whose texts the
 * automated check holds (crowd-show.blade.php; docs/mildcrowd-auto-moderation.md): they still read name, description and
 * house rules, everyone else sees „Crowd in Prüfung“ until a person decided. Like the note on a held Post.
 */
@Composable
fun CrowdHeldNote() {
    val shape = RoundedCornerShape(MildcrowdRadius.TILE.dp)
    Column(
        Modifier
            .fillMaxWidth()
            .background(MildcrowdColors.Dune, shape)
            .border(1.dp, MildcrowdColors.Sun.copy(alpha = 0.4f), shape)
            .padding(16.dp)
            .semantics(mergeDescendants = true) {}
            .testTag("crowd.held"),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Text("Zurückgehalten und dem Moderationsteam gemeldet", style = MildcrowdType.SmallBold, color = MildcrowdColors.Night)
        Text(
            "Name, Beschreibung und Hausregeln prüft gerade ein Mensch aus dem Moderationsteam. Andere sehen so lange einen Platzhalter.",
            style = MildcrowdType.Small,
            color = MildcrowdColors.Ink
        )
        Text(
            "Ist alles in Ordnung, sind die Texte wieder für alle sichtbar. Du kannst sie jederzeit ändern.",
            style = MildcrowdType.Small,
            color = MildcrowdColors.Ink2
        )
    }
}
