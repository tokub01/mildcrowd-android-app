package de.velwebsolutions.mildcrowd.network.ui.compose

import android.net.Uri
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.Image
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.PhotoCamera
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.media.ImageCodec
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.WebRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.Choice
import de.velwebsolutions.mildcrowd.network.api.ExternalLink
import de.velwebsolutions.mildcrowd.network.api.ImageUpload
import de.velwebsolutions.mildcrowd.network.api.PollDraft
import de.velwebsolutions.mildcrowd.network.api.PublishResult
import de.velwebsolutions.mildcrowd.network.ui.ImageConverter
import de.velwebsolutions.mildcrowd.network.ui.components.ChoiceMenu
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.rememberImagePicker
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** What the composer form can ask its view model for. */
class ComposerCallbacks(
    val onBody: (String) -> Unit,
    val onVisibility: (String) -> Unit,
    val onKind: (String) -> Unit,
    val onLinkRead: (Boolean) -> Unit,
    val onToneConfirmed: (Boolean) -> Unit,
    /** A picked image, or null when it could not be read. */
    val onAddImage: (ImageUpload?) -> Unit,
    val onRemoveImage: (Int) -> Unit,
    val onAlt: (Int, String) -> Unit,
    val checkTone: suspend () -> Unit,
    val publish: suspend () -> PublishResult?,
    /** The answer of a publish whose form went away while it was sent (a rotation during the upload), for the next form. */
    val unclaimedPublish: suspend () -> PublishResult? = { null },
    /** „Umfrage hinzufügen“ / „entfernen“ and its fields. */
    val poll: PollCallbacks = PollCallbacks.None
)

/** The poll of a new crowd Post in the composer. */
class PollCallbacks(val onShow: (PollDraft?) -> Unit, val onEdit: ((PollDraft) -> PollDraft) -> Unit) {
    companion object {
        val None = PollCallbacks({}, {})
    }
}

/** The form's callbacks wired to this view model. */
fun ComposeViewModel.callbacks() = ComposerCallbacks(
    onBody = ::updateBody,
    onVisibility = ::setVisibility,
    onKind = ::setKind,
    onLinkRead = ::setLinkRead,
    onToneConfirmed = ::setToneConfirmed,
    onAddImage = ::add,
    onRemoveImage = ::removeImage,
    onAlt = ::updateAlt,
    checkTone = ::checkTone,
    publish = ::publish,
    unclaimedPublish = ::unclaimedPublish,
    poll = PollCallbacks(::showPoll, ::editPoll)
)

/**
 * The composer form (`livewire/network/compose`): text with live tone hints, "Nochmal lesen?" with "So senden", the
 * read-before-carry confirmation, up to four images with required descriptions, audience and kind.
 */
@Composable
fun ComposerForm(
    state: ComposeUiState,
    callbacks: ComposerCallbacks,
    modifier: Modifier = Modifier,
    showsImages: Boolean = true,
    /** Focuses the text field once ("Antworten" on a card opens the reply field ready to type). */
    focusRequest: Boolean = false,
    onFocusHandled: () -> Unit = {},
    /** Called after a successful publish, also one started by the form before this one (a rotation during the upload). */
    onPublished: (PublishResult) -> Unit = {}
) {
    val scope = rememberCoroutineScope()
    val focus = remember { FocusRequester() }
    var publishing by remember { mutableStateOf(false) }
    var loadingImages by remember { mutableStateOf(false) }

    LaunchedEffect(state.body) {
        delay(400)
        callbacks.checkTone()
    }
    LaunchedEffect(focusRequest) {
        if (focusRequest) {
            runCatching { focus.requestFocus() }
            onFocusHandled()
        }
    }
    // A Post sent from the form before this one (it went away during the upload, e.g. with a rotation) ends here.
    LaunchedEffect(Unit) { callbacks.unclaimedPublish()?.let(onPublished) }

    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        state.generalError?.let { Banner(it) }
        if (!state.plainCarry) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                MildcrowdTextField(
                    label = state.bodyLabel,
                    value = state.body,
                    onValueChange = callbacks.onBody,
                    prompt = if (state.isReply) {
                        "Antworten …"
                    } else if (state.isCarry) {
                        null
                    } else {
                        "Was möchtest du teilen?"
                    },
                    error = state.fieldError("body"),
                    lines = 3..10,
                    modifier = Modifier.focusRequester(focus).testTag("compose.body")
                )
                Text(
                    "${state.length}/${state.maxLength}",
                    style = MildcrowdType.Caption,
                    color = MildcrowdColors.Muted,
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "${state.length} von ${state.maxLength} Zeichen" }
                )
                AnimatedVisibility(state.toneHint != null) { state.toneHint?.let { ToneHintNote(it) } }
            }
        }
        AnimatedVisibility(state.toneNudge) {
            ToneQuestion(
                text = "Nochmal lesen? Dein Post klingt verletzend. Vielleicht findest du andere Worte. Ist er deutlich beleidigend, " +
                    "hetzerisch oder herabsetzend, halten wir ihn nach dem Senden zurück, bis das Moderationsteam ihn geprüft hat.",
                confirmation = "So senden – ich meine es genau so.",
                isConfirmed = state.toneConfirmed,
                onConfirmedChange = callbacks.onToneConfirmed,
                tag = "compose.toneConfirmed"
            )
        }
        val links = state.context.valueOrNull?.links.orEmpty()
        if (links.isNotEmpty()) LinkConfirmation(links, state, callbacks)
        if (showsImages && !state.plainCarry) {
            ImagesSection(state, callbacks, loadingImages, onLoading = { loadingImages = it })
        }
        if (state.canPoll) PollSection(state, callbacks.poll)
        Options(state, callbacks)
        MildcrowdButton(
            text = state.submitLabel,
            // While it publishes the button shows its spinner and takes no further taps.
            onClick = {
                publishing = true
                scope.launch {
                    try {
                        callbacks.publish()?.let(onPublished)
                    } finally {
                        publishing = false
                    }
                }
            },
            fullWidth = false,
            enabled = !loadingImages,
            loading = publishing || state.isPublishing,
            modifier = Modifier.testTag("compose.submit")
        )
    }
}

