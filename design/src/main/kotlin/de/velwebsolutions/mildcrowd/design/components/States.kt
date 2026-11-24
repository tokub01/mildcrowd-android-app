package de.velwebsolutions.mildcrowd.design.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType

/** Placeholder shapes of the coming content instead of a spinner. */
enum class SkeletonLayout { Cards, Rows }

/** Shows a placeholder, the error with retry, or the content of a [Loadable]. */
@Composable
fun <T> LoadableView(
    state: Loadable<T>,
    onRetry: suspend () -> Unit,
    modifier: Modifier = Modifier,
    skeleton: SkeletonLayout? = null,
    content: @Composable (T) -> Unit
) {
    when (state) {
        is Loadable.Loaded -> content(state.value)
        is Loadable.Failed -> ErrorStateView(state.error, onRetry, modifier)
        else -> if (skeleton != null) SkeletonView(modifier, skeleton) else LoadingView(modifier)
    }
}

/** Centered spinner with "Lädt …" or its own label ("Mildcrowd startet …"). */
@Composable
fun LoadingView(modifier: Modifier = Modifier, label: String = "Lädt …") {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        CircularProgressIndicator(color = MildcrowdColors.Night)
        Spacer(Modifier.height(12.dp))
        Text(label, style = MildcrowdType.Small, color = MildcrowdColors.Muted)
    }
}

/**
 * Placeholder shapes in the layout of the coming content (heading, then Post cards or list rows), so the screen
 * does not jump when the data arrives. One opacity animation for the whole block keeps it cheap.
 */
@Composable
fun SkeletonView(modifier: Modifier = Modifier, layout: SkeletonLayout = SkeletonLayout.Cards) {
    val transition = rememberInfiniteTransition(label = "skeleton")
    val alpha by transition.animateFloat(1f, 0.55f, infiniteRepeatable(tween(900), RepeatMode.Reverse), label = "alpha")
    Column(
        modifier
            .fillMaxSize()
            .background(MildcrowdColors.Sand)
            .padding(20.dp)
            .alpha(alpha)
            .clearAndSetSemantics { contentDescription = "Lädt …" },
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Bar(90.dp, 12.dp)
        Bar(220.dp, 30.dp)
        Bar(300.dp, 14.dp)
        Spacer(Modifier.height(8.dp))
        repeat(if (layout == SkeletonLayout.Cards) 3 else 6) {
            if (layout == SkeletonLayout.Cards) {
                Column(
                    Modifier.fillMaxWidth().background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.M.dp)).padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(Modifier.size(44.dp).background(MildcrowdColors.Dune, CircleShape))
                        Spacer(Modifier.width(12.dp))
                        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                            Bar(130.dp, 14.dp)
                            Bar(90.dp, 11.dp)
                        }
                    }
                    Bar(null, 14.dp)
                    Bar(null, 14.dp)
                    Bar(180.dp, 14.dp)
                }
            } else {
                Column(
                    Modifier.fillMaxWidth().background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.S.dp))
                        .padding(horizontal = 20.dp, vertical = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Bar(160.dp, 15.dp)
                    Bar(240.dp, 12.dp)
                }
            }
        }
    }
}

@Composable
private fun Bar(width: Dp?, height: Dp) {
    Box(
        (if (width == null) Modifier.fillMaxWidth() else Modifier.width(width))
            .height(height)
            .background(MildcrowdColors.Dune, RoundedCornerShape(height / 2))
    )
}

/** Error with a retry button. */
@Composable
fun ErrorStateView(error: ApiError, onRetry: suspend () -> Unit, modifier: Modifier = Modifier) {
    Column(
        modifier.fillMaxSize().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(Icons.Outlined.WarningAmber, contentDescription = null, tint = MildcrowdColors.Danger, modifier = Modifier.size(32.dp))
        Text(error.userMessage, style = MildcrowdType.Body, color = MildcrowdColors.Ink, textAlign = TextAlign.Center)
        AsyncMildcrowdButton("Noch einmal versuchen", onRetry, kind = MildcrowdButtonKind.Secondary, fullWidth = false)
    }
}

