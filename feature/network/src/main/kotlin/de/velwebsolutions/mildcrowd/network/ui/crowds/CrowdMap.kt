package de.velwebsolutions.mildcrowd.network.ui.crowds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.min
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.CrowdSummary
import de.velwebsolutions.mildcrowd.network.api.MapRegion
import de.velwebsolutions.mildcrowd.network.data.NetworkRepository
import de.velwebsolutions.mildcrowd.network.ui.NetworkViewModel
import de.velwebsolutions.mildcrowd.network.ui.apiError
import de.velwebsolutions.mildcrowd.network.ui.attempt
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.membersLabel
import de.velwebsolutions.mildcrowd.network.ui.retry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** „Karte der Crowds“ (/app/crowds/karte?region=BY): the Bundesländer with their crowds, and the crowds of the chosen one. */
@HiltViewModel(assistedFactory = CrowdMapViewModel.Factory::class)
class CrowdMapViewModel @AssistedInject constructor(@Assisted region: String?, private val repository: NetworkRepository) :
    NetworkViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(region: String?): CrowdMapViewModel
    }

    private val mutableRegions = MutableStateFlow<Loadable<List<MapRegion>>>(Loadable.Idle)

    /** All 16 Bundesländer in tile order. */
    val regions: StateFlow<Loadable<List<MapRegion>>> = mutableRegions.asStateFlow()
    private val mutableChosen = MutableStateFlow(region)

    /** The chosen Bundesland's code. */
    val chosen: StateFlow<String?> = mutableChosen.asStateFlow()
    private val mutableCrowds = MutableStateFlow<Loadable<List<CrowdSummary>>>(Loadable.Idle)

    /** The crowds of the chosen Bundesland. */
    val crowds: StateFlow<Loadable<List<CrowdSummary>>> = mutableCrowds.asStateFlow()

    suspend fun load() {
        mutableRegions.load { repository.regions() } ?: return
        chosen.value?.let { choose(it) }
    }

    suspend fun loadIfNeeded() {
        if (regions.value is Loadable.Idle) load()
    }

    /** A tile: lists that Bundesland's crowds. */
    suspend fun choose(code: String) {
        mutableChosen.value = code
        mutableCrowds.value = Loadable.Loading
        val result = attempt { repository.regionCrowds(code) }
        mutableCrowds.value = result.apiError?.let { Loadable.Failed(it) } ?: Loadable.Loaded(result.getOrThrow())
    }
}

/** The name of Bundesland [code] among [regions]; the code itself when it is unknown. */
fun regionName(regions: List<MapRegion>, code: String): String = regions.firstOrNull { it.code == code }?.name ?: code

/** „Karte der Crowds“ (/app/crowds/karte). */
@Composable
fun CrowdMapScreen(
    region: String?,
    viewModel: CrowdMapViewModel = hiltViewModel<CrowdMapViewModel, CrowdMapViewModel.Factory>(
        key = "map-$region",
        creationCallback = { it.create(region) }
    )
) {
    val regions by viewModel.regions.collectAsStateWithLifecycle()
    val chosen by viewModel.chosen.collectAsStateWithLifecycle()
    val crowds by viewModel.crowds.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    LaunchedEffect(Unit) { viewModel.loadIfNeeded() }
    MildcrowdScreen(title = "Karte", error = error?.userMessage, onDismissError = viewModel::dismissError) {
        LoadableView(regions, onRetry = viewModel.retry(viewModel::load)) { tiles ->
            NetworkPage(onRefresh = viewModel::load) {
                block("header") {
                    PageHeader(
                        "Karte der Crowds",
                        eyebrow = "Crowds",
                        lead = "Wo Crowds zu Hause sind – so, wie ihre Gastgeber:innen es angeben. Eine selbst gezeichnete Übersicht " +
                            "nach Bundesländern, ohne Kartendienst und ohne genaue Orte."
                    )
                }
                block("map") {
                    Panel(spacing = 8) {
                        RegionTiles(tiles, chosen) { code -> viewModel.launch { viewModel.choose(code) } }
                        Text(
                            "Schematisch, nicht maßstabsgetreu. Tippe auf ein Bundesland.",
                            style = MildcrowdType.Small,
                            color = MildcrowdColors.Muted,
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        )
                    }
                }
                block("list") { RegionCrowds(chosen?.let { regionName(tiles, it) }, crowds) }
                block("hint") {
                    SideCard("Deine Crowd auf der Karte", tone = PanelTone.Dune) {
                        Text(
                            "Gastgeber:innen tragen unter „Bearbeiten“ ein Bundesland und, wenn sie mögen, einen Ort ein. Mehr nicht: " +
                                "keine Adressen, keine Koordinaten.",
                            style = MildcrowdType.Body,
                            color = MildcrowdColors.Ink2
                        )
                    }
                }
            }
        }
    }
}