@Composable
private fun LinkConfirmation(links: List<ExternalLink>, state: ComposeUiState, callbacks: ComposerCallbacks) {
    val navigator = LocalNavigator.current
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Hast du den Link gelesen? Teilen heißt: Du stehst dafür ein.",
            style = MildcrowdType.BodyBold,
            color = MildcrowdColors.Ink,
            modifier = Modifier.semantics { heading() }
        )
        links.forEach { link -> UnderlinedLink(link.host, { navigator.open(WebRoute(link.url)) }) }
        ConsentCheck("Ich habe den Inhalt gelesen", state.linkRead, callbacks.onLinkRead, Modifier.testTag("compose.linkRead"))
        state.fieldError("link_read")?.let { FieldError(it) }
    }
}

@Composable
private fun ImagesSection(state: ComposeUiState, callbacks: ComposerCallbacks, loadingImages: Boolean, onLoading: (Boolean) -> Unit) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val max = state.maxImages
    val remaining = maxOf(0, max - state.images.size)
    val picker = rememberImagePicker(maxItems = maxOf(1, remaining)) { uris: List<Uri> ->
        scope.launch {
            onLoading(true)
            try {
                uris.forEachIndexed { offset, uri ->
                    callbacks.onAddImage(
                        ImageConverter.upload(context, uri, "bild-${state.images.size + offset + 1}", maxEdge = state.imageEdge)
                    )
                }
            } finally {
                onLoading(false)
            }
        }
    }
    val shape = RoundedCornerShape(MildcrowdRadius.TILE.dp)
    Column(
        Modifier.fillMaxWidth().border(1.dp, MildcrowdColors.DuneLine, shape).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTextButton(
                "Bilder hinzufügen",
                Icons.Outlined.Add,
                enabled = remaining > 0,
                tag = "compose.addImages",
                onClick = picker.pick
            )
            picker.takePhoto?.let {
                IconTextButton("Foto aufnehmen", Icons.Outlined.PhotoCamera, enabled = remaining > 0, tag = "compose.camera", onClick = it)
            }
        }
        Text("bis zu $max, JPG, PNG, WebP oder GIF", style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
        if (loadingImages) Text("Bilder werden hochgeladen …", style = MildcrowdType.Small, color = MildcrowdColors.Muted)
        state.fieldError("images")?.let { FieldError(it) }
        state.images.forEachIndexed { index, picked ->
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.Top) {
                val thumbnail = remember(picked.id) { ImageCodec.thumbnail(picked.upload.data)?.asImageBitmap() }
                if (thumbnail != null) {
                    Image(
                        thumbnail,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.size(64.dp).clip(RoundedCornerShape(MildcrowdRadius.CHIP.dp))
                    )
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    MildcrowdTextField(
                        label = "Bildbeschreibung ${index + 1}",
                        value = picked.alt,
                        onValueChange = { callbacks.onAlt(index, it) },
                        error = state.fieldError("alts.$index") ?: state.fieldError("images.$index"),
                        modifier = Modifier.testTag("compose.alt.$index")
                    )
                    UnderlinedLink("Bild ${index + 1} entfernen", { callbacks.onRemoveImage(index) }, style = MildcrowdType.Small)
                }
            }
        }
        Text(
            "Wir entfernen beim Hochladen Standort und Kameradaten aus den Bildern.",
            style = MildcrowdType.Caption,
            color = MildcrowdColors.Muted
        )
    }
}