/**
 * [LoadableView] for a feature the member's tier may not include: the server's 403 (`Das gibt es ab der Mitgliedschaft
 * Mittendrin.`) shows [TierLockedView] with [title] instead of an error with retry.
 */
@Composable
fun <T> TierLoadableView(
    state: Loadable<T>,
    title: String,
    onRetry: suspend () -> Unit,
    modifier: Modifier = Modifier,
    feature: String? = null,
    content: @Composable (T) -> Unit
) {
    val locked = state.errorOrNull as? ApiError.Forbidden
    if (locked != null) {
        TierLockedView(title, locked.userMessage, modifier, feature)
    } else {
        LoadableView(state, onRetry, modifier, content = content)
    }
}

/**
 * The tier named in the server's lock message (`TierFeatures::lockedMessage`: „Das gibt es ab der Mitgliedschaft Mittendrin.“),
 * or null for another text.
 */
fun lockedTier(message: String): String? = Regex("ab der Mitgliedschaft (.+?)\\.?$").find(message)?.groupValues?.get(1)

/**
 * Shown instead of a feature the tier does not include (the web's `x-network.tier-locked`): „Ab {Stufe}“ from the
 * server's [message], the feature's name and what it offers ([feature]; the message where none is given), and the way
 * to the membership.
 */
@Composable
fun TierLockedView(title: String, message: String, modifier: Modifier = Modifier, feature: String? = null) {
    val navigator = LocalNavigator.current
    val tier = lockedTier(message)
    Column(modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(16.dp).testTag("tier.locked")) {
        Panel(spacing = 16) {
            if (tier != null) Eyebrow("Ab $tier")
            Text(title, style = MildcrowdType.display(28), color = MildcrowdColors.Night, modifier = Modifier.semantics { heading() })
            Text(feature ?: message, style = MildcrowdType.Lead, color = MildcrowdColors.Ink2)
            MildcrowdButton("Mitgliedschaft ansehen", { navigator.open(MembershipRoute.Overview) }, fullWidth = false)
        }
    }
}

/** Friendly empty state ("Noch keine Posts …"). */
@Composable
fun EmptyStateView(
    title: String,
    modifier: Modifier = Modifier,
    message: String? = null,
    icon: ImageVector = Icons.Outlined.Inbox,
    actions: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier.fillMaxWidth().padding(32.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = null, tint = MildcrowdColors.Spring, modifier = Modifier.size(34.dp))
        Text(title, style = MildcrowdType.heading(), color = MildcrowdColors.Night, textAlign = TextAlign.Center)
        if (message != null) Text(message, style = MildcrowdType.Small, color = MildcrowdColors.Muted, textAlign = TextAlign.Center)
        Column(Modifier.padding(top = 4.dp), horizontalAlignment = Alignment.CenterHorizontally, content = actions)
    }
}

/** The kind of a [Banner]. */
enum class BannerKind { Error, Info, Success }

/** Inline banner for errors or notices inside forms and lists. */
@Composable
fun Banner(text: String, modifier: Modifier = Modifier, kind: BannerKind = BannerKind.Error) {
    val (icon, foreground, background) = when (kind) {
        BannerKind.Error -> Triple(Icons.Outlined.ErrorOutline, MildcrowdColors.Danger, MildcrowdColors.Danger.copy(alpha = 0.12f))
        BannerKind.Info -> Triple(Icons.Outlined.Info, MildcrowdColors.Ink, MildcrowdColors.Dune)
        BannerKind.Success -> Triple(Icons.Outlined.CheckCircle, MildcrowdColors.SpringText, MildcrowdColors.Spring.copy(alpha = 0.14f))
    }
    Row(
        modifier.fillMaxWidth().background(background, RoundedCornerShape(MildcrowdRadius.XS.dp)).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(icon, contentDescription = null, tint = foreground, modifier = Modifier.size(18.dp))
        Text(text, style = MildcrowdType.Small, color = foreground, modifier = Modifier.weight(1f))
    }
}
