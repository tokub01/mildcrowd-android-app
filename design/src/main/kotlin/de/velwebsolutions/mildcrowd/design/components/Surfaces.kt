package de.velwebsolutions.mildcrowd.design.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType

/** The shadow under paper cards (the web's `--shadow-soft`); on the dark ground it does not show. */
private val CardShadow = Color(0xFF171A2E)

/** A paper card with the web's border and radius, resting slightly above the ground. */
@Composable
fun CrowdCard(modifier: Modifier = Modifier, padding: Int = 16, content: @Composable ColumnScope.() -> Unit) {
    val shape = RoundedCornerShape(MildcrowdRadius.S.dp)
    Column(
        modifier
            .fillMaxWidth()
            .shadow(6.dp, shape, clip = false, ambientColor = CardShadow, spotColor = CardShadow)
            .background(MildcrowdColors.Paper, shape)
            .border(1.dp, MildcrowdColors.PaperLine, shape)
            .padding(padding.dp),
        content = content
    )
}

/** Small uppercase label above a heading (`o-eyebrow`, on night `o-eyebrow-night`). */
@Composable
fun Eyebrow(text: String, modifier: Modifier = Modifier, onNight: Boolean = false) {
    Text(
        text.uppercase(),
        style = MildcrowdType.Eyebrow.copy(letterSpacing = 0.14.em),
        color = if (onNight) MildcrowdColors.SunOnNight else MildcrowdColors.SunText,
        modifier = modifier
    )
}

/** A count badge (unread chats, hints); nothing for zero. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    if (count <= 0) return
    Box(
        modifier
            .defaultMinSize(minWidth = 20.dp, minHeight = 20.dp)
            .background(MildcrowdColors.Sun, CircleShape)
            .padding(horizontal = 6.dp)
            .semantics { contentDescription = "$count neu" },
        contentAlignment = Alignment.Center
    ) {
        Text(
            if (count >
                99
            ) {
                "99+"
            } else {
                "$count"
            },
            style = MildcrowdType.Caption.copy(fontWeight = FontWeight.Bold),
            color = MildcrowdColors.SunInk
        )
    }
}

/** The sand page background used by every screen. */
fun Modifier.mildcrowdScreenBackground(): Modifier = background(MildcrowdColors.Sand)
