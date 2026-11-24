package de.velwebsolutions.mildcrowd.network.ui.crowds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.ChatRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.CrowdMeetups
import de.velwebsolutions.mildcrowd.network.api.CrowdWelcome
import de.velwebsolutions.mildcrowd.network.api.Meetup
import de.velwebsolutions.mildcrowd.network.api.Member
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.DateTimeField
import de.velwebsolutions.mildcrowd.network.ui.components.Disclosure
import de.velwebsolutions.mildcrowd.network.ui.components.MemberAvatar
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import java.time.Instant

/** „Mittwoch, 4. November 2026, 18:00 Uhr“ (`Meetup::whenText`, German time). */
fun meetupWhen(instant: Instant): String = GermanTime.format(instant, "EEEE, d. MMMM yyyy, HH:mm") + " Uhr"

/** „3 kommen · 1 vielleicht · 0 nicht“. */
fun meetupCounts(counts: Meetup.Counts): String =
    "${counts.yes} ${if (counts.yes == 1) "kommt" else "kommen"} · ${counts.maybe} vielleicht · ${counts.no} nicht"

/** What the crowd page can do with its meetups and Willkommenspaten. */
class CrowdMeetupActions(
    val onOpenForm: (Meetup?) -> Unit,
    val onEditForm: ((MeetupForm) -> MeetupForm) -> Unit,
    val onCloseForm: () -> Unit,
    val onSave: suspend () -> Unit,
    val onAnswer: (Meetup, String) -> Unit,
    val onCancel: (Meetup) -> Unit,
    val onAllowMembers: (Boolean) -> Unit,
    val onVolunteer: (Boolean) -> Unit
) {
    companion object {
        val None = CrowdMeetupActions({}, {}, {}, {}, { _, _ -> }, {}, {}, {})
    }
}

/** The meetup part of the crowd page: the list and its form, for members only. */
class CrowdMeetupsUi(val meetups: CrowdMeetups?, val form: MeetupForm?, val actions: CrowdMeetupActions) {
    companion object {
        val None = CrowdMeetupsUi(null, null, CrowdMeetupActions.None)
    }
}

/**
 * „Treffen“: „Treffen anlegen“ for those allowed, the form, the coming meetups (or „Gerade ist kein Treffen geplant.“),
 * „Vergangene Treffen“ and – for hosts – „Mitglieder dürfen Treffen anlegen“.
 */
@Composable
internal fun MeetupsSection(meetups: CrowdMeetups, form: MeetupForm?, isHost: Boolean, actions: CrowdMeetupActions) {
    var showPast by rememberSaveable { mutableStateOf(false) }
    Column(Modifier.fillMaxWidth().testTag("crowd.meetups"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            SectionTitle("Treffen", Modifier.weight(1f))
            if (meetups.canCreate && form == null) {
                MildcrowdButton(
                    "Treffen anlegen",
                    { actions.onOpenForm(null) },
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false,
                    modifier = Modifier.testTag("meetup.create")
                )
            }
        }
        form?.let { MeetupFormView(it, actions) }
        if (meetups.upcoming.isEmpty()) {
            Text("Gerade ist kein Treffen geplant.", style = MildcrowdType.Body, color = MildcrowdColors.Muted)
        }
        meetups.upcoming.forEach { MeetupCard(it, actions) }
        if (meetups.past.isNotEmpty()) {
            Disclosure("Vergangene Treffen", showPast, {
                showPast = !showPast
            }, Modifier.testTag("meetup.past"), border = MildcrowdColors.DuneLine) {
                meetups.past.forEach { MeetupCard(it, actions) }
            }
        }
        if (isHost) {
            ConsentCheck(
                "Mitglieder dürfen Treffen anlegen",
                meetups.membersCreateMeetups,
                actions.onAllowMembers,
                Modifier.testTag("meetup.allowMembers")
            )
        }
    }
}

