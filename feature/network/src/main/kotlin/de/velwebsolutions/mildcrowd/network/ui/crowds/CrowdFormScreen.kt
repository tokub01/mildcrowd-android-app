package de.velwebsolutions.mildcrowd.network.ui.crowds

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.ErrorStateView
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadingView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.Choice
import de.velwebsolutions.mildcrowd.network.api.CrowdInput
import de.velwebsolutions.mildcrowd.network.ui.ImageConverter
import de.velwebsolutions.mildcrowd.network.ui.components.ChoiceList
import de.velwebsolutions.mildcrowd.network.ui.components.ChoiceMenu
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.PaperBox
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.rememberImagePicker
import de.velwebsolutions.mildcrowd.network.ui.retry

/**
 * Found a crowd (/app/crowds/neu) or edit it as host (/crowds/{slug}/bearbeiten): texts, house rules, audience, colour,
 * the optional banner (stored at once when editing, with "Crowd eröffnen" when founding), closing and roles.
 */
@Composable
fun CrowdFormScreen(
    slug: String?,
    viewModel: CrowdFormViewModel =
        hiltViewModel<CrowdFormViewModel, CrowdFormViewModel.Factory>(key = "crowdForm:$slug", creationCallback = { it.create(slug) })
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val editing = slug != null
    val picker = rememberImagePicker { uris ->
        viewModel.launch { viewModel.pickBanner(ImageConverter.upload(context, uris.first(), "banner", maxEdge = 3000)) }
    }

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    LaunchedEffect(state.outcome) {
        val outcome = state.outcome ?: return@LaunchedEffect
        // Back to the crowd (edited), or the new crowd in place of the form.
        navigator.back()
        if (outcome is CrowdFormOutcome.Open) navigator.open(NetworkRoute.Crowd(outcome.slug))
        viewModel.consumeOutcome()
    }

    MildcrowdScreen(
        title = if (editing) "Crowd bearbeiten" else "Crowd eröffnen",
        message = status,
        onDismissMessage = viewModel::dismissStatus
    ) {
        val loadError = state.loadError
        val options = state.options
        when {
            loadError != null -> ErrorStateView(loadError, onRetry = viewModel.retry(viewModel::load))

            options == null -> LoadingView()

            else -> NetworkPage {
                block("header") {
                    PageHeader(
                        if (editing) "${state.edit?.crowd?.name.orEmpty()} bearbeiten" else "Crowd eröffnen",
                        lead = if (editing) {
                            null
                        } else {
                            "Du wirst Gastgeber:in: Du achtest auf die Hausregeln und kannst Posts entfernen, die dagegen verstoßen."
                        }
                    )
                }
                block("form") {
                    PaperBox(spacing = 18) {
                        // Held by the automated check: the host reads the texts, others „Crowd in Prüfung“ (docs/mildcrowd-auto-moderation.md).
                        if (state.edit?.crowd?.held == true) CrowdHeldNote()
                        val failure = error
                        if (failure != null && CrowdFormViewModel.FIELDS.none { failure.message(it) != null }) Banner(failure.userMessage)
                        MildcrowdTextField(
                            "Name",
                            state.input.name,
                            { value -> viewModel.updateInput { it.copy(name = value) } },
                            error = failure?.message("name"),
                            modifier = Modifier.testTag("crowdForm.name"),
                            maxLength = 60
                        )
                        MildcrowdTextField(
                            "Worum geht es?",
                            state.input.description,
                            { value -> viewModel.updateInput { it.copy(description = value) } },
                            error = failure?.message("description"),
                            hint = "Ein Satz, der neugierig macht.",
                            lines = 1..4,
                            modifier = Modifier.testTag("crowdForm.description"),
                            maxLength = 300
                        )
                        MildcrowdTextField(
                            "Hausregeln",
                            state.input.houseRules,
                            { value -> viewModel.updateInput { it.copy(houseRules = value) } },
                            error = failure?.message("house_rules"),
                            hint = "Gelten zusätzlich zu den Community-Regeln von Mildcrowd.",
                            lines = 4..10,
                            modifier = Modifier.testTag("crowdForm.rules"),
                            maxLength = 3000
                        )
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Wer darf die Crowd lesen?", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
                            ChoiceList(options.visibilities, state.input.visibility, { value ->
                                viewModel.updateInput { it.copy(visibility = value) }
                            })
                            failure?.message("visibility")?.let { FieldError(it) }
                        }
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Farbe", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
                            ColorChoice(options.colors, state.input.color) { value -> viewModel.updateInput { it.copy(color = value) } }
                            failure?.message("color")?.let { FieldError(it) }
                        }
                        if (options.regions.isNotEmpty()) {
                            RegionFields(options.regions, state.input, failure?.message("region") ?: failure?.message("city")) { change ->
                                viewModel.updateInput(change)
                            }
                        }
                        BannerSection(state, editing, failure?.message("banner"), onPick = picker.pick, onRemove = viewModel::removeBanner)
                        AsyncMildcrowdButton(
                            if (editing) "Speichern" else "Crowd eröffnen",
                            viewModel::save,
                            fullWidth = false,
                            modifier = Modifier.testTag("crowdForm.save")
                        )
                    }
                }
                val edit = state.edit
                // Only an edited crowd has an edit form (and with it its slug).
                if (edit != null) {
                    val slug = edit.crowd.slug
                    block("close") {
                        val navigator = LocalNavigator.current
                        PaperBox(border = MildcrowdColors.Danger) {
                            SectionTitle("Crowd schließen")
                            Text(
                                "Die Crowd verschwindet für alle. Die Posts bleiben bei den Menschen, die sie geschrieben haben.",
                                style = MildcrowdType.Body,
                                color = MildcrowdColors.Ink2
                            )
                            MildcrowdButton(
                                "Crowd schließen …",
                                { navigator.open(NetworkRoute.CloseCrowd(slug)) },
                                kind = MildcrowdButtonKind.Secondary,
                                fullWidth = false,
                                modifier = Modifier.testTag("crowdForm.close")
                            )
                        }
                    }
                    block("roles") {
                        PaperBox {
                            SectionTitle("Gastgeber:innen")
                            error?.message("role")?.let { FieldError(it) }
                            edit.members.forEach { member ->
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        buildAnnotatedString {
                                            append(member.displayName)
                                            withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append(" @${member.username}") }
                                            append(" · ${member.roleLabel}")
                                        },
                                        style = MildcrowdType.Small,
                                        color = MildcrowdColors.Ink,
                                        modifier = Modifier.weight(1f)
                                    )
                                    val host = member.role == "host"
                                    UnderlinedLink(
                                        if (host) "Zu Mitglied machen" else "Zu Gastgeber:in machen",
                                        { viewModel.launch { viewModel.setRole(member.id, if (host) "member" else "host") } },
                                        style = MildcrowdType.Small
                                    )
                                }
                            }
                            edit.hiddenMembersNote?.let { note ->
                                Text(
                                    note,
                                    style = MildcrowdType.Small,
                                    color = MildcrowdColors.Ink2,
                                    modifier = Modifier.testTag("crowdForm.hiddenMembers")
                                )
                            }
                        }
                    }
                }
                block("side.rules") {
                    SideCard("Gute Hausregeln", tone = PanelTone.Dune) {
                        Bullets(
                            listOf(
                                "Kurz und konkret: drei bis fünf Punkte reichen.",
                                "Sag, was erwünscht ist – nicht nur, was verboten ist.",
                                "Die Community-Regeln von Mildcrowd gelten immer zusätzlich."
                            )
                        )
                    }
                }
                block("side.host") {
                    SideCard("Als Gastgeber:in", tone = PanelTone.Night) {
                        Text(
                            "Du kannst Posts aus der Crowd entfernen, die gegen die Hausregeln verstoßen, und weitere Gastgeber:innen " +
                                "ernennen. Rechtswidriges meldet ihr dem Moderationsteam."
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun BannerSection(state: CrowdFormState, editing: Boolean, error: String?, onPick: () -> Unit, onRemove: suspend () -> Unit) {
    val shape = RoundedCornerShape(MildcrowdRadius.TILE.dp)
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            buildAnnotatedString {
                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("Banner ") }
                withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append("(freiwillig)") }
            },
            style = MildcrowdType.Small,
            color = MildcrowdColors.Ink
        )
        val picked = state.banner
        val current = state.edit?.crowd
        if (picked != null) {
            val bitmap = remember(picked) { BitmapFactory.decodeByteArray(picked.data, 0, picked.data.size)?.asImageBitmap() }
            if (bitmap != null) {
                Image(
                    bitmap,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxWidth().aspectRatio(3f).clip(shape)
                )
            }
            if (!editing) Text("Ausgewählt: ${picked.filename}", style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
        } else if (current?.bannerUrl != null) {
            CrowdBanner(current, Modifier.clip(shape), height = 110.dp)
        }
        val hasBanner = picked != null || current?.bannerUrl != null
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
            MildcrowdButton(
                if (hasBanner) "Anderes Bild wählen" else "Bild hochladen",
                onPick,
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false
            )
            if (hasBanner) {
                AsyncMildcrowdButton("Entfernen", onRemove, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            }
        }
        Text(
            "Breites Querformat, wir schneiden es auf 3:1 zu. Standort- und Kameradaten entfernen wir. Bitte nur Bilder, die zu den Community-Regeln passen.",
            style = MildcrowdType.Caption,
            color = MildcrowdColors.Muted
        )
        error?.let { FieldError(it) }
    }
}

