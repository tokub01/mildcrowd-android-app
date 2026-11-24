package de.velwebsolutions.mildcrowd.network.ui.profiles

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.CapsuleInvitation
import de.velwebsolutions.mildcrowd.network.api.Member
import de.velwebsolutions.mildcrowd.network.api.TimeCapsule
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.DateTimeField
import de.velwebsolutions.mildcrowd.network.ui.components.PaperBox
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.UnderlinedLink
import de.velwebsolutions.mildcrowd.network.ui.components.block
import java.time.Instant

/** „24. Dezember 2026 um 18:00 Uhr“ (`ManageCapsuleContributions::deadlineText`, German time). */
fun capsuleMoment(instant: Instant): String = GermanTime.format(instant, "d. MMMM yyyy 'um' HH:mm") + " Uhr"

/** What the creator sees per invitee: „Eingeladen“, „Hat beigetragen“, „Abgelehnt“, „Erschienen“, „Nicht erschienen“. */
fun inviteeStatus(status: String): String = when (status) {
    "contributed" -> "Hat beigetragen"
    "declined" -> "Abgelehnt"
    "published" -> "Erschienen"
    "failed" -> "Nicht erschienen"
    else -> "Eingeladen"
}

/** „Geht auf am … in der Crowd Garten · Beitragen bis …“ of an invitation. */
fun invitationLine(invitation: CapsuleInvitation): String {
    val place = invitation.crowd?.let { "in der Crowd ${it.name}" } ?: "auf dem Profil von ${invitation.from.displayName}"
    val until = invitation.contributeUntil?.let { " · Beitragen bis ${capsuleMoment(it)}" }.orEmpty()
    return "Geht auf am ${capsuleMoment(invitation.opensAt)} $place$until"
}

/** „Gemeinsam versiegeln“: who writes along (at most 10) and until when. */
@Composable
internal fun SharedSealSection(
    draft: CapsuleDraft,
    candidates: List<Member>?,
    fields: Map<String, String>,
    viewModel: TimeCapsulesViewModel
) {
    ConsentCheck(
        "Gemeinsam versiegeln",
        draft.shared,
        { on -> viewModel.launch { viewModel.setShared(on) } },
        Modifier.testTag("capsule.shared")
    )
    if (!draft.shared) return
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.testTag("capsule.invitees")) {
        Text("Wer schreibt mit? (höchstens 10)", style = MildcrowdType.SmallBold, color = MildcrowdColors.Night)
        Text(
            "Deine Freund:innen" + (if (draft.crowdId != null) " und die Mitglieder der Crowd" else "") +
                ". Jede Person schreibt ihren eigenen Text; beim Aufgehen erscheinen die Beiträge als Antworten unter deinem " +
                "Posts. Bis dahin liest sie niemand, auch du nicht.",
            style = MildcrowdType.Small,
            color = MildcrowdColors.Ink2
        )
        if (candidates.isNullOrEmpty()) {
            Text(
                "Noch niemand zum Einladen: Freund:innen und Mitglieder deiner Crowds erscheinen hier.",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Muted
            )
        }
        candidates.orEmpty().forEach { person ->
            ConsentCheck(
                "${person.displayName} @${person.username}",
                person.username in draft.invitees,
                { viewModel.toggleInvitee(person.username) },
                Modifier.testTag("capsule.invitee.${person.username}")
            )
        }
        fields["invitees"]?.let { Text(it, style = MildcrowdType.Small, color = MildcrowdColors.Danger) }
        DateTimeField(
            "Beitragen bis",
            draft.contributeUntil ?: draft.opensAt.minusSeconds(3_600),
            viewModel::updateContributeUntil,
            Modifier.testTag("capsule.until"),
            error = fields["contribute_until"],
            earliest = Instant.now(),
            latest = draft.opensAt
        )
        Text("Ohne Wahl: bis eine Stunde vor dem Aufgehen.", style = MildcrowdType.Small, color = MildcrowdColors.Muted)
    }
}

/** On an own gemeinsame Zeitkapsel: the deadline and where each invitee stands (the texts stay sealed). */
@Composable
internal fun CapsuleSharedInfo(capsule: TimeCapsule) {
    val shared = capsule.shared ?: return
    Column(Modifier.testTag("capsule.shared.${capsule.id}"), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            "Gemeinsam" + shared.contributeUntil?.let { " · Beitragen bis ${capsuleMoment(it)}" }.orEmpty(),
            style = MildcrowdType.SmallBold,
            color = MildcrowdColors.Night
        )
        shared.invitees.forEach { invitee ->
            Text(
                "${invitee.member.displayName}: ${inviteeStatus(invitee.status)}",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Ink2
            )
        }
    }
}

