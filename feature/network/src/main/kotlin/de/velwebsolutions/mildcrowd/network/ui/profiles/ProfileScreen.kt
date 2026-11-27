package de.velwebsolutions.mildcrowd.network.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Favorite
import androidx.compose.material.icons.outlined.MoreHoriz
import androidx.compose.material.icons.outlined.People
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.ChatRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.NoContext
import de.velwebsolutions.mildcrowd.network.api.Profile
import de.velwebsolutions.mildcrowd.network.ui.PostListState
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.CrowdRow
import de.velwebsolutions.mildcrowd.network.ui.components.FeedHandlers
import de.velwebsolutions.mildcrowd.network.ui.components.MemberAvatar
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.OnNightButton
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.feedSection
import de.velwebsolutions.mildcrowd.network.ui.components.handlers
import de.velwebsolutions.mildcrowd.network.ui.components.share
import de.velwebsolutions.mildcrowd.network.ui.retry
import java.time.Instant

/** A profile action that asks first, with the web's wording. */
enum class ProfileConfirmation(val action: ProfileAction, val button: String, val message: String?) {
    Unfollow(ProfileAction.Unfollow, "Nicht mehr folgen", null),
    EndFriendship(ProfileAction.RemoveFriend, "Freundschaft beenden", null),
    Decline(ProfileAction.RemoveFriend, "Ablehnen", "Ablehnen sagt niemandem etwas."),
    Block(ProfileAction.Block, "Blockieren bestätigen", "Ihr verliert gegenseitige Follows.")
    ;

    fun title(name: String): String = when (this) {
        Unfollow -> "$name nicht mehr folgen?"
        EndFriendship -> "Freundschaft beenden?"
        Decline -> "Anfrage ablehnen?"
        Block -> "Wirklich blockieren?"
    }
}

/** What the profile's buttons do. */
class ProfileCallbacks(val perform: suspend (ProfileAction) -> Unit, val confirm: (ProfileConfirmation) -> Unit)

/**
 * A member profile (/profil/{handle}) – also the "Profil" tab for the own one: header with the banner or colour band,
 * avatar, the buttons (follow, friendship, write, mute, restrict, block, report), bio, location and Follower where their
 * audiences allow, the member's Posts and the side cards.
 */
@Composable
fun ProfileScreen(
    handle: String,
    viewModel: ProfileViewModel = hiltViewModel<ProfileViewModel, ProfileViewModel.Factory>(creationCallback = { it.create(handle) })
) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val posts by viewModel.posts.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val leave by viewModel.leave.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    var confirmation by remember { mutableStateOf<ProfileConfirmation?>(null) }
    val loaded = profile.valueOrNull

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    LaunchedEffect(leave) {
        // Like the web, blocking leads back to the Start: the profile no longer exists for the viewer.
        if (leave) {
            if (navigator.sheet != null) navigator.dismissSheet()
            navigator.popToRoot()
            navigator.selectedTab = MainTab.Source
        }
    }

    MildcrowdScreen(
        title = if (loaded?.isMe == true) "Profil" else loaded?.member?.displayName ?: "Profil",
        message = status ?: posts.status,
        error = (error ?: posts.actionError)?.userMessage,
        onDismissMessage = {
            viewModel.dismissStatus()
            viewModel.posts.dismissStatus()
        },
        onDismissError = {
            viewModel.dismissError()
            viewModel.posts.dismissError()
        },
        actions = {
            if (loaded != null) {
                IconButton(onClick = { share(context, loaded.url) }) { Icon(Icons.Outlined.Share, contentDescription = "Profil teilen") }
            }
        }
    ) {
        LoadableView(profile, onRetry = viewModel.retry(viewModel::load), skeleton = SkeletonLayout.Cards) { shown ->
            val handlers = viewModel.posts.handlers(viewModel::launch)
            NetworkPage(onRefresh = viewModel::load) {
                profileContent(shown, posts, handlers, ProfileCallbacks(viewModel::perform) { confirmation = it })
            }
        }
    }
    confirmation?.let { asked ->
        ConfirmDialog(
            title = asked.title(loaded?.member?.displayName.orEmpty()),
            message = asked.message,
            confirmLabel = asked.button,
            onConfirm = { viewModel.launch { viewModel.perform(asked.action) } },
            onDismiss = { confirmation = null }
        )
    }
}

