package de.velwebsolutions.mildcrowd.design

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Inbox
import androidx.compose.material3.Text
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarView
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.BannerKind
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.CountBadge
import de.velwebsolutions.mildcrowd.design.components.CrowdCard
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.EmptyStateView
import de.velwebsolutions.mildcrowd.design.components.ErrorStateView
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.LoadingView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdKeyboard
import de.velwebsolutions.mildcrowd.design.components.MildcrowdLogo
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.RelativeTimeText
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.SettingToggle
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.components.SkeletonView
import de.velwebsolutions.mildcrowd.design.components.SupportContacts
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.GraphicsMode

/** Every component with each of its optional parameters given, next to the default calls of [ComponentsTest]. */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ExplicitParametersTest {
    @get:Rule
    val compose = createComposeRule()

    @Test
    fun `components with every parameter set`() {
        val tagged = Modifier.testTag("x")
        compose.setContent {
            MildcrowdTheme {
                Column(Modifier.verticalScroll(rememberScrollState())) {
                    MildcrowdButton("A", {
                    }, tagged, MildcrowdButtonKind.Accent, fullWidth = false, enabled = true, loading = false, leading = null)
                    AsyncMildcrowdButton("B", {}, tagged, MildcrowdButtonKind.Secondary, fullWidth = false, enabled = false)
                    MildcrowdTextButton("C", {}, tagged, enabled = true, color = MildcrowdColors.Sun)
                    Text("D", Modifier.pressable(onClick = {}, enabled = false))
                    Pill("E", tagged, PillTone.Night, null)
                    MildcrowdTextField(
                        "F", "x", {}, tagged, prompt = null, error = null, hint = null, isSecure = false, lines = null,
                        keyboardOptions = MildcrowdKeyboard.Email, keyboardActions = KeyboardActions(), enabled = false
                    )
                    MildcrowdTextField("Leer", "", {}, prompt = "Hinweis im Feld")
                    MildcrowdTextField("Lang", "", {}, isSecure = true, lines = 1..3)
                    listOf(MildcrowdKeyboard.Username, MildcrowdKeyboard.OneTimeCode, MildcrowdKeyboard.NoCapitalization).forEach { _ -> }
                    FieldError("G", tagged)
                    SettingToggle("H", true, {}, tagged, detail = null, enabled = true)
                    PageHeader("I", tagged, eyebrow = null, lead = null)
                    Panel(tagged, PanelTone.Warning, spacing = 4) { Text("J") }
                    SideCard("K", tagged, PanelTone.Dune) { Text("K") }
                    SectionCard("L", tagged, detail = null, count = null, tone = PanelTone.Dune) { Text("L") }
                    DashedNote(tagged, highlighted = false) { Text("M") }
                    DashedNote("N", tagged, highlighted = false)
                    Bullets(listOf("O"), tagged)
                    AvatarView("P", "night", tagged, photoUrl = null, size = 20.dp)
                    AuthorizedImage("/q.png", "Q", tagged, ContentScale.Crop, cacheOnDisk = false)
                    RelativeTimeText(Instant.now(), tagged, MildcrowdType.Small, MildcrowdColors.Ink)
                    LoadableView(Loadable.Loaded("R"), onRetry = {}, modifier = tagged, skeleton = SkeletonLayout.Cards) { Text(it) }
                    LoadingView(tagged)
                    SkeletonView(tagged, SkeletonLayout.Cards)
                    ErrorStateView(ApiError.NotFound, {}, tagged)
                    EmptyStateView("S", tagged, message = null, icon = Icons.Outlined.Inbox) {}
                    Banner("T", tagged, BannerKind.Error)
                    CrowdCard(tagged, padding = 4) { Text("U") }
                    Eyebrow("V", tagged, onNight = false)
                    CountBadge(1, tagged)
                    MildcrowdLogo(tagged, 30.dp, wordmark = true)
                    SupportContacts(tagged)
                    Column(Modifier.fillMaxSize()) { LoadableView<String>(Loadable.Loading, {}, tagged, SkeletonLayout.Cards) { Text(it) } }
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-explicit.png")
        compose.onNodeWithTag("x", useUnmergedTree = true).let { }
    }
}