@Composable
private fun IconTextButton(title: String, icon: ImageVector, enabled: Boolean, tag: String, onClick: () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(icon, contentDescription = null, tint = MildcrowdColors.Night, modifier = Modifier.size(20.dp))
        MildcrowdTextButton(title, onClick, enabled = enabled, modifier = Modifier.testTag(tag))
    }
}

@Composable
private fun Options(state: ComposeUiState, callbacks: ComposerCallbacks) {
    val context = state.context.valueOrNull ?: return
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text("Wer darf lesen?", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
            ChoiceMenu(
                "Wer darf lesen?",
                context.visibilities,
                state.visibility,
                callbacks.onVisibility,
                Modifier.testTag("compose.visibility")
            )
            // Mildcrowd is members-only: whatever is chosen, no guest or search engine reads a Post (compose.blade.php).
            Text(
                "Posts lesen nur angemeldete Mitglieder, nie Gäste oder Suchmaschinen.",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Muted,
                modifier = Modifier.testTag("compose.visibilityHint")
            )
        }
        if (context.kinds.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("Art des Posts", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
                ChoiceMenu("Art des Posts", context.kinds, state.kind, callbacks.onKind, Modifier.testTag("compose.kind"))
            }
        }
        (state.fieldError("visibility") ?: state.fieldError("kind"))?.let { FieldError(it) }
    }
}

/** „Umfrage hinzufügen“, or the poll: „Frage“, „Antwort 1“ …, „Mehrere Antworten erlaubt“, „Läuft“ and „Umfrage entfernen“. */
@Composable
private fun PollSection(state: ComposeUiState, callbacks: PollCallbacks) {
    val poll = state.poll
    if (poll == null) {
        MildcrowdButton(
            "Umfrage hinzufügen",
            { callbacks.onShow(PollDraft()) },
            kind = MildcrowdButtonKind.Secondary,
            fullWidth = false,
            modifier = Modifier.testTag("compose.poll.add")
        )
        return
    }
    Column(
        Modifier
            .fillMaxWidth()
            .border(1.dp, MildcrowdColors.DuneLine, RoundedCornerShape(MildcrowdRadius.S.dp))
            .padding(16.dp)
            .testTag("compose.poll"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text("Umfrage", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
        MildcrowdTextField(
            label = "Frage",
            value = poll.question,
            onValueChange = { value -> callbacks.onEdit { it.copy(question = value) } },
            error = state.fieldError("poll.question"),
            modifier = Modifier.testTag("compose.poll.question")
        )
        poll.options.forEachIndexed { index, option ->
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                MildcrowdTextField(
                    label = "Antwort ${index + 1}",
                    value = option,
                    onValueChange = { value ->
                        callbacks.onEdit {
                            it.copy(
                                options = it.options.mapIndexed { at, old ->
                                    if (at == index) {
                                        value
                                    } else {
                                        old
                                    }
                                }
                            )
                        }
                    },
                    error = state.fieldError("poll.options.$index"),
                    modifier = Modifier.weight(1f).testTag("compose.poll.option.$index")
                )
                if (poll.options.size > 2) {
                    MildcrowdTextButton(
                        "Entfernen",
                        { callbacks.onEdit { it.copy(options = it.options.filterIndexed { at, _ -> at != index }) } },
                        Modifier.testTag("compose.poll.remove.$index")
                    )
                }
            }
        }
        (state.fieldError("poll.options") ?: state.fieldError("poll"))?.let { FieldError(it) }
        if (poll.options.size < 6) {
            MildcrowdButton(
                "Antwort hinzufügen",
                { callbacks.onEdit { it.copy(options = it.options + "") } },
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("compose.poll.addOption")
            )
        }
        ConsentCheck("Mehrere Antworten erlaubt", poll.multiple, { value ->
            callbacks.onEdit { it.copy(multiple = value) }
        }, Modifier.testTag("compose.poll.multiple"))
        ChoiceMenu(
            "Läuft",
            POLL_DURATIONS,
            poll.duration.orEmpty(),
            { value -> callbacks.onEdit { it.copy(duration = value.ifEmpty { null }) } },
            Modifier.testTag("compose.poll.duration")
        )
        state.fieldError("poll.duration")?.let { FieldError(it) }
        MildcrowdTextButton("Umfrage entfernen", { callbacks.onShow(null) }, Modifier.testTag("compose.poll.remove"))
    }
}

/** The runtimes of a poll („Läuft“). */
private val POLL_DURATIONS = listOf(Choice("", "ohne Ende"), Choice("1d", "1 Tag"), Choice("3d", "3 Tage"), Choice("1w", "1 Woche"))
