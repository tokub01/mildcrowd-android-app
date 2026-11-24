package de.velwebsolutions.mildcrowd.network.ui.profiles

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.TierLoadableView
import de.velwebsolutions.mildcrowd.design.components.TierLockedView
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.navigation.WebRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.Choice
import de.velwebsolutions.mildcrowd.network.api.Member
import de.velwebsolutions.mildcrowd.network.api.TimeCapsule
import de.velwebsolutions.mildcrowd.network.api.TimeCapsules
import de.velwebsolutions.mildcrowd.network.api.Yearbook
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ChoiceMenu
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.DateTimeField
import de.velwebsolutions.mildcrowd.network.ui.components.ImageGrid
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.PaperBox
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.capsuleTime
import de.velwebsolutions.mildcrowd.network.ui.retry
import java.time.Instant
import java.time.ZoneId

/** The text of a locked tier page when an old server sends no message of its own. */
private const val FROM_MITTENDRIN = "Das gibt es ab der Mitgliedschaft Mittendrin."

/** The question before a capsule is opened early or deleted. */
private enum class CapsuleQuestion { Open, Delete }

/**
 * Zeitkapseln (/app/zeitkapseln, from Mittendrin): the form that seals a Post for a later day, the waiting capsules with
 * "Jetzt öffnen" and "Löschen", and the opened ones. Below Mittendrin the server's message and the way to the membership.
 */
