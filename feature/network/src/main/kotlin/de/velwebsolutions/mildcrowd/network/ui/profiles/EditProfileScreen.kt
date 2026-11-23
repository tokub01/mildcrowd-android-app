package de.velwebsolutions.mildcrowd.network.ui.profiles

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.Choice
import de.velwebsolutions.mildcrowd.network.api.OwnProfile
import de.velwebsolutions.mildcrowd.network.ui.ImageConverter
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ImagePicker
import de.velwebsolutions.mildcrowd.network.ui.components.MemberAvatar
import de.velwebsolutions.mildcrowd.network.ui.components.MemberRow
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.PaperBox
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.rememberImagePicker
import de.velwebsolutions.mildcrowd.network.ui.compose.ToneHintNote
import de.velwebsolutions.mildcrowd.network.ui.compose.ToneQuestion
import de.velwebsolutions.mildcrowd.network.ui.crowds.ColorChoice
import de.velwebsolutions.mildcrowd.network.ui.retry
import kotlinx.coroutines.delay

/** Under „Farbe deiner Initialen“ (docs/mildcrowd-accounts.md). */
const val AVATAR_COLOR_HINT =
    "Deine Initialen siehst du überall, wo kein Profilbild gezeigt wird. „Automatisch“ wählt eine Farbe für dich."

/**
 * "Dein Profil" (/app/profil): bio with tone hints, location, the optional photo and banner and the colour of the
 * initials. Who may see the profile is a privacy setting (account area).
 */