/** The four colours with their German names (Blau, Pfirsich, Nacht, Nebel). */
@Composable
fun ColorChoice(
    choices: List<Choice>,
    selection: String,
    /** The colour a choice shows („Automatisch“ shows the one it resolves to). */
    swatch: (String) -> String = { it },
    onSelect: (String) -> Unit
) {
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        choices.forEach { choice ->
            val selected = selection == choice.value
            Column(
                Modifier
                    .defaultMinSize(minWidth = 56.dp, minHeight = 56.dp)
                    .selectable(selected = selected, role = Role.RadioButton) { onSelect(choice.value) }
                    .semantics { contentDescription = choice.label },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier.size(
                        40.dp
                    ).border(3.dp, if (selected) MildcrowdColors.Night else Color.Transparent, CircleShape).padding(4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(Modifier.size(32.dp).background(AvatarTint.background(swatch(choice.value)), CircleShape))
                }
                Text(choice.label, style = MildcrowdType.Caption, color = MildcrowdColors.Ink)
            }
        }
    }
}

/** „Region (freiwillig, für die Karte der Crowds)“: a Bundesland and, with one, a city or area – never coordinates. */
@Composable
internal fun RegionFields(regions: List<Choice>, input: CrowdInput, error: String?, onChange: ((CrowdInput) -> CrowdInput) -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("crowdForm.regionFields")) {
        Text(
            buildAnnotatedString {
                append("Region ")
                withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append("(freiwillig, für die Karte der Crowds)") }
            },
            style = MildcrowdType.SmallBold,
            color = MildcrowdColors.Ink
        )
        ChoiceMenu(
            "Bundesland",
            listOf(Choice("", "Keine Region")) + regions,
            input.region.orEmpty(),
            { code -> onChange { it.copy(region = code.ifEmpty { null }) } },
            Modifier.testTag("crowdForm.region")
        )
        if (input.region != null) {
            MildcrowdTextField(
                "Ort oder Gegend",
                input.city,
                { value -> onChange { it.copy(city = value) } },
                prompt = "z. B. Leipzig",
                modifier = Modifier.testTag("crowdForm.city"),
                maxLength = 60
            )
        }
        error?.let { FieldError(it) }
    }
}
