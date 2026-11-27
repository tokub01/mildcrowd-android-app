package de.velwebsolutions.mildcrowd.account.ui.welcome

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
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
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.account.api.PrivacyChoice
import de.velwebsolutions.mildcrowd.account.api.WelcomeTour
import de.velwebsolutions.mildcrowd.account.ui.components.FlowChoices
import de.velwebsolutions.mildcrowd.account.ui.components.LinkText
import de.velwebsolutions.mildcrowd.account.ui.retry
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdKeyboard
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Einstieg (/app/willkommen): about you, who sees you, crowds, people. Every step can be skipped, the whole tour too
 * („Später“); finishing or skipping ends it for good, so the app stops opening it. A new step fades in at the top. Back
 * from the Privatsphäre-Check, „Wer dich sieht“ shows what was saved there.
 */
@Composable
internal fun WelcomeScreen(viewModel: WelcomeViewModel = hiltViewModel()) {
    val navigator = LocalNavigator.current
    val ui by viewModel.ui.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    LifecycleEventEffect(Lifecycle.Event.ON_RESUME) { scope.launch { viewModel.appeared() } }
    // The live hint waits for a short pause in typing.
    LaunchedEffect(ui.bio, ui.location) {
        delay(500)
        viewModel.textChanged()
    }
    val finish: suspend () -> Unit = {
        // Back to where it was opened from: the Start's card or the settings.
        if (viewModel.finish()) navigator.back()
    }
    // System back goes one step back first, like the „Zurück“ button.
    BackHandler(enabled = ui.step > 1, onBack = viewModel::back)
    MildcrowdScreen(title = "Willkommen") {
        WelcomeContent(ui, viewModel, finish, navigator::open)
    }
}

