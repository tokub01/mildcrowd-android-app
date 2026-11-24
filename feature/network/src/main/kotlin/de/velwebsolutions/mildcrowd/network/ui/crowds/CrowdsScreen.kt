package de.velwebsolutions.mildcrowd.network.ui.crowds

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.CrowdDirectory
import de.velwebsolutions.mildcrowd.network.api.CrowdSummary
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.OnNightButton
import de.velwebsolutions.mildcrowd.network.ui.components.OnNightKind
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.membersLabel
import de.velwebsolutions.mildcrowd.network.ui.retry

/** "Crowds" (/app/crowds): the member's crowds and all others as cards; "Crowd eröffnen" for members who may write. */
@Composable
fun CrowdsScreen(viewModel: CrowdsViewModel = hiltViewModel<CrowdsViewModel>()) {
    val directory by viewModel.directory.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    MildcrowdScreen(title = "Crowds", error = error?.userMessage, onDismissError = viewModel::dismissError) {
        LoadableView(directory, onRetry = viewModel.retry(viewModel::load)) { list ->
            NetworkPage(onRefresh = viewModel::load) { crowdsContent(list) }
        }
    }
}

/** The overview's page. */
fun LazyListScope.crowdsContent(list: CrowdDirectory) {
    block("header") {
        val navigator = LocalNavigator.current
        Column(
            Modifier.fillMaxWidth().background(MildcrowdColors.Night, RoundedCornerShape(MildcrowdRadius.L.dp)).padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Eyebrow("Crowds", onNight = true)
            Text(
                "Räume für das, was dich interessiert",
                style = MildcrowdType.display(26),
                color = MildcrowdColors.OnNight,
                modifier = Modifier.semantics { heading() }
            )
            Text(
                "Themenräume mit Gastgeber:innen und eigenen Hausregeln. Einer Crowd folgst du wie einem Menschen – dann kannst du dort auch schreiben.",
                style = MildcrowdType.Body,
                color = MildcrowdColors.OnNight2
            )
            ButtonFlow {
                if (list.canCreate) {
                    MildcrowdButton(
                        "Crowd eröffnen",
                        { navigator.open(NetworkRoute.CreateCrowd) },
                        kind = MildcrowdButtonKind.Accent,
                        fullWidth = false,
                        modifier = Modifier.testTag("crowds.create")
                    )
                }
                OnNightButton(
                    "Karte",
                    onClick = { navigator.open(NetworkRoute.CrowdMap()) },
                    modifier = Modifier.testTag("crowds.map"),
                    kind = OnNightKind.Outline
                )
            }
        }
    }
    section("mine", "Deine Crowds", list.mine, "Du folgst noch keiner Crowd.")
    section("others", "Weitere Crowds", list.others, "Noch keine weiteren Crowds.")
}

private fun LazyListScope.section(key: String, title: String, crowds: List<CrowdSummary>, empty: String) {
    block("$key.title") { SectionTitle(title) }
    if (crowds.isEmpty()) block("$key.empty") { DashedNote(empty) }
    crowds.forEach { crowd -> block("$key.${crowd.id}") { CrowdSummaryCard(crowd) } }
}

/** A crowd card of the overview: banner strip or colour surface, name, description and members. */
@Composable
fun CrowdSummaryCard(crowd: CrowdSummary) {
    val navigator = LocalNavigator.current
    Column(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(MildcrowdRadius.M.dp))
            .background(MildcrowdColors.Paper)
            .pressable({ navigator.open(NetworkRoute.Crowd(crowd.slug)) })
            .testTag("crowd.card.${crowd.slug}")
    ) {
        CrowdBanner(crowd, height = 80.dp)
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(crowd.name, style = MildcrowdType.CardTitle, color = MildcrowdColors.Night)
            Text(crowd.description, style = MildcrowdType.Small, color = MildcrowdColors.Ink2, maxLines = 3)
            Text(
                crowd.membersCount?.let(::membersLabel).orEmpty() + if (crowd.role == "host") " · du bist Gastgeber:in" else "",
                style = MildcrowdType.Caption,
                color = MildcrowdColors.Muted
            )
        }
    }
}

/** The banner of a crowd, or its colour surface with the web's soft circle. */
@Composable
fun CrowdBanner(crowd: CrowdSummary, modifier: Modifier = Modifier, height: Dp = 128.dp) {
    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .clipToBounds()
            .background(AvatarTint.surface(crowd.color))
            .clearAndSetSemantics {},
        contentAlignment = Alignment.BottomEnd
    ) {
        val banner = crowd.bannerUrl
        if (banner != null) {
            AuthorizedImage(
                banner,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxWidth().height(height)
            )
        } else {
            Box(
                Modifier
                    .offset(x = height * 0.3f, y = height * 0.5f)
                    .requiredSize(height * 1.4f)
                    .background(softCircle(crowd.color), CircleShape)
            )
        }
    }
}

/** The soft circle on a crowd colour; on the night surface a fixed line colour instead of see-through white. */
internal fun softCircle(color: String): Color = if (color == "night") {
    MildcrowdColors.NightLine
} else {
    MildcrowdColors.Paper.copy(alpha = 0.25f)
}
