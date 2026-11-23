package de.velwebsolutions.mildcrowd.design.theme

import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.LocalTextSelectionColors
import androidx.compose.foundation.text.selection.TextSelectionColors
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.dp

/**
 * Material 3 with the Mildcrowd tokens of the mode shown: sand pages, paper cards, night (Tinte) for primary actions,
 * the accent blue as accent.
 */
private fun mildcrowdColorScheme(dark: Boolean): ColorScheme {
    val scheme = if (dark) darkColorScheme() else lightColorScheme()
    return scheme.copy(
        primary = MildcrowdColors.Night,
        onPrimary = MildcrowdColors.OnNight,
        primaryContainer = MildcrowdColors.Dune,
        onPrimaryContainer = MildcrowdColors.Night,
        secondary = MildcrowdColors.Spring,
        onSecondary = MildcrowdColors.SpringInk,
        secondaryContainer = MildcrowdColors.Dune,
        onSecondaryContainer = MildcrowdColors.Night,
        tertiary = MildcrowdColors.Sun,
        onTertiary = MildcrowdColors.SunInk,
        background = MildcrowdColors.Sand,
        onBackground = MildcrowdColors.Ink,
        surface = MildcrowdColors.Paper,
        onSurface = MildcrowdColors.Ink,
        surfaceVariant = MildcrowdColors.Dune,
        onSurfaceVariant = MildcrowdColors.Ink2,
        surfaceContainer = MildcrowdColors.Paper,
        surfaceContainerLow = MildcrowdColors.Paper,
        surfaceContainerHigh = MildcrowdColors.Paper,
        surfaceContainerHighest = MildcrowdColors.Dune,
        surfaceContainerLowest = MildcrowdColors.Field,
        inverseSurface = MildcrowdColors.Night,
        inverseOnSurface = MildcrowdColors.OnNight,
        error = MildcrowdColors.Danger,
        onError = MildcrowdColors.OnDanger,
        outline = MildcrowdColors.FieldLine,
        outlineVariant = MildcrowdColors.DuneLine
    )
}

private val MildcrowdTypography = Typography(
    displayLarge = MildcrowdType.display(36),
    displayMedium = MildcrowdType.display(32),
    displaySmall = MildcrowdType.display(30),
    headlineLarge = MildcrowdType.display(28),
    headlineMedium = MildcrowdType.heading(24),
    headlineSmall = MildcrowdType.heading(21),
    titleLarge = MildcrowdType.heading(21),
    titleMedium = MildcrowdType.BodyBold,
    titleSmall = MildcrowdType.SmallBold,
    bodyLarge = MildcrowdType.Body,
    bodyMedium = MildcrowdType.Small,
    bodySmall = MildcrowdType.Caption,
    labelLarge = MildcrowdType.Button,
    labelMedium = MildcrowdType.SmallBold,
    labelSmall = MildcrowdType.Caption
)

private val MildcrowdShapes = Shapes(
    extraSmall = RoundedCornerShape(MildcrowdRadius.CHIP.dp),
    small = RoundedCornerShape(MildcrowdRadius.XS.dp),
    medium = RoundedCornerShape(MildcrowdRadius.TILE.dp),
    large = RoundedCornerShape(MildcrowdRadius.M.dp),
    extraLarge = RoundedCornerShape(MildcrowdRadius.L.dp)
)

/** The theme of the app: light or dark as [Appearance] says (the system's mode unless this device chose one). */
@Composable
fun MildcrowdTheme(content: @Composable () -> Unit) {
    val dark = Appearance.isDark
    MaterialTheme(colorScheme = mildcrowdColorScheme(dark), typography = MildcrowdTypography, shapes = MildcrowdShapes) {
        CompositionLocalProvider(
            LocalTextStyle provides MildcrowdType.Body.copy(color = MildcrowdColors.Ink),
            LocalTextSelectionColors provides TextSelectionColors(MildcrowdColors.Spring, MildcrowdColors.Spring.copy(alpha = 0.3f)),
            content = content
        )
    }
}

/** The one motion of Mildcrowd: short and settling, for state changes, insertions and removals. */
object MildcrowdMotion {
    fun <T> spec(): FiniteAnimationSpec<T> = spring(dampingRatio = 0.9f, stiffness = 900f)
}