/** The 5 × 5 grid of tiles: the chosen one in night, one with crowds in spring, one without pale. */
@Composable
internal fun RegionTiles(regions: List<MapRegion>, chosen: String?, onChoose: (String) -> Unit) {
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(1f)
            .semantics { contentDescription = "Bundesländer mit der Zahl ihrer Crowds" }
            .testTag("map.tiles"),
        contentAlignment = Alignment.TopCenter
    ) {
        val gap = 6.dp
        val size: Dp = (min(maxWidth, 520.dp) - gap * 4) / 5
        Box(Modifier.widthIn(max = size * 5 + gap * 4).fillMaxWidth()) {
            regions.forEach { region ->
                Tile(
                    region,
                    region.code == chosen,
                    size,
                    Modifier.offset(x = (size + gap) * region.tile.x, y = (size + gap) * region.tile.y)
                ) {
                    onChoose(region.code)
                }
            }
        }
    }
}

@Composable
private fun Tile(region: MapRegion, active: Boolean, size: Dp, modifier: Modifier, onClick: () -> Unit) {
    val filled = region.crowdsCount > 0
    val fill = when {
        active -> MildcrowdColors.Night
        filled -> MildcrowdColors.Spring
        else -> MildcrowdColors.Dune
    }
    val text = when {
        active -> MildcrowdColors.OnNight
        filled -> MildcrowdColors.SpringInk
        else -> MildcrowdColors.Ink2
    }
    val shape = RoundedCornerShape(size * 0.19f)
    Column(
        modifier
            .size(size)
            .background(fill, shape)
            .border(2.dp, if (filled || active) Color.Transparent else MildcrowdColors.DuneLine, shape)
            .clickable(onClick = onClick)
            .testTag("map.${region.code}")
            .clearAndSetSemantics {
                contentDescription = region.accessibleName
                role = Role.Button
                selected = active
            },
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(region.code, style = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = MildcrowdType.Body.fontSize), color = text)
        Text("${region.crowdsCount}", style = MildcrowdType.BodyBold, color = text)
    }
}

/** The crowds of the chosen Bundesland, or the hint to choose one. */
@Composable
private fun RegionCrowds(name: String?, crowds: Loadable<List<CrowdSummary>>) {
    val navigator = LocalNavigator.current
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        if (name == null) {
            Text("Wähle ein Bundesland, um seine Crowds zu sehen.", style = MildcrowdType.Body, color = MildcrowdColors.Ink2)
            return@Column
        }
        SectionTitle("Crowds in $name")
        when (crowds) {
            is Loadable.Failed -> Text(crowds.error.userMessage, style = MildcrowdType.Body, color = MildcrowdColors.Danger)

            is Loadable.Loaded -> {
                if (crowds.value.isEmpty()) DashedNote("In $name hat noch keine Crowd ihr Zuhause angegeben.")
                crowds.value.forEach { crowd ->
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .background(MildcrowdColors.Paper, RoundedCornerShape(MildcrowdRadius.S.dp))
                            .clickable(role = Role.Button) { navigator.open(NetworkRoute.Crowd(crowd.slug)) }
                            .padding(16.dp)
                            .testTag("map.crowd.${crowd.slug}"),
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text(
                            buildAnnotatedString {
                                withStyle(SpanStyle(fontWeight = FontWeight.Bold)) { append(crowd.name) }
                                crowd.city?.takeIf { it.isNotBlank() }?.let { city ->
                                    withStyle(SpanStyle(color = MildcrowdColors.Muted)) { append(" · $city") }
                                }
                            },
                            style = MildcrowdType.Body,
                            color = MildcrowdColors.Night
                        )
                        Text(crowd.description, style = MildcrowdType.Small, color = MildcrowdColors.Ink2, maxLines = 2)
                        Text(membersLabel(crowd.membersCount ?: 0), style = MildcrowdType.Small, color = MildcrowdColors.Muted)
                    }
                }
            }

            else -> CircularProgressIndicator(color = MildcrowdColors.Night, modifier = Modifier.padding(8.dp))
        }
    }
}