/** The profile page's content. */
fun LazyListScope.profileContent(profile: Profile, posts: PostListState<NoContext>, handlers: FeedHandlers, callbacks: ProfileCallbacks) {
    block("header") { Header(profile, callbacks) }
    block("posts.title") { SectionTitle("Posts") }
    feedSection(posts, handlers, empty = "Noch keine Posts.")
    profile.crowds?.let { crowds ->
        block("side.crowds") {
            SideCard("Crowds") {
                if (crowds.isEmpty()) Text("Noch in keiner Crowd.")
                crowds.forEach { CrowdRow(it, detail = if (it.role == "host") "Gastgeber:in" else "Mitglied") }
            }
        }
    }
    if (profile.isMe) {
        block("side.own") {
            val navigator = LocalNavigator.current
            SideCard("Nur für dich", tone = PanelTone.Night) {
                Text("Wie deine Posts ankommen, siehst nur du.")
                ButtonFlow {
                    OnNightButton("Deine Zahlen", { navigator.open(NetworkRoute.Statistics) })
                    OnNightButton("Gemerkt", { navigator.open(NetworkRoute.Bookmarks) })
                    OnNightButton("Freund:innen", { navigator.open(NetworkRoute.Friends) })
                }
            }
        }
        block("side.visibility") {
            val navigator = LocalNavigator.current
            SideCard("Sichtbarkeit", tone = PanelTone.Dune) {
                Text("${profile.profileAudience.orEmpty()}.")
                UnderlinedLink("Privatsphäre-Einstellungen", { navigator.open(AccountRoute.Privacy) }, style = MildcrowdType.SmallBold)
            }
        }
    } else {
        block("side.noads") {
            SideCard("Ein Netzwerk ohne Werbung", tone = PanelTone.Dune) {
                Text("Auf Mildcrowd siehst du Posts der Reihe nach, ohne Empfehlungen, die dich festhalten sollen.")
            }
        }
    }
}

@Composable
private fun Header(profile: Profile, callbacks: ProfileCallbacks) {
    Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(MildcrowdRadius.M.dp)).background(MildcrowdColors.Paper)) {
        ProfileBanner(profile)
        Column(Modifier.padding(start = 24.dp, end = 24.dp, bottom = 24.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Box(Modifier.pullUp(44.dp).size(88.dp).border(4.dp, MildcrowdColors.Paper, CircleShape).padding(4.dp)) {
                MemberAvatar(profile.member, size = 80.dp)
            }
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text(
                        profile.member.displayName,
                        style = MildcrowdType.display(28),
                        color = MildcrowdColors.Night,
                        modifier = Modifier.semantics {
                            heading()
                        }
                    )
                    profile.status?.takeIf { it.isNotEmpty() }?.let { status ->
                        Text(
                            status,
                            style = MildcrowdType.Body.copy(fontWeight = FontWeight.SemiBold),
                            color = MildcrowdColors.Ink2,
                            modifier = Modifier.testTag("profile.status")
                        )
                    }
                    Text(
                        "@${profile.member.username} · dabei seit ${monthYear(profile.joinedAt)}",
                        style = MildcrowdType.Body,
                        color = MildcrowdColors.Muted
                    )
                    if (profile.supporter) {
                        Pill(
                            "Trägt Mildcrowd mit",
                            Modifier.padding(top = 4.dp).testTag("profile.supporter"),
                            PillTone.Spring,
                            Icons.Outlined.Favorite
                        )
                    }
                    if (profile.foundingBadge) {
                        // The founding mark, switched on in the privacy centre (founding_badge), next to the supporter mark.
                        Pill(
                            "Gründungsmitglied",
                            Modifier.padding(top = 4.dp).testTag("profile.founding"),
                            PillTone.Dune,
                            Icons.Outlined.WbSunny
                        )
                    }
                }
                profile.bio?.let { Text(it, style = MildcrowdType.Body, color = MildcrowdColors.Ink) }
                if (profile.bioHeld) HeldBadge()
                Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                    profile.location?.let { IconLabel(Icons.Outlined.Place, it) }
                    profile.followers?.let { IconLabel(Icons.Outlined.People, "Follower: $it", Modifier.testTag("profile.followers")) }
                }
                Buttons(profile, callbacks)
            }
        }
    }
}

@Composable
private fun IconLabel(icon: ImageVector, text: String, modifier: Modifier = Modifier) {
    Row(modifier, horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MildcrowdColors.Ink2, modifier = Modifier.size(20.dp))
        Text(text, style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
    }
}