@Composable
internal fun WelcomeContent(ui: WelcomeUiState, viewModel: WelcomeViewModel, finish: suspend () -> Unit, open: (AppRoute) -> Unit) {
    val scroll = rememberScrollState()
    LaunchedEffect(ui.step) { scroll.animateScrollTo(0) }
    Column(Modifier.fillMaxSize().verticalScroll(scroll).padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Column(Modifier.widthIn(max = 720.dp).fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(20.dp)) {
            LoadableView(ui.state, onRetry = viewModel.retry(viewModel::load)) { tour ->
                Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                    Header(ui, tour, finish)
                    ui.error?.let { Banner(it) }
                    AnimatedContent(ui.stepKey, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "step") { key ->
                        Column(
                            Modifier.fillMaxWidth().background(
                                MildcrowdColors.Paper,
                                RoundedCornerShape(MildcrowdRadius.M.dp)
                            ).padding(20.dp),
                            verticalArrangement = Arrangement.spacedBy(18.dp)
                        ) {
                            when (key) {
                                "profil" -> ProfileStep(ui, tour, viewModel)
                                "sichtbarkeit" -> VisibilityStep(ui, tour, viewModel, open)
                                "crowds" -> CrowdsStep(tour, viewModel, open)
                                else -> PeopleStep(open)
                            }
                        }
                    }
                    Buttons(ui, tour, viewModel, finish)
                    Text(
                        "Jeder Schritt ist freiwillig. Alles findest du später auch in deinem Profil, den Privatsphäre-Einstellungen und unter Crowds.",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Muted
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Header(ui: WelcomeUiState, tour: WelcomeTour, finish: suspend () -> Unit) {
    val scope = rememberCoroutineScope()
    val once = remember(scope) { OneAtATime(scope) }
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        FlowRow(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalArrangement = Arrangement.Center) {
            Eyebrow("Willkommen · Schritt ${ui.step} von ${tour.steps.size}", Modifier.align(Alignment.CenterVertically))
            MildcrowdTextButton("Später – direkt zur Startseite", { once.run(finish) }, Modifier.testTag("welcome.later"))
        }
        Text(
            tour.steps.getOrNull(ui.step - 1)?.title.orEmpty(),
            style = MildcrowdType.display(30),
            color = MildcrowdColors.Night,
            modifier = Modifier.semantics { heading() }.testTag("welcome.title")
        )
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            for (index in 1..maxOf(tour.steps.size, 1)) {
                Box(
                    Modifier.weight(1f).height(6.dp).background(
                        if (index <=
                            ui.step
                        ) {
                            MildcrowdColors.Night
                        } else {
                            MildcrowdColors.Dune
                        },
                        CircleShape
                    )
                )
            }
        }
    }
}

/** Runs one action at a time: taps while it runs are ignored (the text button has no busy state of its own). */
internal class OneAtATime(private val scope: CoroutineScope) {
    private var running = false

    fun run(action: suspend () -> Unit) {
        if (running) return
        running = true
        scope.launch {
            try {
                action()
            } finally {
                running = false
            }
        }
    }
}

@Composable
private fun ColumnScope.ProfileStep(ui: WelcomeUiState, tour: WelcomeTour, viewModel: WelcomeViewModel) {
    Text(
        "Ein paar Worte helfen anderen, dich einzuordnen. Alles ist freiwillig und lässt sich jederzeit ändern.",
        style = MildcrowdType.Body,
        color = MildcrowdColors.Ink2
    )
    MildcrowdTextField(
        "Über dich (optional)",
        ui.bio,
        { value -> viewModel.edit { it.copy(bio = value) } },
        modifier = Modifier.testTag("welcome.bio"),
        prompt = "Zum Beispiel: Ich fotografiere analog und gärtnere auf dem Balkon.",
        error = ui.fieldErrors["bio"],
        lines = 3..6,
        maxLength = 300
    )
    MildcrowdTextField(
        "Ort (optional)",
        ui.location,
        { value -> viewModel.edit { it.copy(location = value) } },
        prompt = "Zum Beispiel deine Stadt",
        error = ui.fieldErrors["location"],
        maxLength = 80
    )
    ui.toneHint?.let { hint ->
        Text(
            "Kurzer Hinweis: „$hint“ wirkt schnell herablassend. Bitte achte auf einen freundlichen Ton – siehe Regeln zum Umgangston.",
            style = MildcrowdType.Small,
            color = MildcrowdColors.Ink,
            modifier = Modifier.fillMaxWidth().background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.CHIP.dp)).padding(12.dp)
        )
    }
    if (ui.toneQuestion) {
        Column(
            Modifier.fillMaxWidth().background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.TILE.dp)).padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "Nochmal lesen? Deine Beschreibung klingt verletzend. Vielleicht findest du andere Worte. Ist sie deutlich beleidigend, hetzerisch oder herabsetzend, sehen andere sie erst, wenn das Moderationsteam sie geprüft hat.",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Ink
            )
            ConsentCheck(ui.toneConfirmed, { on ->
                viewModel.edit { it.copy(toneConfirmed = on) }
            }, Modifier.testTag("welcome.toneConfirmed")) {
                Text("So speichern – ich meine es genau so.", style = MildcrowdType.Small, color = MildcrowdColors.Ink)
            }
        }
    }
    Text("Farbe deiner Initialen", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
    Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
        (listOf(PrivacyChoice(AvatarTint.AUTOMATIC, "Automatisch")) + tour.colors).forEach { color ->
            val isSelected = ui.avatarColor == color.value
            Column(
                Modifier
                    .sizeIn(minWidth = 44.dp, minHeight = 44.dp)
                    .clip(RoundedCornerShape(MildcrowdRadius.CHIP.dp))
                    .clickable(role = Role.RadioButton) { viewModel.edit { it.copy(avatarColor = color.value) } }
                    .semantics {
                        contentDescription = color.label
                        selected = isSelected
                    },
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(AvatarTint.background(AvatarTint.shown(color.value, viewModel.member?.id ?: 0)), CircleShape)
                        .then(if (isSelected) Modifier.border(3.dp, MildcrowdColors.Night, CircleShape) else Modifier)
                )
                Text(color.label, style = MildcrowdType.Caption, color = MildcrowdColors.Ink)
            }
        }
    }
    Text(
        "Deine Initialen siehst du überall, wo kein Profilbild gezeigt wird. „Automatisch“ wählt eine Farbe für dich.",
        style = MildcrowdType.Caption,
        color = MildcrowdColors.Muted
    )
}