/** One meetup: when, title, place, link, description, the answers with their counts and names, and its changes. */
@Composable
internal fun MeetupCard(meetup: Meetup, actions: CrowdMeetupActions) {
    var showPeople by rememberSaveable(meetup.id) { mutableStateOf(false) }
    var confirmCancel by remember { mutableStateOf(false) }
    val navigator = LocalNavigator.current
    Column(
        Modifier
            .fillMaxWidth()
            .background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.M.dp))
            .padding(20.dp)
            .testTag("meetup.${meetup.id}"),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        val state = when {
            meetup.cancelled -> " · Abgesagt"
            meetup.past -> " · Vorbei"
            else -> ""
        }
        Text(
            meetupWhen(meetup.startsAt) + state,
            style = MildcrowdType.SmallBold,
            color = if (meetup.cancelled) MildcrowdColors.Danger else MildcrowdColors.Ink2
        )
        Text(
            meetup.title,
            style = MildcrowdType.heading(
                19
            ).copy(textDecoration = if (meetup.cancelled) TextDecoration.LineThrough else TextDecoration.None),
            color = MildcrowdColors.Night
        )
        meetup.place?.let { Text(it, style = MildcrowdType.Body, color = MildcrowdColors.Ink2) }
        meetup.link?.let { link ->
            Text(
                link,
                style = MildcrowdType.Body.copy(textDecoration = TextDecoration.Underline),
                color = MildcrowdColors.Night,
                modifier = Modifier.pressable({ navigator.openWeb(link) }).padding(vertical = 4.dp).testTag("meetup.link")
            )
        }
        meetup.description?.let { Text(it, style = MildcrowdType.Body, color = MildcrowdColors.Ink) }
        Text(meetupCounts(meetup.counts), style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
        if (meetup.canAnswer) {
            ButtonFlow(Modifier.testTag("meetup.answers")) {
                Meetup.ANSWERS.forEach { (value, label) ->
                    val chosen = meetup.myAnswer == value
                    MildcrowdButton(
                        label,
                        { actions.onAnswer(meetup, value) },
                        kind = if (chosen) MildcrowdButtonKind.Primary else MildcrowdButtonKind.Secondary,
                        fullWidth = false,
                        modifier = Modifier.semantics { selected = chosen }.testTag("meetup.answer.$value")
                    )
                }
            }
        }
        if (meetup.people.isNotEmpty()) {
            Disclosure("Wer geantwortet hat", showPeople, { showPeople = !showPeople }, border = MildcrowdColors.DuneLine) {
                meetup.people.sortedBy { person -> Meetup.ANSWERS.indexOfFirst { it.first == person.answer } }.forEach { person ->
                    Text(
                        "${person.member.displayName} · ${Meetup.answerLabel(person.answer)}",
                        style = MildcrowdType.Body,
                        color = MildcrowdColors.Ink
                    )
                }
            }
        }
        if (meetup.canEdit) {
            ButtonFlow {
                MildcrowdButton("Ändern", {
                    actions.onOpenForm(meetup)
                }, kind = MildcrowdButtonKind.Secondary, fullWidth = false, modifier = Modifier.testTag("meetup.edit"))
                MildcrowdButton("Treffen absagen", {
                    confirmCancel = true
                }, kind = MildcrowdButtonKind.Secondary, fullWidth = false, modifier = Modifier.testTag("meetup.cancel"))
            }
        }
    }
    if (confirmCancel) {
        ConfirmDialog(
            "Treffen absagen?",
            "Absagen",
            onConfirm = { actions.onCancel(meetup) },
            onDismiss = { confirmCancel = false },
            message = "Wer zugesagt hat, bekommt einen Hinweis."
        )
    }
}

