package de.velwebsolutions.mildcrowd.network.ui.posts

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Share
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.NetworkSignals
import de.velwebsolutions.mildcrowd.network.api.Post
import de.velwebsolutions.mildcrowd.network.api.PostDetail
import de.velwebsolutions.mildcrowd.network.api.PostEditForm
import de.velwebsolutions.mildcrowd.network.ui.PostAction
import de.velwebsolutions.mildcrowd.network.ui.components.CarriedOriginal
import de.velwebsolutions.mildcrowd.network.ui.components.CollectionActions
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.CrowdDot
import de.velwebsolutions.mildcrowd.network.ui.components.Disclosure
import de.velwebsolutions.mildcrowd.network.ui.components.LeaveGuard
import de.velwebsolutions.mildcrowd.network.ui.components.LocalCollectionActions
import de.velwebsolutions.mildcrowd.network.ui.components.LocalPollActions
import de.velwebsolutions.mildcrowd.network.ui.components.MemberRow
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.PaperBox
import de.velwebsolutions.mildcrowd.network.ui.components.PollActions
import de.velwebsolutions.mildcrowd.network.ui.components.PostCard
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.TextLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.fullDate
import de.velwebsolutions.mildcrowd.network.ui.components.share
import de.velwebsolutions.mildcrowd.network.ui.compose.ComposeUiState
import de.velwebsolutions.mildcrowd.network.ui.compose.ComposeViewModel
import de.velwebsolutions.mildcrowd.network.ui.compose.ComposerCallbacks
import de.velwebsolutions.mildcrowd.network.ui.compose.ComposerForm
import de.velwebsolutions.mildcrowd.network.ui.compose.callbacks
import de.velwebsolutions.mildcrowd.network.ui.retry

/**
 * The composer as a sheet: a new Post in the Start or a crowd, or a (quick) reply. While something is written or
 * picked, "Schließen" and the back gesture ask before discarding the draft; the sheet closes itself after publishing.
 */
@Composable
fun ComposeScreen(
    crowdSlug: String?,
    replyTo: Int?,
    viewModel: ComposeViewModel = hiltViewModel<ComposeViewModel, ComposeViewModel.Factory>(
        key = "compose:$crowdSlug:$replyTo",
        creationCallback = { it.create(crowdSlug, replyTo, null) }
    )
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    var confirmDiscard by remember { mutableStateOf(false) }
    val close: () -> Unit = { if (navigator.sheet != null) navigator.dismissSheet() else navigator.back() }

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }

    // A draft keeps the sheet from being swiped away; "Schließen" and back ask first.
    DisposableEffect(state.hasDraft) {
        navigator.sheetDismissible = !state.hasDraft
        onDispose { navigator.sheetDismissible = true }
    }

    LeaveGuard(enabled = state.hasDraft, onAttempt = { confirmDiscard = true }) {
        ComposePage(state, viewModel, onClose = { if (state.hasDraft) confirmDiscard = true else close() }, onPublished = close)
    }
    if (confirmDiscard) {
        ConfirmDialog(
            title = "Entwurf verwerfen?",
            message = "Dein Text und deine Bilder gehen verloren.",
            confirmLabel = "Verwerfen",
            onConfirm = {
                viewModel.discard()
                close()
            },
            onDismiss = { confirmDiscard = false }
        )
    }
}

@Composable
private fun ComposePage(state: ComposeUiState, viewModel: ComposeViewModel, onClose: () -> Unit, onPublished: () -> Unit) {
    val navigator = LocalNavigator.current
    MildcrowdScreen(
        title = if (state.isReply) "Antworten" else "Post schreiben",
        closesItself = true,
        navigationAction = {
            if (navigator.sheet != null) {
                IconButton(
                    onClick = onClose,
                    modifier = Modifier.testTag("compose.close")
                ) {
                    Icon(Icons.Outlined.Close, contentDescription = "Schließen", tint = MildcrowdColors.Night)
                }
            }
        }
    ) {
        LoadableView(state.context, onRetry = viewModel.retry(viewModel::load)) {
            ComposeContent(state, viewModel.callbacks(), onPublished = { onPublished() })
        }
    }
}

