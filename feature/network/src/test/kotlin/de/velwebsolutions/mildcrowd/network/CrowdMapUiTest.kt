package de.velwebsolutions.mildcrowd.network

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.semantics.SemanticsActions
import androidx.compose.ui.test.SemanticsNodeInteraction
import androidx.compose.ui.test.hasAnyAncestor
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.isRoot
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.test.performTextInput
import com.github.takahirom.roborazzi.captureRoboImage
import de.velwebsolutions.mildcrowd.design.navigation.AppRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.Appearance
import de.velwebsolutions.mildcrowd.design.theme.AppearanceChoice
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdDetailScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdFormScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdFormViewModel
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdMapScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdMapViewModel
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdViewModel
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdsScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdsViewModel
import de.velwebsolutions.mildcrowd.testing.MainDispatcherRule
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/** The Karte der Crowds, the region on the crowd page and in its form, at 360 dp (light and dark). */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h740dp")
class CrowdMapUiTest {
    @get:Rule val main = MainDispatcherRule()

    @get:Rule val compose = createComposeRule()

    private val navigator = Navigator()
    private val models = ViewModelKeeper()
    private var screen by mutableStateOf<@Composable () -> Unit>({})

    @After
    fun clear() {
        models.clear()
        Appearance.show(AppearanceChoice.Automatic)
    }

    private fun show(content: @Composable () -> Unit) {
        screen = content
        compose.setContent { MildcrowdTheme { CompositionLocalProvider(LocalNavigator provides navigator) { screen() } } }
    }

    private fun switchTo(content: @Composable () -> Unit) {
        screen = content
        compose.waitForIdle()
    }

    private fun SemanticsNodeInteraction.tap() = performSemanticsAction(SemanticsActions.OnClick)

    private fun capture(name: String) {
        compose.waitForIdle()
        val roots = compose.onAllNodes(isRoot())
        roots[roots.fetchSemanticsNodes().size - 1].captureRoboImage("build/outputs/roborazzi/network-$name.png")
    }

    private fun waitFor(text: String) =
        compose.waitUntil(5_000) { compose.onAllNodes(hasText(text, substring = true)).fetchSemanticsNodes().isNotEmpty() }

    private fun opened(route: AppRoute) = MainTab.entries.any { route in navigator.stack(it) }

    @Test
    fun theMapListsTheCrowdsOfATile() {
        val network = StubbedNetwork(
            stub("GET", "network/regions", regionsJson()),
            stub(
                "GET",
                "network/regions/BY/crowds",
                """{"data":[${regionCrowdJson("isar", "München")},${regionCrowdJson("alm", null)}]}"""
            ),
            stub("GET", "network/regions/HB/crowds", """{"data":[]}"""),
            stub("GET", "network/regions/SN/crowds", """{"message":"Server Error"}""", 500)
        )
        val model = models.keep(CrowdMapViewModel(null, network.repository))
        show { CrowdMapScreen(null, model) }
        waitFor("Wähle ein Bundesland")
        capture("crowd-map")
        compose.onNode(hasContentDescription("Bayern: 3 Crowds")).tap()
        waitFor("Crowds in Bayern")
        waitFor(" · München")
        capture("crowd-map-bayern")
        compose.onNodeWithTag("network.page").performScrollToNode(hasTestTag("map.crowd.isar"))
        compose.onNodeWithTag("map.crowd.isar").tap()
        assertTrue(opened(NetworkRoute.Crowd("isar")))
        compose.onNode(hasContentDescription("Bremen: 0 Crowds")).tap()
        waitFor("In Bremen hat noch keine Crowd ihr Zuhause angegeben.")
        compose.onNode(hasContentDescription("Sachsen: 2 Crowds")).tap()
        waitFor("Server Error")
        Appearance.show(AppearanceChoice.Dark)
        compose.onNode(hasContentDescription("Bayern: 3 Crowds")).tap()
        waitFor("Crowds in Bayern")
        capture("crowd-map-dark")
    }

    @Test
    fun theCrowdShowsItsRegionAndTheFormChangesIt() {
        val crowd = """
            {"data":{"crowd":${regionCrowdJson("isar", "München")},"url":"http://localhost/crowds/isar","house_rules":[],"hosts":[],
            "is_member":false,"is_host":false,"can_follow":true,"can_post":false}}
        """.trimIndent()
        val network = StubbedNetwork(
            stub("GET", "network/crowds/isar", crowd),
            stub("GET", "network/crowds/isar/posts", pageJson()),
            stub("GET", "network/crowds/isar/collections", """{"data":{"can_create":false,"collections":[]}}"""),
            stub("GET", "network/crowds/isar/edit", editFormJson()),
            stub(
                "PUT",
                "network/crowds/isar",
                """{"message":"x","errors":{"region":["Bitte wähle ein Bundesland aus der Liste."]}}""",
                422
            ),
            stub("GET", "network/crowds", Fixtures.crowds)
        )
        val page = models.keep(CrowdViewModel("isar", network.repository))
        show { CrowdDetailScreen("isar", page) }
        waitFor("Bayern · München")
        compose.onNodeWithTag("crowd.region").tap()
        assertTrue(opened(NetworkRoute.CrowdMap("BY")))

        val form = models.keep(CrowdFormViewModel("isar", network.repository))
        switchTo { CrowdFormScreen("isar", form) }
        compose.waitUntil(5_000) { form.state.value.options != null }
        compose.onNodeWithTag("network.page").performScrollToNode(hasTestTag("crowdForm.regionFields"))
        capture("crowd-form-region")
        compose.onNode(hasSetTextAction() and hasAnyAncestor(hasTestTag("crowdForm.city"))).performTextInput("Nord ")
        compose.onNodeWithTag("crowdForm.region").performClick()
        compose.onNodeWithText("Bremen").performClick()
        compose.waitUntil(5_000) { form.state.value.input.region == "HB" }
        compose.onNodeWithTag("crowdForm.region").performClick()
        compose.onNodeWithText("Keine Region").performClick()
        compose.waitUntil(5_000) { form.state.value.input.region == null }
        compose.onNodeWithTag("crowdForm.city").assertDoesNotExist()
        compose.onNodeWithTag("network.page").performScrollToNode(hasTestTag("crowdForm.save"))
        compose.onNodeWithTag("crowdForm.save").tap()
        waitFor("Bitte wähle ein Bundesland aus der Liste.")

        val overview = models.keep(CrowdsViewModel(network.repository))
        switchTo { CrowdsScreen(overview) }
        compose.waitUntil(5_000) { compose.onAllNodes(hasTestTag("crowds.map")).fetchSemanticsNodes().isNotEmpty() }
        compose.onNodeWithTag("crowds.map").tap()
        assertTrue(opened(NetworkRoute.CrowdMap()))

        val reader = StubbedNetwork(stub("GET", "network/crowds", Fixtures.crowds.replace("\"can_create\": true", "\"can_create\": false")))
        val readOnly = models.keep(CrowdsViewModel(reader.repository))
        switchTo { CrowdsScreen(readOnly) }
        compose.waitUntil(5_000) { readOnly.directory.value.valueOrNull?.canCreate == false }
        compose.onNodeWithTag("crowds.create").assertDoesNotExist()
        compose.onNodeWithTag("crowds.map").assertExists()
    }
}
