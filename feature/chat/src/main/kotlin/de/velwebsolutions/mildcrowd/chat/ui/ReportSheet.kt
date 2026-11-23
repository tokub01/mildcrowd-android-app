package de.velwebsolutions.mildcrowd.chat.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RadioButtonChecked
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LocalApiClient
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.SupportContacts
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.WebRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType

/**
 * Dialog "Nachricht melden" (partials/report.blade.php; iOS `ReportSheet`): consent-based report of one message, then
 * thanks, the offer to block and where to find help.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun ReportSheet(model: ReportModel, onDismiss: () -> Unit) {
    val state by model.state.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    // „Meldungen“ in the Datenschutzerklärung, on the server the app talks to (datenschutz.blade.php, id="meldungen").
    val privacy = LocalApiClient.current?.absoluteUrl("/datenschutz#meldungen")?.toString()
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
        containerColor = MildcrowdColors.Sand
    ) {
        ReportContent(state, model::setCategory, model::setComment, model::send, model::block, onDismiss) {
            if (privacy != null) navigator.open(WebRoute(privacy))
        }
    }
}

@Composable
internal fun ReportContent(
    state: ReportState,
    onCategory: (String) -> Unit,
    onComment: (String) -> Unit,
    onSend: suspend () -> Unit,
    onBlock: suspend () -> Unit,
    onClose: () -> Unit,
    onPrivacy: () -> Unit = {}
) {
    Column(
        modifier = Modifier.fillMaxWidth().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text("Nachricht melden", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night, modifier = Modifier.weight(1f))
            MildcrowdTextButton(if (state.sent) "Schließen" else "Abbrechen", onClose)
        }
        if (state.sent) {
            Text(
                "Danke, dass du das meldest.",
                style = MildcrowdType.heading(21),
                color = MildcrowdColors.Night,
                modifier = Modifier.testTag("chat.report.thanks")
            )
            Text(
                "Du hast richtig gehandelt. Unser Moderationsteam schaut sich das an. Die Person erfährt nicht, wer gemeldet hat.",
                style = MildcrowdType.Body,
                color = MildcrowdColors.Ink2
            )
            if (state.blocked) {
                Text(
                    "Blockiert. Ihr könnt euch keine Nachrichten mehr schicken.",
                    style = MildcrowdType.BodyBold,
                    color = MildcrowdColors.SpringText
                )
            } else {
                AsyncMildcrowdButton("Person blockieren", onBlock, fullWidth = false)
            }
            state.error?.let { FieldError(it) }
            MildcrowdButton("Schließen", onClose, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
            SupportContacts()
        } else {
            Text(
                "Melden ist ausdrücklich erwünscht – auch wenn du dir nicht sicher bist. Diese eine Nachricht geht an unser Moderationsteam, zusammen mit Absender und Uhrzeit; andere Nachrichten aus dem Chat sieht dort niemand. Wir sagen der Person nicht, wer gemeldet hat.",
                style = MildcrowdType.Body,
                color = MildcrowdColors.Ink2
            )
            state.quote?.let {
                Text(
                    it,
                    style = MildcrowdType.Body,
                    color = MildcrowdColors.Ink,
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(MildcrowdColors.Dune, RoundedCornerShape(MildcrowdRadius.TILE.dp))
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                )
            }
            ReportModel.attachmentNote(state.attachments)?.let { note ->
                Text(
                    note,
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Ink2,
                    modifier = Modifier.testTag("chat.report.attachments")
                )
            }
            Text("Was ist das Problem?", style = MildcrowdType.SmallBold, color = MildcrowdColors.Ink)
            ReportModel.categories.forEach { category ->
                val selected = state.category == category
                val shape = RoundedCornerShape(MildcrowdRadius.CHIP.dp)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .defaultMinSize(minHeight = 44.dp)
                        .background(if (selected) MildcrowdColors.Dune else Color.Transparent, shape)
                        .border(1.dp, if (selected) MildcrowdColors.Night else MildcrowdColors.DuneLine, shape)
                        .selectable(selected, role = Role.RadioButton, onClick = { onCategory(category) })
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        if (selected) Icons.Outlined.RadioButtonChecked else Icons.Outlined.RadioButtonUnchecked,
                        contentDescription = null,
                        tint = MildcrowdColors.Night
                    )
                    Text(category, style = MildcrowdType.Small, color = MildcrowdColors.Ink)
                }
            }
            MildcrowdTextField("Möchtest du noch etwas dazu sagen? (freiwillig)", state.comment, onComment, lines = 3..10)
            state.error?.let { FieldError(it) }
            AsyncMildcrowdButton("Melden", onSend, fullWidth = false, modifier = Modifier.testTag("chat.report.submit"))
            Text(
                "Wir speichern deine Meldung als Nachweis; Einzelheiten in der Datenschutzerklärung.",
                style = MildcrowdType.Small,
                color = MildcrowdColors.Ink2
            )
            MildcrowdTextButton("Datenschutzerklärung", onPrivacy, Modifier.testTag("chat.report.privacy"))
        }
    }
}