@Composable
fun TimeCapsulesScreen(
    viewModel: TimeCapsulesViewModel = hiltViewModel<TimeCapsulesViewModel>(),
    /** Opened from a capsule hint (`#einladungen`): the page scrolls to „Einladungen“. */
    atInvitations: Boolean = false
) {
    val capsules by viewModel.capsules.collectAsStateWithLifecycle()
    val draft by viewModel.draft.collectAsStateWithLifecycle()
    val candidates by viewModel.candidates.collectAsStateWithLifecycle()
    val texts by viewModel.contributions.collectAsStateWithLifecycle()
    val textErrors by viewModel.contributionErrors.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val invitationsShown = capsules.valueOrNull?.invitations?.isNotEmpty() == true
    LaunchedEffect(atInvitations && invitationsShown) {
        // „Einladungen“ follow the header (or the way to the membership).
        if (atInvitations && invitationsShown) listState.animateScrollToItem(1)
    }
    val status by viewModel.status.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val fields = error?.fieldMessages.orEmpty()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    MildcrowdScreen(
        title = "Zeitkapseln",
        message = status,
        // Field errors stand below their field; everything else is a toast.
        error = error?.takeIf { fields.keys.none { it in FIELD_ERRORS } }?.userMessage,
        onDismissMessage = viewModel::dismissStatus,
        onDismissError = viewModel::dismissError
    ) {
        LoadableView(capsules, onRetry = viewModel.retry(viewModel::load)) { info ->
            if (!info.available && info.invitations.isNotEmpty()) {
                NetworkPage(state = listState, onRefresh = viewModel::load) {
                    block("locked") { CapsuleLockedPanel(info.lockedMessage ?: FROM_MITTENDRIN) }
                    invitationsSection(info.invitations, texts, textErrors, viewModel)
                }
            } else if (!info.available) {
                TierLockedView(
                    "Zeitkapseln",
                    info.lockedMessage ?: FROM_MITTENDRIN,
                    feature = "Schreib heute einen Post, der erst an einem Tag deiner Wahl erscheint: zum Geburtstag, zum " +
                        "Jahrestag oder für dein künftiges Ich. Bis dahin sieht ihn kein anderes Mitglied."
                )
            } else {
                NetworkPage(state = listState, onRefresh = viewModel::load) {
                    block("header") {
                        PageHeader(
                            "Zeitkapseln",
                            eyebrow = "Für später",
                            lead =
                                "Schreib heute, was erst an einem bestimmten Tag erscheinen soll: zum Geburtstag, zum Jahrestag oder für " +
                                    "dein künftiges Ich. Bis dahin sieht die Zeitkapsel kein anderes Mitglied."
                        )
                    }
                    invitationsSection(info.invitations, texts, textErrors, viewModel)
                    block("form") { CapsuleForm(info, draft, candidates, fields, viewModel) }
                    block("waiting") {
                        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                            SectionTitle("Warten noch")
                            if (info.waiting.isEmpty()) DashedNote("Noch keine Zeitkapsel versiegelt.")
                            info.waiting.forEach { capsule -> WaitingCapsule(capsule, viewModel) }
                        }
                    }
                    if (info.past.isNotEmpty()) {
                        block("past") {
                            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                                SectionTitle("Aufgegangen")
                                info.past.forEach { capsule -> PastCapsule(capsule, viewModel) }
                            }
                        }
                    }
                    block("side") {
                        SideCard("So funktioniert's", tone = PanelTone.Dune) {
                            Bullets(
                                listOf(
                                    "Frühestens in einer Stunde, spätestens in ${info.maxYears} Jahren.",
                                    "Bis zu ${info.maxWaiting} Zeitkapseln können gleichzeitig warten.",
                                    "Beim Versiegeln und beim Aufgehen prüfen wir sie wie jeden Post. " +
                                        "Läuft deine Mitgliedschaft beim Aufgehen nicht, bleibt sie zu."
                                )
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CapsuleForm(
    info: TimeCapsules,
    draft: CapsuleDraft,
    candidates: List<Member>?,
    fields: Map<String, String>,
    viewModel: TimeCapsulesViewModel
) {
    val places = listOf(Choice("", "auf deinem Profil")) + info.crowds.map { Choice("${it.id}", "in der Crowd ${it.name}") }
    PaperBox(spacing = 16) {
        MildcrowdTextField(
            "Dein Post",
            draft.body,
            viewModel::updateBody,
            lines = 4..10,
            error = fields["body"],
            hint = "${draft.body.codePointCount(0, draft.body.length)} / ${info.maxLength}",
            modifier = Modifier.testTag("capsule.body")
        )
        val now = Instant.now()
        DateTimeField(
            "Geht auf am",
            draft.opensAt,
            viewModel::updateOpensAt,
            error = fields["opens_at"],
            earliest = now,
            latest = CapsuleDraft.latestOpening(now, info.maxYears)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text("Erscheint", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
            ChoiceMenu(
                "Erscheint",
                places,
                draft.crowdId?.toString().orEmpty(),
                { value -> viewModel.launch { viewModel.updateCrowd(value.toIntOrNull()) } },
                Modifier.testTag("capsule.place")
            )
        }
        SharedSealSection(draft, candidates, fields, viewModel)
        AsyncMildcrowdButton(
            "Zeitkapsel versiegeln",
            viewModel::seal,
            enabled = draft.body.isNotBlank(),
            fullWidth = false,
            modifier = Modifier.testTag("capsule.seal")
        )
    }
}

@Composable
private fun WaitingCapsule(capsule: TimeCapsule, viewModel: TimeCapsulesViewModel) {
    var question by remember { mutableStateOf<CapsuleQuestion?>(null) }
    PaperBox(spacing = 8, modifier = Modifier.testTag("capsule.waiting.${capsule.id}")) {
        Text(
            "Geht auf am ${capsuleTime(capsule.opensAt)} ${capsule.crowd?.let { "in ${it.name}" } ?: "auf deinem Profil"}",
            style = MildcrowdType.SmallBold,
            color = MildcrowdColors.Ink2
        )
        Text(capsule.body, style = MildcrowdType.Body, color = MildcrowdColors.Ink)
        CapsuleSharedInfo(capsule)
        ButtonFlow {
            MildcrowdButton("Jetzt öffnen", { question = CapsuleQuestion.Open }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            MildcrowdButton("Löschen", { question = CapsuleQuestion.Delete }, kind = MildcrowdButtonKind.Destructive, fullWidth = false)
        }
    }
    if (question == CapsuleQuestion.Open) {
        ConfirmDialog(
            "Die Zeitkapsel jetzt schon öffnen?",
            "Jetzt öffnen",
            onConfirm = { viewModel.launch { viewModel.openNow(capsule.id) } },
            onDismiss = { question = null },
            message = "Dann erscheint sie gleich als Post.",
            destructive = false
        )
    }
    if (question == CapsuleQuestion.Delete) {
        ConfirmDialog(
            "Die Zeitkapsel löschen?",
            "Löschen",
            onConfirm = { viewModel.launch { viewModel.delete(capsule.id) } },
            onDismiss = { question = null },
            message = "Das lässt sich nicht rückgängig machen."
        )
    }
}

@Composable
private fun PastCapsule(capsule: TimeCapsule, viewModel: TimeCapsulesViewModel) {
    val navigator = LocalNavigator.current
    var deleting by remember { mutableStateOf(false) }
    PaperBox(spacing = 4, modifier = Modifier.testTag("capsule.past.${capsule.id}")) {
        val postId = capsule.postId
        val reason = capsule.failedReason
        if (postId != null) {
            UnderlinedLink(
                "Aufgegangen am ${GermanTime.format(capsule.openedAt ?: capsule.opensAt, "d. MMMM yyyy", ZoneId.systemDefault())}",
                { navigator.open(NetworkRoute.Post(postId)) },
                style = MildcrowdType.BodyBold
            )
        } else if (reason != null) {
            Text("Nicht aufgegangen: $reason", style = MildcrowdType.BodyBold, color = MildcrowdColors.Danger)
        } else {
            // Claimed by the server, its Post is being published right now (web: „Geht gerade auf …“).
            Text("Geht gerade auf …", style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink2)
        }
        Text(capsule.body, style = MildcrowdType.Body, color = MildcrowdColors.Ink2, maxLines = 2)
        CapsuleSharedInfo(capsule)
        if (postId == null && capsule.openedAt == null) {
            // A capsule that did not open keeps its text until the author removes it.
            MildcrowdButton(
                "Löschen",
                { deleting = true },
                kind = MildcrowdButtonKind.Destructive,
                fullWidth = false,
                modifier = Modifier.testTag("capsule.delete.${capsule.id}")
            )
        }
    }
    if (deleting) {
        ConfirmDialog(
            "Die Zeitkapsel löschen?",
            "Löschen",
            onConfirm = { viewModel.launch { viewModel.delete(capsule.id) } },
            onDismiss = { deleting = false },
            message = "Das lässt sich nicht rückgängig machen."
        )
    }
}

/**
 * Dein Jahrbuch (/app/jahrbuch/{year?}, from Mittendrin): the year chips, the cover with the year's totals, the months with
 * their Posts, and "Drucken oder als PDF", which opens the printable book on the website. Below Mittendrin the server's
 * message and the way to the membership.
 */
@Composable
fun YearbookScreen(
    year: Int?,
    viewModel: YearbookViewModel = hiltViewModel<YearbookViewModel, YearbookViewModel.Factory>(creationCallback = { it.create(year) })
) {
    val book by viewModel.book.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    MildcrowdScreen(title = "Jahrbuch", error = error?.userMessage, onDismissError = viewModel::dismissError) {
        TierLoadableView(
            book,
            "Dein Jahrbuch",
            onRetry = viewModel.retry(viewModel::load),
            feature = "Deine Posts und Fotos eines Jahres, gestaltet als Buch zum Durchblättern, Drucken oder als PDF zum " +
                "Aufheben. Damit bleibt, was du geteilt hast, auch ohne Bildschirm."
        ) { shown ->
            NetworkPage(onRefresh = viewModel::load) {
                block("years") {
                    ButtonFlow {
                        shown.years.forEach { option ->
                            MildcrowdButton(
                                "$option",
                                { viewModel.launch { viewModel.show(option) } },
                                kind = if (option == shown.year) MildcrowdButtonKind.Primary else MildcrowdButtonKind.Secondary,
                                fullWidth = false,
                                modifier = Modifier.testTag("yearbook.year.$option").semantics { selected = option == shown.year }
                            )
                        }
                    }
                }
                block("print") {
                    MildcrowdButton(
                        "Drucken oder als PDF",
                        { navigator.open(WebRoute(shown.printUrl)) },
                        kind = MildcrowdButtonKind.Secondary,
                        modifier = Modifier.testTag("yearbook.print")
                    )
                }
                block("cover") { Cover(shown) }
                if (shown.months.isEmpty()) {
                    block("empty") { DashedNote("In ${shown.year} hast du noch keine Posts geteilt. Was du schreibst, landet hier.") }
                }
                shown.months.forEach { month ->
                    block("month.${shown.year}.${month.name}") {
                        Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                            Text(
                                month.name,
                                style = MildcrowdType.display(28),
                                color = MildcrowdColors.Night,
                                modifier = Modifier.semantics {
                                    heading()
                                }
                            )
                            month.posts.forEach { entry -> YearbookEntry(entry) }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun Cover(book: Yearbook) {
    Column(
        Modifier
            .fillMaxWidth()
            .background(MildcrowdColors.Night, RoundedCornerShape(MildcrowdRadius.L.dp))
            .padding(horizontal = 24.dp, vertical = 48.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Eyebrow("Jahrbuch", onNight = true)
        Text(
            "${book.year}",
            style = MildcrowdType.display(56),
            color = MildcrowdColors.OnNight,
            modifier = Modifier.semantics {
                heading()
            }
        )
        Text(yearbookTotals(book.totals), style = MildcrowdType.Body, color = MildcrowdColors.OnNight2, textAlign = TextAlign.Center)
    }
}

@Composable
private fun YearbookEntry(entry: Yearbook.Entry) {
    PaperBox(modifier = Modifier.testTag("yearbook.entry.${entry.id}")) {
        val day = GermanTime.format(entry.createdAt, "EEEE, d. MMMM")
        Text(listOfNotNull(day, entry.crowd?.name).joinToString(" · "), style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink2)
        Text(entry.body, style = MildcrowdType.Lead, color = MildcrowdColors.Ink)
        if (entry.images.isNotEmpty()) ImageGrid(entry.images, entry.id)
        if (entry.reactions > 0 || entry.replies > 0) {
            val reactions = "${entry.reactions} ${if (entry.reactions == 1) "Reaktion" else "Reaktionen"}"
            val replies = "${entry.replies} ${if (entry.replies == 1) "Antwort" else "Antworten"}"
            Text("$reactions · $replies", style = MildcrowdType.Small, color = MildcrowdColors.Muted)
        }
    }
}

/** The capsule form's fields that show their own message. */
private val FIELD_ERRORS = setOf("body", "opens_at", "invitees", "contribute_until")