/** The composer page: the form on paper, with the text field focused. */
@Composable
fun ComposeContent(state: ComposeUiState, callbacks: ComposerCallbacks, onPublished: () -> Unit) {
    var focus by rememberSaveable { mutableStateOf(true) }
    NetworkPage {
        block {
            PaperBox {
                ComposerForm(state, callbacks, focusRequest = focus, onFocusHandled = { focus = false }, onPublished = { onPublished() })
            }
        }
    }
}

/**
 * "Teilen" (/posts/{id}/teilen): the original above a composer for own words; links need "Ich habe den
 * Inhalt gelesen". Afterwards the Start shows the new Post.
 */
@Composable
fun CarryScreen(
    id: Int,
    viewModel: CarryViewModel = hiltViewModel<CarryViewModel, CarryViewModel.Factory>(creationCallback = { it.create(id) }),
    composer: ComposeViewModel =
        hiltViewModel<ComposeViewModel, ComposeViewModel.Factory>(key = "carry:$id", creationCallback = { it.create(null, null, id) }),
    /** From the card's `can_quote`; the carry page's own answer decides as well. */
    quote: Boolean = true
) {
    val source by viewModel.source.collectAsStateWithLifecycle()
    val state by composer.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) {
        viewModel.load()
        composer.loadIfNeeded()
    }
    MildcrowdScreen(title = "Teilen") {
        LoadableView(
            source,
            onRetry = viewModel.retry {
                viewModel.load()
                composer.loadIfNeeded()
            }
        ) { original ->
            val plain = !quote || !original.canQuote
            LaunchedEffect(plain) { if (plain) composer.carryPlainly() }
            NetworkPage {
                block {
                    PageHeader(
                        "Teilen",
                        lead = if (plain) PLAIN_CARRY_LEAD else QUOTE_CARRY_LEAD
                    )
                }
                block { CarriedOriginal(original.author, original.segments) }
                if (plain) {
                    block {
                        Text(plainCarryNote(original.author.displayName), style = MildcrowdType.Small, color = MildcrowdColors.Muted)
                    }
                }
                block {
                    PaperBox {
                        ComposerForm(state, composer.callbacks(), onPublished = {
                            navigator.popToRoot(MainTab.Source)
                            navigator.selectedTab = MainTab.Source
                        })
                    }
                }
            }
        }
    }
}

/** The lead of the carry page when own words are allowed (empty is a plain carry). */
const val QUOTE_CARRY_LEAD = "Magst du ein paar eigene Worte dazuschreiben? Warum ist das wichtig?"

/** The lead of the carry page when own words would be an answer the member may not give. */
const val PLAIN_CARRY_LEAD = "Teile den Post mit den Menschen, die dir folgen."

/** Why the carry page has no text field. */
fun plainCarryNote(author: String): String = "$author lässt nicht alle antworten. Du kannst den Post deshalb nur ohne eigene Worte teilen."

/**
 * "Post bearbeiten" (/posts/{id}/bearbeiten): the text within the edit window; images and audience stay. A changed
 * text is only left through "Änderung speichern" or "Abbrechen", which asks first; a rotation keeps it.
 */
@Composable
fun EditPostScreen(
    id: Int,
    viewModel: EditPostViewModel = hiltViewModel<EditPostViewModel, EditPostViewModel.Factory>(creationCallback = { it.create(id) })
) {
    val form by viewModel.form.collectAsStateWithLifecycle()
    val text by viewModel.text.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val saved by viewModel.saved.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    var confirmDiscard by remember { mutableStateOf(false) }
    val changed = form.valueOrNull?.let { it.body != text } ?: false

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    LaunchedEffect(saved) { if (saved) navigator.back() }

    LeaveGuard(enabled = changed, onAttempt = { confirmDiscard = true }) {
        EditPostPage(viewModel, form, text, error, onCancel = { if (changed) confirmDiscard = true else navigator.back() })
    }
    if (confirmDiscard) {
        ConfirmDialog("Änderung verwerfen?", "Verwerfen", onConfirm = { navigator.back() }, onDismiss = { confirmDiscard = false })
    }
}

