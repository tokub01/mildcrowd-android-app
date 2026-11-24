package de.velwebsolutions.mildcrowd.staff.ui.admin

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffDate
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffLoadable
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffPage
import de.velwebsolutions.mildcrowd.staff.ui.components.StaffTitle
import de.velwebsolutions.mildcrowd.staff.ui.retry

/** Systemzustand (web: livewire.admin.system): health checks, backups and monitoring, bot refusals, last runs. */
@Composable
fun SystemScreen(viewModel: SystemViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    MildcrowdScreen("System", error = state.error, onDismissError = viewModel::dismissError) {
        StaffLoadable(state.status, viewModel.retry(viewModel::load)) { status ->
            StaffPage(viewModel::load) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Eyebrow("Admin")
                    StaffTitle("Systemzustand")
                    Text(
                        "Alles, wovon die Versprechen von Mildcrowd abhängen: Löschfristen, Mollie-Abgleich, Mails und Sicherungen. " +
                            "Probleme meldet der Zeitplaner alle 15 Minuten per E-Mail. Was zu tun ist, steht in " +
                            "docs/mildcrowd-operations.md.",
                        style = MildcrowdType.Small,
                        color = MildcrowdColors.Ink2
                    )
                }
                SectionCard(if (status.healthy) "Alles in Ordnung" else "Es gibt Probleme") {
                    AsyncMildcrowdButton(
                        "Jetzt prüfen",
                        viewModel::load,
                        Modifier.testTag("system.check"),
                        kind = MildcrowdButtonKind.Secondary,
                        fullWidth = false
                    )
                    status.checks.forEach { check ->
                        HorizontalDivider(color = MildcrowdColors.PaperLine)
                        Column(Modifier.semantics(mergeDescendants = true) {}, verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(check.label, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                            Text(
                                "${if (check.ok) "✓" else "✕"} ${check.detail}",
                                style = if (check.ok) MildcrowdType.Small else MildcrowdType.SmallBold,
                                color = if (check.ok) MildcrowdColors.Ink2 else MildcrowdColors.Danger
                            )
                        }
                    }
                }
                SectionCard("Sicherungen und Überwachung") {
                    status.backups.forEach { line -> Text("• $line", style = MildcrowdType.Small, color = MildcrowdColors.Ink) }
                }
                SectionCard("Abgewehrte Bot-Versuche heute") {
                    if (status.botRefusals.isEmpty()) Text("Heute keine.", style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
                    status.botRefusals.forEach { refusal ->
                        Row(Modifier.fillMaxWidth()) {
                            Text(refusal.label, style = MildcrowdType.Small, color = MildcrowdColors.Ink, modifier = Modifier.weight(1f))
                            Text("${refusal.count}", style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
                        }
                    }
                    Text(
                        "Nur Zähler, ohne Adressen oder Konten (docs/mildcrowd-bot-protection.md).",
                        style = MildcrowdType.Caption,
                        color = MildcrowdColors.Muted
                    )
                }
                SectionCard("Letzte Läufe") {
                    if (status.heartbeats.isEmpty()) {
                        Text(
                            "Noch keine Läufe aufgezeichnet.",
                            style = MildcrowdType.Body,
                            color = MildcrowdColors.Ink2
                        )
                    }
                    status.heartbeats.forEach { beat ->
                        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                            Text(beat.key, style = MildcrowdType.BodyBold, color = MildcrowdColors.Ink)
                            Text(
                                "erfolgreich ${StaffDate.short(
                                    beat.lastSuccessAt
                                )} · fehlgeschlagen ${StaffDate.short(beat.lastFailureAt)}",
                                style = MildcrowdType.Caption,
                                color = MildcrowdColors.Ink2
                            )
                        }
                    }
                }
            }
        }
    }
}