@Composable
fun EditProfileScreen(viewModel: EditProfileViewModel = hiltViewModel<EditProfileViewModel>()) {
    val profile by viewModel.profile.collectAsStateWithLifecycle()
    val form by viewModel.form.collectAsStateWithLifecycle()
    val status by viewModel.status.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    val context = LocalContext.current
    val photoPicker = rememberImagePicker { uris ->
        viewModel.setUploading(true)
        viewModel.launch { viewModel.uploadPhoto(ImageConverter.upload(context, uris.first(), "profilbild", maxEdge = 1024)) }
    }
    val bannerPicker = rememberImagePicker { uris ->
        viewModel.setUploading(true)
        viewModel.launch { viewModel.uploadBanner(ImageConverter.upload(context, uris.first(), "banner", maxEdge = 3000)) }
    }

    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    LaunchedEffect(form.bio, form.location, form.status) {
        delay(500)
        viewModel.checkTone()
    }

    MildcrowdScreen(title = "Dein Profil", message = status, onDismissMessage = viewModel::dismissStatus) {
        LoadableView(profile, onRetry = viewModel.retry(viewModel::load)) { current ->
            NetworkPage {
                block("header") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Eyebrow("Profil")
                        Text(
                            "Dein Profil",
                            style = MildcrowdType.display(28),
                            color = MildcrowdColors.Night,
                            modifier = Modifier.semantics {
                                heading()
                            }
                        )
                        UnderlinedLink(
                            "So sehen dich andere: dein Profil ansehen.",
                            { navigator.open(NetworkRoute.Profile(current.member.username)) },
                            color = MildcrowdColors.Ink2
                        )
                        UnderlinedLink(
                            "Name und Benutzername änderst du in den Einstellungen.",
                            { navigator.open(AccountRoute.Settings) },
                            color = MildcrowdColors.Ink2
                        )
                    }
                }
                block("form") {
                    ProfileForm(current, form, error, viewModel, photoPicker, bannerPicker)
                }
                block("preview") {
                    SideCard("Vorschau") {
                        MemberRow(current.member, size = 56.dp)
                        Text(current.bio.ifEmpty { "Noch keine Beschreibung." }, style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
                        ButtonFlow {
                            MildcrowdButton("Deine Zahlen", {
                                navigator.open(NetworkRoute.Statistics)
                            }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
                            MildcrowdButton("Menschen einladen", {
                                navigator.open(NetworkRoute.Invitations)
                            }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ProfileForm(
    current: OwnProfile,
    form: EditProfileState,
    error: ApiError?,
    viewModel: EditProfileViewModel,
    photoPicker: ImagePicker,
    bannerPicker: ImagePicker
) {
    val navigator = LocalNavigator.current
    PaperBox(spacing = 18) {
        if (error != null && EditProfileViewModel.FIELDS.none { error.message(it) != null }) Banner(error.userMessage)
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            MildcrowdTextField(
                "Über dich (optional)",
                form.bio,
                viewModel::updateBio,
                error = error?.message("bio"),
                lines = 3..8,
                modifier = Modifier.testTag("profileEdit.bio"),
                maxLength = 300
            )
            AnimatedVisibility(form.toneHint != null) { form.toneHint?.let { ToneHintNote(it) } }
            AnimatedVisibility(form.toneNudge) {
                ToneQuestion(
                    text =
                        "Nochmal lesen? Deine Beschreibung klingt verletzend. Vielleicht findest du andere Worte. Ist sie deutlich " +
                            "beleidigend, hetzerisch oder herabsetzend, sehen andere sie erst, wenn das Moderationsteam sie geprüft hat.",
                    confirmation = "So speichern – ich meine es genau so.",
                    isConfirmed = form.toneConfirmed,
                    onConfirmedChange = viewModel::setToneConfirmed,
                    tag = "profileEdit.toneConfirmed"
                )
            }
            if (current.bioHeld) HeldBadge()
        }
        MildcrowdTextField(
            "Ort (optional)",
            form.location,
            viewModel::updateLocation,
            error = error?.message("location"),
            hint = "Zum Beispiel deine Stadt. Lass es leer, wenn du das nicht zeigen möchtest.",
            modifier = Modifier.testTag("profileEdit.location"),
            maxLength = 80
        )
        MildcrowdTextField(
            "Status (optional)",
            form.status,
            viewModel::updateStatus,
            error = error?.message("status"),
            hint = "Was gerade bei dir los ist, zum Beispiel „Im Urlaub bis Sonntag“. Steht unter deinem Namen, auch im Chat.",
            modifier = Modifier.testTag("profileEdit.status")
        )
        Text(
            buildAnnotatedString {
                append("Wer dein Profil lesen und mit dir interagieren darf, wählst du in den ")
                withStyle(SpanStyle(fontWeight = FontWeight.Bold, textDecoration = TextDecoration.Underline)) {
                    append("Privatsphäre-Einstellungen")
                }
                append(".")
            },
            style = MildcrowdType.Body,
            color = MildcrowdColors.Ink2,
            modifier = Modifier.clickable(role = Role.Button) { navigator.open(AccountRoute.Privacy) }
        )
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionalLabel("Profilbild")
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MemberAvatar(current.member, size = 64.dp)
                MildcrowdButton(
                    if (current.hasPhoto) "Anderes Bild wählen" else "Bild hochladen",
                    photoPicker.pick,
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false,
                    enabled = !form.uploading,
                    modifier = Modifier.testTag("profileEdit.photo")
                )
                if (current.hasPhoto) {
                    AsyncMildcrowdButton(
                        "Entfernen",
                        viewModel::removePhoto,
                        kind = MildcrowdButtonKind.Secondary,
                        fullWidth = false
                    )
                }
            }
            photoPicker.takePhoto?.let { take -> UnderlinedLink("Foto aufnehmen", take, style = MildcrowdType.Small) }
            if (form.uploading) Text("Bild wird hochgeladen …", style = MildcrowdType.Small, color = MildcrowdColors.Muted)
            Text(
                "Quadratisch zugeschnitten. Standort- und Kameradaten entfernen wir. Menschen, die Bilder von Fremden erst freigeben, " +
                    "sehen statt des Bildes deine Initialen. Bitte nur Bilder, die zu den Community-Regeln passen.",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Muted
            )
            error?.message("photo")?.let { FieldError(it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            OptionalLabel("Banner")
            current.bannerUrl?.let { url ->
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(3f)
                        .clip(RoundedCornerShape(MildcrowdRadius.TILE.dp))
                        .background(AvatarTint.background(current.member.avatarColor))
                        .clearAndSetSemantics {}
                ) {
                    AuthorizedImage(url, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                MildcrowdButton(
                    if (current.bannerUrl != null) "Anderes Bild wählen" else "Bild hochladen",
                    bannerPicker.pick,
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false,
                    enabled = !form.uploading,
                    modifier = Modifier.testTag("profileEdit.banner")
                )
                if (current.bannerUrl != null) {
                    AsyncMildcrowdButton(
                        "Entfernen",
                        viewModel::removeBanner,
                        kind = MildcrowdButtonKind.Secondary,
                        fullWidth = false,
                        modifier = Modifier.semantics { contentDescription = "Banner entfernen" }
                    )
                }
            }
            Text(
                "Breites Querformat, wir schneiden es auf 3:1 zu. Standort- und Kameradaten entfernen wir. Menschen, die Bilder von " +
                    "Fremden erst freigeben, sehen stattdessen deine Farbe. Bitte nur Bilder, die zu den Community-Regeln passen.",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Muted
            )
            error?.message("banner")?.let { FieldError(it) }
        }
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Farbe deiner Initialen", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
            ColorChoice(
                listOf(Choice(AvatarTint.AUTOMATIC, "Automatisch")) + current.colors,
                form.color,
                swatch = { AvatarTint.shown(it, current.member.id) },
                onSelect = viewModel::updateColor
            )
            Text(AVATAR_COLOR_HINT, style = MildcrowdType.Caption, color = MildcrowdColors.Muted)
            error?.message("avatar_color")?.let { FieldError(it) }
        }
        AsyncMildcrowdButton("Profil speichern", viewModel::save, fullWidth = false, modifier = Modifier.testTag("profileEdit.save"))
    }
}

@Composable
private fun OptionalLabel(title: String) {
    Text(
        buildAnnotatedString {
            withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append("$title ") }
            withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append("(freiwillig)") }
        },
        style = MildcrowdType.Small,
        color = MildcrowdColors.Ink
    )
}
