package de.velwebsolutions.mildcrowd.chat.ui

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ExpandLess
import androidx.compose.material.icons.outlined.ExpandMore
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType

/** A scrolling page with the iOS padding (20), or a pane's own inset beside other panes, and spacing between its sections. */
@Composable
internal fun ChatPage(modifier: Modifier = Modifier, spacing: Int = 20, content: @Composable ColumnScope.() -> Unit) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(PaddingValues(LocalPaneInset.current ?: 20.dp)),
        verticalArrangement = Arrangement.spacedBy(spacing.dp),
        content = content
    )
}

/** A heading that opens and closes the content below it (SwiftUI's DisclosureGroup). */
@Composable
internal fun Disclosure(
    title: String,
    expanded: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 44.dp)
                .pressable(onToggle)
                .semantics { stateDescription = if (expanded) "Aufgeklappt" else "Zugeklappt" },
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(title, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night, modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            Icon(
                if (expanded) Icons.Outlined.ExpandLess else Icons.Outlined.ExpandMore,
                contentDescription = null,
                tint = MildcrowdColors.Night
            )
        }
        AnimatedVisibility(expanded) {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp), content = content)
        }
    }
}

/** A confirmation before a destructive step (SwiftUI's confirmationDialog). */
@Composable
internal fun ConfirmDialog(title: String, confirm: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        text = { Text(title, style = MildcrowdType.Body, color = MildcrowdColors.Ink) },
        confirmButton = {
            MildcrowdTextButton(
                confirm,
                {
                    onDismiss()
                    onConfirm()
                },
                color = MildcrowdColors.Danger
            )
        },
        dismissButton = { MildcrowdTextButton("Abbrechen", onDismiss) },
        containerColor = MildcrowdColors.Paper
    )
}

/** A small text button with a 44 dp touch target ("Entfernen", "Passphrase verwenden"). */
@Composable
internal fun SmallAction(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color = MildcrowdColors.Night,
    underline: Boolean = false
) {
    Text(
        text,
        style = if (underline) MildcrowdType.SmallBold.copy(textDecoration = TextDecoration.Underline) else MildcrowdType.SmallBold,
        color = color,
        modifier = modifier
            .defaultMinSize(minHeight = 44.dp)
            .pressable(onClick)
            .padding(vertical = 12.dp)
    )
}