@Composable
private fun ColumnScope.VisibilityStep(ui: WelcomeUiState, tour: WelcomeTour, viewModel: WelcomeViewModel, open: (AppRoute) -> Unit) {
    Text(
        "Mildcrowd startet zurückhaltend: Nur Mitglieder sehen dich. Du kannst das jetzt ändern oder später in Ruhe im Privatsphäre-Check.",
        style = MildcrowdType.Body,
        color = MildcrowdColors.Ink2
    )
    Text("Wer darf dein Profil sehen?", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
    FlowChoices(tour.audiences, ui.profileAudience, tag = "welcome.audience") { value ->
        viewModel.edit { it.copy(profileAudience = value, visibilityChanged = true) }
    }
    ui.fieldErrors["profile_audience"]?.let { FieldError(it) }
    Text("Wer liest deine Posts normalerweise?", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
    FlowChoices(tour.visibilities, ui.defaultPostVisibility, tag = "welcome.visibility") { value ->
        viewModel.edit { it.copy(defaultPostVisibility = value, visibilityChanged = true) }
    }
    ui.fieldErrors["default_post_visibility"]?.let { FieldError(it) }
    LinkText("Alle Einstellungen im Privatsphäre-Check", { open(AccountRoute.PrivacyCheckup) })
}

@Composable
private fun ColumnScope.CrowdsStep(tour: WelcomeTour, viewModel: WelcomeViewModel, open: (AppRoute) -> Unit) {
    // How the list came about (Art. 27 DSA): picked by people, or simply the largest crowds.
    val origin =
        if (tour.crowdsHandPicked) {
            "Diese hier haben Menschen ausgesucht, nicht ein Algorithmus."
        } else {
            "Hier stehen die Crowds mit den meisten Mitgliedern – der Reihe nach, ohne Empfehlung nach deinen Interessen."
        }
    Text(
        "Crowds sind Gruppen zu einem Thema. Tritt ein paar bei, dann füllt sich deine Startseite. $origin",
        style = MildcrowdType.Body,
        color = MildcrowdColors.Ink2
    )
    if (tour.crowds.isEmpty()) {
        Text("Noch gibt es keine Crowds. Du kannst selbst eine gründen.", style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
    }
    tour.crowds.forEach { crowd ->
        Column(
            Modifier.fillMaxWidth().border(1.dp, MildcrowdColors.PaperLine, RoundedCornerShape(MildcrowdRadius.TILE.dp)).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
                val banner = crowd.bannerUrl
                if (banner != null) {
                    AuthorizedImage(
                        banner,
                        contentDescription = null,
                        modifier = Modifier.size(width = 72.dp, height = 36.dp).clip(RoundedCornerShape(MildcrowdRadius.CHIP.dp)),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(Modifier.size(36.dp).background(AvatarTint.surface(crowd.color), CircleShape))
                }
                Column {
                    Text(crowd.name, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                    Text(
                        "${crowd.membersCount} ${if (crowd.membersCount == 1) "Mitglied" else "Mitglieder"}",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Muted
                    )
                }
            }
            Text(crowd.description, style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
            AsyncMildcrowdButton(
                if (crowd.joined) "Beigetreten ✓" else "Beitreten",
                { viewModel.toggle(crowd) },
                modifier = Modifier.semantics { selected = crowd.joined }.testTag("welcome.crowd.${crowd.slug}"),
                kind = if (crowd.joined) MildcrowdButtonKind.Primary else MildcrowdButtonKind.Secondary,
                fullWidth = false
            )
        }
    }
    LinkText("Alle Crowds ansehen", { open(NetworkRoute.Crowds) })
}

@Composable
private fun ColumnScope.PeopleStep(open: (AppRoute) -> Unit) {
    var search by remember { mutableStateOf("") }
    Text(
        "Deine Startseite zeigt, was Menschen teilen, denen du folgst – der Reihe nach und mit Ende. Such nach Menschen, die du kennst, oder lade sie ein.",
        style = MildcrowdType.Body,
        color = MildcrowdColors.Ink2
    )
    MildcrowdTextField("Menschen suchen", search, {
        search = it
    }, prompt = "Name oder @benutzername", keyboardOptions = MildcrowdKeyboard.NoCapitalization)
    MildcrowdButton("Suchen", {
        open(NetworkRoute.Search(search.ifEmpty { null }))
    }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
    MildcrowdButton("Freund:innen einladen", { open(NetworkRoute.Invitations) }, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun Buttons(ui: WelcomeUiState, tour: WelcomeTour, viewModel: WelcomeViewModel, finish: suspend () -> Unit) {
    FlowRow(horizontalArrangement = Arrangement.spacedBy(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (ui.step > 1) MildcrowdButton("Zurück", viewModel::back, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
        if (ui.step < tour.steps.size) {
            AsyncMildcrowdButton("Weiter", viewModel::next, modifier = Modifier.testTag("welcome.next"), fullWidth = false)
            MildcrowdTextButton("Schritt überspringen", viewModel::skip, Modifier.align(Alignment.CenterVertically).testTag("welcome.skip"))
        } else {
            AsyncMildcrowdButton("Los geht's", finish, modifier = Modifier.testTag("welcome.finish"), fullWidth = false)
        }
    }
}