/** „Einladungen“ to others' gemeinsame Zeitkapseln (`#einladungen`). */
internal fun LazyListScope.invitationsSection(
    invitations: List<CapsuleInvitation>,
    texts: Map<Int, String>,
    errors: Map<Int, String>,
    viewModel: TimeCapsulesViewModel
) {
    if (invitations.isEmpty()) return
    block("invitations") {
        Column(verticalArrangement = Arrangement.spacedBy(12.dp), modifier = Modifier.testTag("capsule.invitations")) {
            SectionTitle("Einladungen")
            invitations.forEach { InvitationCard(it, texts[it.id] ?: it.body.orEmpty(), errors[it.id], viewModel) }
        }
    }
}

@Composable
private fun InvitationCard(invitation: CapsuleInvitation, text: String, error: String?, viewModel: TimeCapsulesViewModel) {
    val navigator = LocalNavigator.current
    var confirmDecline by remember { mutableStateOf(false) }
    PaperBox(spacing = 8, modifier = Modifier.testTag("capsule.invitation.${invitation.id}")) {
        Text(
            "${invitation.from.displayName} lädt dich zu einer gemeinsamen Zeitkapsel ein.",
            style = MildcrowdType.BodyBold,
            color = MildcrowdColors.Night
        )
        Text(invitationLine(invitation), style = MildcrowdType.Small, color = MildcrowdColors.Ink2)
        val postId = invitation.postId
        when {
            invitation.status == "declined" -> Text("Du hast abgelehnt.", style = MildcrowdType.Small, color = MildcrowdColors.Muted)

            postId != null -> UnderlinedLink("Dein Beitrag ist erschienen", {
                navigator.open(NetworkRoute.Post(postId))
            }, style = MildcrowdType.BodyBold)

            invitation.status == "failed" -> {
                Text(
                    "Nicht erschienen: ${invitation.failedReason.orEmpty()}",
                    style = MildcrowdType.BodyBold,
                    color = MildcrowdColors.Danger
                )
                Text(invitation.body.orEmpty(), style = MildcrowdType.Body, color = MildcrowdColors.Ink2, maxLines = 2)
                AsyncMildcrowdButton(
                    "Beitrag entfernen",
                    { viewModel.removeContribution(invitation) },
                    kind = MildcrowdButtonKind.Secondary,
                    fullWidth = false
                )
            }

            invitation.canEdit -> {
                MildcrowdTextField(
                    "Dein Beitrag",
                    text,
                    { viewModel.updateContribution(invitation.id, it) },
                    Modifier.testTag("capsule.contribution.${invitation.id}"),
                    error = error,
                    lines = 3..8
                )
                ButtonFlow {
                    AsyncMildcrowdButton(
                        if (invitation.body == null) "Beitragen" else "Beitrag speichern",
                        { viewModel.saveContribution(invitation) },
                        fullWidth = false,
                        modifier = Modifier.testTag("capsule.contribute.${invitation.id}")
                    )
                    if (invitation.body != null) {
                        AsyncMildcrowdButton(
                            "Beitrag entfernen",
                            { viewModel.removeContribution(invitation) },
                            kind = MildcrowdButtonKind.Secondary,
                            fullWidth = false,
                            modifier = Modifier.testTag("capsule.remove.${invitation.id}")
                        )
                    }
                    MildcrowdButton(
                        "Ablehnen",
                        { confirmDecline = true },
                        kind = MildcrowdButtonKind.Secondary,
                        fullWidth = false,
                        modifier = Modifier.testTag("capsule.decline.${invitation.id}")
                    )
                }
            }

            else -> Text(
                if (invitation.body == null) {
                    "Die Zeit zum Beitragen ist vorbei."
                } else {
                    "Dein Beitrag ist versiegelt und erscheint, wenn die Zeitkapsel aufgeht."
                },
                style = MildcrowdType.Small,
                color = MildcrowdColors.Muted
            )
        }
    }
    if (confirmDecline) {
        ConfirmDialog(
            "Einladung ablehnen?",
            "Ablehnen",
            onConfirm = { viewModel.launch { viewModel.decline(invitation) } },
            onDismiss = { confirmDecline = false },
            message = "Dein Text wird gelöscht."
        )
    }
}

/** Below Mittendrin, but invited: the way to the membership in a card, with the invitations below it. */
@Composable
internal fun CapsuleLockedPanel(message: String) {
    val navigator = LocalNavigator.current
    Panel(tone = PanelTone.Paper, spacing = 16) {
        Eyebrow("Ab Mittendrin")
        Text("Zeitkapseln", style = MildcrowdType.display(28), color = MildcrowdColors.Night)
        Text(message, style = MildcrowdType.Lead, color = MildcrowdColors.Ink2)
        MildcrowdButton("Mitgliedschaft ansehen", { navigator.open(MembershipRoute.Overview) }, fullWidth = false)
    }
}