@Composable
private fun Buttons(profile: Profile, callbacks: ProfileCallbacks) {
    val navigator = LocalNavigator.current
    if (profile.isMe) {
        MildcrowdButton(
            "Profil bearbeiten",
            { navigator.open(NetworkRoute.EditProfile) },
            kind = MildcrowdButtonKind.Secondary,
            fullWidth = false,
            modifier = Modifier.testTag("profile.edit")
        )
        return
    }
    ButtonFlow {
        if (profile.following) {
            MildcrowdButton(
                "Gefolgt ✓",
                { callbacks.confirm(ProfileConfirmation.Unfollow) },
                fullWidth = false,
                modifier = Modifier.semantics { contentDescription = "Gefolgt ✓, Nicht mehr folgen" }.testTag("profile.unfollow")
            )
        } else if (profile.canFollow) {
            AsyncMildcrowdButton(
                "Folgen",
                { callbacks.perform(ProfileAction.Follow) },
                kind = MildcrowdButtonKind.Accent,
                fullWidth = false,
                modifier = Modifier.testTag("profile.follow")
            )
        }
        when (profile.friendship) {
            "friends" -> MildcrowdButton(
                "Befreundet ✓",
                { callbacks.confirm(ProfileConfirmation.EndFriendship) },
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.semantics { contentDescription = "Befreundet ✓, Freundschaft beenden" }
            )

            "incoming" -> {
                AsyncMildcrowdButton("Freundschaft annehmen", { callbacks.perform(ProfileAction.AcceptFriend) }, fullWidth = false)
                MildcrowdButton("Ablehnen", {
                    callbacks.confirm(ProfileConfirmation.Decline)
                }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            }

            "outgoing" -> AsyncMildcrowdButton(
                "Anfrage zurückziehen",
                { callbacks.perform(ProfileAction.RemoveFriend) },
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("profile.withdrawFriend")
            )

            else -> if (profile.acceptsFriendRequests) {
                AsyncMildcrowdButton(
                    "Freundschaft anfragen",
                    { callbacks.perform(ProfileAction.RequestFriend) },
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false,
                    modifier = Modifier.testTag("profile.requestFriend")
                )
            }
        }
        if (profile.canWrite) {
            MildcrowdButton(
                "Schreiben",
                { navigator.open(ChatRoute.NewConversationWith(profile.member.username)) },
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("profile.write")
            )
        }
        MoreMenu(profile, callbacks)
    }
    profile.chatHint?.let {
        Text(it, style = MildcrowdType.Small, color = MildcrowdColors.Muted, modifier = Modifier.testTag("profile.chatHint"))
    }
}

@Composable
private fun MoreMenu(profile: Profile, callbacks: ProfileCallbacks) {
    val navigator = LocalNavigator.current
    var open by remember { mutableStateOf(false) }
    var pending by remember { mutableStateOf<ProfileAction?>(null) }
    LaunchedEffect(pending) {
        pending?.let { callbacks.perform(it) }
        pending = null
    }
    Box {
        MildcrowdButton(
            "Mehr",
            { open = true },
            kind = MildcrowdButtonKind.Secondary,
            fullWidth = false,
            leading = {
                Icon(Icons.Outlined.MoreHoriz, contentDescription = null, tint = MildcrowdColors.Night, modifier = Modifier.size(18.dp))
            },
            modifier = Modifier.semantics {
                contentDescription = "Weitere Aktionen zu ${profile.member.displayName}"
            }.testTag("profile.menu")
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }, containerColor = MildcrowdColors.Paper) {
            @Composable
            fun item(title: String, destructive: Boolean = false, action: () -> Unit) {
                DropdownMenuItem(
                    text = {
                        Text(title, style = MildcrowdType.Body, color = if (destructive) MildcrowdColors.Danger else MildcrowdColors.Ink)
                    },
                    onClick = {
                        open = false
                        action()
                    }
                )
            }
            item(if (profile.hasMuted) "Nicht mehr stumm" else "Stummschalten") {
                pending =
                    if (profile.hasMuted) ProfileAction.Unmute else ProfileAction.Mute
            }
            item(if (profile.hasRestricted) "Nicht mehr einschränken" else "Einschränken") {
                pending = if (profile.hasRestricted) ProfileAction.Unrestrict else ProfileAction.Restrict
            }
            item("Blockieren …", destructive = true) { callbacks.confirm(ProfileConfirmation.Block) }
            item("Melden") { navigator.open(AccountRoute.Report(profile.url)) }
        }
    }
}

/** Moves content up by [amount] and takes that much less room (the avatar overlapping the banner). */
private fun Modifier.pullUp(amount: Dp): Modifier = layout { measurable, constraints ->
    val placeable = measurable.measure(constraints)
    val shift = amount.roundToPx()
    layout(placeable.width, maxOf(0, placeable.height - shift)) { placeable.place(0, -shift) }
}

/** "Mai 2026". */
fun monthYear(instant: Instant): String = GermanTime.format(instant, "MMMM yyyy")

/** "Wird geprüft" on a held profile text (own profile and its form). */
@Composable
fun HeldBadge() {
    Text(
        "Wird geprüft: Deine Beschreibung sehen andere erst, wenn das Moderationsteam sie angesehen hat.",
        style = MildcrowdType.SmallBold,
        color = MildcrowdColors.SunInk,
        modifier = Modifier.background(MildcrowdColors.Sun, RoundedCornerShape(MildcrowdRadius.CHIP.dp)).padding(10.dp)
    )
}

/** The top of a profile: the 3:1 banner, or the strip in the avatar colour (also while the banner loads). */
@Composable
fun ProfileBanner(profile: Profile) {
    val banner = profile.bannerUrl
    val color = AvatarTint.background(profile.member.avatarColor)
    if (banner != null) {
        Box(Modifier.fillMaxWidth().aspectRatio(3f).background(color).clearAndSetSemantics {}) {
            AuthorizedImage(banner, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        }
    } else {
        Box(Modifier.fillMaxWidth().height(112.dp).background(color).clearAndSetSemantics {})
    }
}
