package de.velwebsolutions.mildcrowd.design.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import kotlinx.coroutines.launch

/**
 * Button looks of the web app: [Primary] (Tinte), [Accent] (the accent blue), [Secondary] (outlined),
 * [Destructive] (coral text) – pill-shaped, 48 dp high.
 */
enum class MildcrowdButtonKind { Primary, Accent, Secondary, Destructive }

private fun MildcrowdButtonKind.foreground() = when (this) {
    MildcrowdButtonKind.Primary -> MildcrowdColors.OnNight
    MildcrowdButtonKind.Accent -> MildcrowdColors.SunInk
    MildcrowdButtonKind.Secondary -> MildcrowdColors.Night
    MildcrowdButtonKind.Destructive -> MildcrowdColors.Danger
}

private fun MildcrowdButtonKind.background() = when (this) {
    MildcrowdButtonKind.Primary -> MildcrowdColors.Night
    MildcrowdButtonKind.Accent -> MildcrowdColors.Sun
    MildcrowdButtonKind.Secondary, MildcrowdButtonKind.Destructive -> MildcrowdColors.Paper
}

private fun MildcrowdButtonKind.border(): BorderStroke? = when (this) {
    MildcrowdButtonKind.Secondary -> BorderStroke(1.5.dp, MildcrowdColors.Night)
    MildcrowdButtonKind.Destructive -> BorderStroke(1.5.dp, MildcrowdColors.Danger)
    else -> null
}

/**
 * A pill at one line (48 high, radius 24) that keeps its corners when larger text wraps the label onto more lines, so the
 * text never runs into round ends.
 */
private val PillShape = RoundedCornerShape(24.dp)

/** The Mildcrowd pill button. While [loading] it shows a spinner and ignores taps (AsyncButton on iOS). */
@Composable
fun MildcrowdButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    kind: MildcrowdButtonKind = MildcrowdButtonKind.Primary,
    fullWidth: Boolean = true,
    enabled: Boolean = true,
    loading: Boolean = false,
    leading: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val border = kind.border()
    Box(
        modifier = modifier
            .then(if (fullWidth) Modifier.fillMaxWidth() else Modifier)
            .defaultMinSize(minHeight = 48.dp)
            .pressEffect(interaction)
            .alpha(if (enabled) 1f else 0.45f)
            // The accent button (the web's `o-btn-primary`) carries a light shadow in its own colour.
            .shadow(
                elevation = if (kind == MildcrowdButtonKind.Accent) 8.dp else 0.dp,
                shape = PillShape,
                clip = false,
                ambientColor = MildcrowdColors.Sun,
                spotColor = MildcrowdColors.Sun
            )
            .clip(PillShape)
            .background(kind.background(), PillShape)
            .then(if (border != null) Modifier.border(border, PillShape) else Modifier)
            .clickable(interaction, indication = null, enabled = enabled && !loading, role = Role.Button, onClick = onClick)
            .padding(horizontal = 20.dp, vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier.alpha(if (loading) 0f else 1f),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                leading()
                androidx.compose.foundation.layout.Spacer(Modifier.size(8.dp))
            }
            Text(text, style = MildcrowdType.Button, color = kind.foreground(), textAlign = TextAlign.Center)
        }
        if (loading) CircularProgressIndicator(Modifier.size(20.dp), color = kind.foreground(), strokeWidth = 2.dp)
    }
}

/** A button that runs a suspending action and shows a spinner meanwhile (disabled while running). */
@Composable
fun AsyncMildcrowdButton(
    text: String,
    onClick: suspend () -> Unit,
    modifier: Modifier = Modifier,
    kind: MildcrowdButtonKind = MildcrowdButtonKind.Primary,
    fullWidth: Boolean = true,
    enabled: Boolean = true
) {
    val scope = rememberCoroutineScope()
    var running by remember { mutableStateOf(false) }
    MildcrowdButton(
        text = text,
        // The button is disabled while running (loading), so a second tap never gets here.
        onClick = {
            running = true
            scope.launch {
                try {
                    onClick()
                } finally {
                    running = false
                }
            }
        },
        modifier = modifier,
        kind = kind,
        fullWidth = fullWidth,
        enabled = enabled,
        loading = running
    )
}

/** A plain text button in Nachtwasser (toolbar actions like "Schließen", "Fertig"). */
@Composable
fun MildcrowdTextButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    color: Color = MildcrowdColors.Night
) {
    Text(
        text = text,
        style = MildcrowdType.BodyBold,
        color = color,
        modifier = modifier
            .alpha(if (enabled) 1f else 0.45f)
            .clip(CircleShape)
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp)
    )
}

/**
 * Shrinks slightly while pressed and springs back; transform only, so it stays cheap while scrolling (the iOS press
 * effect of cards and buttons).
 */
fun Modifier.pressEffect(interaction: MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.96f else 1f, label = "press")
    graphicsLayer {
        scaleX = scale
        scaleY = scale
        alpha = if (pressed) 0.85f else 1f
    }
}

/** A tappable card or tile that gives way under the finger. */
fun Modifier.pressable(onClick: () -> Unit, enabled: Boolean = true): Modifier = composed {
    val interaction = remember { MutableInteractionSource() }
    pressEffect(interaction).clickable(interaction, indication = null, enabled = enabled, role = Role.Button, onClick = onClick)
}