/** „Neues Treffen“ / „Treffen ändern“. */
@Composable
internal fun MeetupFormView(form: MeetupForm, actions: CrowdMeetupActions) {
    val creating = form.editing == null
    Column(
        Modifier
            .fillMaxWidth()
            .background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.M.dp))
            .padding(20.dp)
            .testTag("meetup.form"),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(if (creating) "Neues Treffen" else "Treffen ändern", style = MildcrowdType.heading(17), color = MildcrowdColors.Night)
        MildcrowdTextField(
            "Titel",
            form.title,
            { value -> actions.onEditForm { it.copy(title = value) } },
            Modifier.testTag("meetup.title"),
            error = form.error?.message("title"),
            maxLength = 120
        )
        DateTimeField(
            "Datum und Uhrzeit",
            form.startsAt,
            { value -> actions.onEditForm { it.copy(startsAt = value) } },
            Modifier.testTag("meetup.startsAt"),
            error = form.error?.message("starts_at"),
            zone = GermanTime.zone,
            earliest = Instant.now()
        )
        MildcrowdTextField(
            "Ort (optional)",
            form.place,
            { value -> actions.onEditForm { it.copy(place = value) } },
            Modifier.testTag("meetup.place"),
            error = form.error?.message("place"),
            maxLength = 200
        )
        MildcrowdTextField(
            "Link (optional, z. B. zum Videoraum)",
            form.link,
            { value -> actions.onEditForm { it.copy(link = value) } },
            Modifier.testTag("meetup.linkField"),
            prompt = "https://",
            error = form.error?.message("link"),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
            maxLength = 500
        )
        MildcrowdTextField(
            "Beschreibung (optional)",
            form.description,
            { value -> actions.onEditForm { it.copy(description = value) } },
            Modifier.testTag("meetup.description"),
            error = form.error?.message("description"),
            lines = 3..8,
            maxLength = 2000
        )
        // A refusal without a field (no right any more, the server failed) stands under the form.
        form.error?.takeIf {
            it !is ApiError.Validation
        }?.let { Text(it.userMessage, style = MildcrowdType.Small, color = MildcrowdColors.Danger) }
        ButtonFlow {
            AsyncMildcrowdButton(
                if (creating) "Treffen anlegen" else "Änderung speichern",
                actions.onSave,
                fullWidth = false,
                modifier = Modifier.testTag("meetup.save")
            )
            MildcrowdButton(
                "Abbrechen",
                actions.onCloseForm,
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("meetup.close")
            )
        }
    }
}

/** „Deine Willkommenspatin/dein Willkommenspate: {Name}“ with „Nachricht schreiben“ (a new chat with the pate). */
@Composable
internal fun PateCard(pate: Member) {
    val navigator = LocalNavigator.current
    Row(
        Modifier
            .fillMaxWidth()
            .background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.M.dp))
            .padding(20.dp)
            .testTag("crowd.pate"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        MemberAvatar(pate)
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(
                "Deine Willkommenspatin/dein Willkommenspate: ${pate.displayName}",
                style = MildcrowdType.Body,
                color = MildcrowdColors.Night
            )
            ButtonFlow {
                MildcrowdButton(
                    "Nachricht schreiben",
                    { navigator.open(ChatRoute.NewConversationWith(pate.username)) },
                    kind = MildcrowdButtonKind.Primary,
                    fullWidth = false,
                    modifier = Modifier.testTag("crowd.pate.message")
                )
                MildcrowdButton(
                    "Profil",
                    { navigator.open(NetworkRoute.Profile(pate.username)) },
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false
                )
            }
        }
    }
}

/** „Willkommenspaten“: the own switch, and for hosts the volunteers („Freiwillige“). */
@Composable
internal fun VolunteerCard(welcome: CrowdWelcome, onVolunteer: (Boolean) -> Unit) {
    SideCard("Willkommenspaten") {
        Text("Neue Mitglieder sehen eine Patin oder einen Paten, bei der oder dem sie sich mit Fragen melden können.")
        ConsentCheck("Ich bin gern Willkommenspate oder -patin", welcome.volunteer, onVolunteer, Modifier.testTag("crowd.volunteer"))
        welcome.volunteers?.let { volunteers ->
            Text("Freiwillige", style = MildcrowdType.SmallBold, color = MildcrowdColors.Night)
            if (volunteers.isEmpty()) Text("Noch niemand.", style = MildcrowdType.Body, color = MildcrowdColors.Muted)
            volunteers.forEach { Text(it.displayName, style = MildcrowdType.Body, color = MildcrowdColors.Ink) }
        }
    }
}