@Composable
private fun EditPostPage(viewModel: EditPostViewModel, form: Loadable<PostEditForm>, text: String, error: ApiError?, onCancel: () -> Unit) {
    MildcrowdScreen(title = "Bearbeiten") {
        LoadableView(form, onRetry = viewModel.retry(viewModel::load)) { loaded ->
            NetworkPage {
                block {
                    PageHeader(
                        "Post bearbeiten",
                        eyebrow = "Post",
                        lead = "Du kannst den Text bis ${GermanTime.format(
                            loaded.until,
                            "HH:mm"
                        )} Uhr ändern. Bilder und Sichtbarkeit bleiben, wie sie sind."
                    )
                }
                block {
                    PaperBox {
                        val failure = error
                        if (failure != null && failure.message("body") == null) Banner(failure.userMessage)
                        MildcrowdTextField(
                            "Dein Post",
                            text,
                            viewModel::updateText,
                            error = failure?.message("body"),
                            lines = 6..12,
                            modifier = Modifier.testTag("edit.body")
                        )
                        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                            AsyncMildcrowdButton(
                                "Änderung speichern",
                                viewModel::save,
                                fullWidth = false,
                                modifier = Modifier.testTag("edit.save")
                            )
                            MildcrowdButton(
                                "Abbrechen",
                                onCancel,
                                kind = MildcrowdButtonKind.Secondary,
                                fullWidth = false
                            )
                        }
                    }
                }
                block {
                    SideCard("Offen für alle", tone = PanelTone.Dune) {
                        Text(
                            "Wer deinen Post lesen darf, sieht, dass er bearbeitet wurde, und kann frühere Fassungen aufklappen. So " +
                                "bleibt nachvollziehbar, worauf andere geantwortet haben."
                        )
                    }
                }
            }
        }
    }
}

/**
 * One Post with its replies (/posts/{id}): earlier versions ("Frühere Fassungen"), the reply form where the author
 * allows replies, the replies oldest first, and the side cards "Geschrieben von", "In der Crowd", "Leise Zahlen". Opened
 * with "Antworten" from a card, the reply field is focused (the web jumps to #antworten).
 */
@Composable
fun PostDetailScreen(
    id: Int,
    viewModel: PostDetailViewModel = hiltViewModel<PostDetailViewModel, PostDetailViewModel.Factory>(creationCallback = { it.create(id) }),
    reply: ComposeViewModel =
        hiltViewModel<ComposeViewModel, ComposeViewModel.Factory>(key = "reply:$id", creationCallback = { it.create(null, id, null) })
) {
    val detail by viewModel.detail.collectAsStateWithLifecycle()
    val replyState by reply.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val leave by viewModel.leave.collectAsStateWithLifecycle()
    val replyFocus by NetworkSignals.replyFocus.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    var focusReply by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(replyFocus) {
        if (replyFocus == id) {
            NetworkSignals.replyFocus.value = null
            focusReply = true
        }
    }
    LaunchedEffect(leave) { if (leave) navigator.back() }

    MildcrowdScreen(
        title = "Post",
        message = status,
        error = error?.userMessage,
        onDismissMessage = viewModel::dismissStatus,
        onDismissError = viewModel::dismissError,
        actions = {
            detail.valueOrNull?.post?.let { post ->
                IconButton(onClick = { share(context, post.url) }) { Icon(Icons.Outlined.Share, contentDescription = "Link teilen") }
            }
        }
    ) {
        LoadableView(detail, onRetry = viewModel.retry(viewModel::load), skeleton = SkeletonLayout.Cards) { loaded ->
            val polls = PollActions(
                vote = { post, index -> viewModel.launch { viewModel.vote(post, index) } },
                close = { post -> viewModel.launch { viewModel.closePoll(post) } }
            )
            val collections =
                CollectionActions { post, collection -> viewModel.launch { viewModel.removeFromCollection(post, collection) } }
            CompositionLocalProvider(LocalPollActions provides polls, LocalCollectionActions provides collections) {
                PostDetailContent(
                    detail = loaded,
                    reply = replyState,
                    replyCallbacks = reply.callbacks(),
                    onAction = { action, post -> viewModel.launch { viewModel.perform(action, post) } },
                    onRefresh = viewModel::load,
                    onLoadReply = reply::loadIfNeeded,
                    focusReply = focusReply,
                    onFocusHandled = { focusReply = false }
                )
            }
        }
    }
}

