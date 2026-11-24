package de.velwebsolutions.mildcrowd.live

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.network.asApiError
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.live.api.QuietMode
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** The calm banner of the Ruhemodus on every screen of the tabs („Ruhemodus bis …“ with „Beenden“), like the web's. */
@Composable
fun QuietModeBanner(live: LiveCoordinator = hiltViewModel<LiveShellViewModel>().live) {
    val mode by live.quietMode.collectAsStateWithLifecycle()
    QuietModeBannerContent(mode, live::endQuietMode)
}

/**
 * While the Ruhemodus is on: its status and „Beenden“; after ending „Der Ruhemodus ist beendet.“ for a moment, after a
 * failure its reason.
 */
@Composable
internal fun QuietModeBannerContent(mode: QuietMode, onEnd: suspend () -> String?) {
    var note by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    LaunchedEffect(note) {
        if (note != null) {
            delay(NOTE_MS)
            note = null
        }
    }
    val shown = note
    if (!mode.active && shown == null) return
    FlowRow(
        modifier = Modifier
            .fillMaxWidth()
            .background(MildcrowdColors.Dune)
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .semantics { liveRegion = LiveRegionMode.Polite }
            .testTag("quietMode.banner"),
        horizontalArrangement = Arrangement.spacedBy(12.dp, Alignment.CenterHorizontally),
        itemVerticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (mode.active) QuietModeState.status(mode.until) else shown.orEmpty(),
            style = MildcrowdType.Small,
            color = MildcrowdColors.Night,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(vertical = 8.dp)
        )
        if (mode.active) {
            MildcrowdTextButton(
                "Beenden",
                {
                    busy = true
                    scope.launch {
                        note = catching { onEnd() }.getOrElse { it.asApiError().userMessage }
                        busy = false
                    }
                },
                Modifier.heightIn(min = 44.dp).testTag("quietMode.banner.end"),
                enabled = !busy
            )
        }
    }
}

/** How long the note after „Beenden“ stays. */
private const val NOTE_MS = 4_000L