/** The Post page's content. */
@Composable
fun PostDetailContent(
    detail: PostDetail,
    reply: ComposeUiState,
    replyCallbacks: ComposerCallbacks,
    onAction: (PostAction, Post) -> Unit,
    onRefresh: suspend () -> Unit,
    onLoadReply: suspend () -> Unit,
    focusReply: Boolean = false,
    onFocusHandled: () -> Unit = {}
) {
    val navigator = LocalNavigator.current
    var showVersions by rememberSaveable { mutableStateOf(false) }
    val post = detail.post
    NetworkPage(onRefresh = onRefresh) {
        block("post") { PostCard(post, onAction = onAction, isDetail = true) }
        if (post.editedAt != null && detail.revisions.isNotEmpty()) {
            block("versions") {
                Disclosure(
                    "Frühere Fassungen (${detail.revisions.size})",
                    showVersions,
                    { showVersions = !showVersions },
                    Modifier.testTag("post.versions")
                ) {
                    detail.revisions.forEach { revision ->
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            HorizontalDivider(color = MildcrowdColors.PaperLine)
                            Text(
                                "Gültig bis ${fullDate(revision.replacedAt)} Uhr",
                                style = MildcrowdType.Small,
                                color = MildcrowdColors.Muted
                            )
                            Text(revision.body, style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
                        }
                    }
                }
            }
        }
        block("replies.title") { SectionTitle("Antworten") }
        val crowd = post.crowd
        if (post.viewer?.canWriteReply == true) {
            block("reply.form") {
                LaunchedEffect(Unit) { onLoadReply() }
                PaperBox {
                    ComposerForm(reply, replyCallbacks, showsImages = false, focusRequest = focusReply, onFocusHandled = onFocusHandled)
                }
            }
        } else if (post.viewer?.canReply == true && crowd != null) {
            // Replies in a crowd land in it, so only its members write them: say so instead of a missing form.
            block("reply.membership") {
                DashedNote(
                    Modifier
                        .clickable(role = Role.Button) { navigator.open(NetworkRoute.Crowd(crowd.slug)) }
                        .testTag("post.replyNeedsMembership")
                ) {
                    Text(
                        buildAnnotatedString {
                            append("Antworten kann hier, wer der Crowd folgt. ")
                            withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                                append("Zu „${crowd.name}“")
                            }
                        },
                        style = MildcrowdType.Body,
                        color = MildcrowdColors.Ink2,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            }
        }
        if (detail.replies.isEmpty()) block("replies.empty") { DashedNote("Noch keine Antworten.") }
        detail.replies.forEach { item ->
            block("reply.${item.id}", contentType = "post") { PostCard(item, onAction = onAction) }
        }
        block("side.author") {
            SideCard("Geschrieben von") {
                MemberRow(post.author, size = 52.dp)
                detail.authorBio?.let { Text(it) }
                TextLink("Zum Profil", NetworkRoute.Profile(post.author.username))
            }
        }
        if (crowd != null) {
            block("side.crowd") {
                SideCard("In der Crowd") {
                    Row(
                        Modifier.clickable(role = Role.Button) { navigator.open(NetworkRoute.Crowd(crowd.slug)) }.padding(vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CrowdDot(crowd.color)
                        Text(crowd.name, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                    }
                    detail.crowdDescription?.let { Text(it) }
                }
            }
        }
        block("side.quiet") {
            SideCard("Leise Zahlen", tone = PanelTone.Dune) {
                Text("Wie viele reagiert haben, sieht nur, wer den Post geschrieben hat. So zählt, was gesagt wird – nicht, wie laut.")
            }
        }
    }
}
