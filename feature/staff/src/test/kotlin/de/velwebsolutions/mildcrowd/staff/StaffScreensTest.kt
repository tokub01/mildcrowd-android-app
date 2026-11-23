package de.velwebsolutions.mildcrowd.staff

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.core.network.DataEnvelope
import de.velwebsolutions.mildcrowd.core.network.MildcrowdJson
import de.velwebsolutions.mildcrowd.core.network.Page
import de.velwebsolutions.mildcrowd.design.navigation.AccountRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MainTab
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import de.velwebsolutions.mildcrowd.design.navigation.StaffRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme
import de.velwebsolutions.mildcrowd.staff.api.CaseSummary
import de.velwebsolutions.mildcrowd.staff.api.ModerationCaseDetail
import de.velwebsolutions.mildcrowd.staff.api.ModerationOverview
import de.velwebsolutions.mildcrowd.staff.api.StatementOfReasons
import de.velwebsolutions.mildcrowd.staff.api.SuspensionList
import de.velwebsolutions.mildcrowd.staff.ui.moderation.CaseDetailActions
import de.velwebsolutions.mildcrowd.staff.ui.moderation.CaseDetailContent
import de.velwebsolutions.mildcrowd.staff.ui.moderation.CaseDetailUiState
import de.velwebsolutions.mildcrowd.staff.ui.moderation.ModerationOverviewContent
import de.velwebsolutions.mildcrowd.staff.ui.moderation.ModerationOverviewUiState
import de.velwebsolutions.mildcrowd.staff.ui.moderation.SuspensionsContent
import de.velwebsolutions.mildcrowd.staff.ui.moderation.SuspensionsUiState
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class StaffScreensTest {
    @get:Rule
    val compose = createComposeRule()

    private val navigator = Navigator().apply { selectedTab = MainTab.Profile }

    private fun <T> data(json: String, serializer: KSerializer<T>): T =
        MildcrowdJson.snake.decodeFromString(DataEnvelope.serializer(serializer), json).data

    private fun show(content: @Composable () -> Unit) {
        compose.setContent { MildcrowdTheme { CompositionLocalProvider(LocalNavigator provides navigator) { content() } } }
    }

    private val caseDetail get() = data(Fixtures.caseDetail, ModerationCaseDetail.serializer())

    @Test
    fun `the queue opens a case`() {
        val overview = data(Fixtures.overview, ModerationOverview.serializer())
        val cases = MildcrowdJson.snake.decodeFromString(Page.serializer(CaseSummary.serializer()), Fixtures.cases).data
        show { ModerationOverviewContent(ModerationOverviewUiState(Loadable.Loaded(overview), cases), {}, {}, {}) }
        compose.onNodeWithText("Warteschlange").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("staff.nav.suspensions").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("queue.case.${cases.first().id}").performScrollTo().performClick()
        assertEquals(StaffRoute.ModerationCase(cases.first().id), navigator.stack(MainTab.Profile).last())
    }

    @Test
    fun `staff without two-factor are sent to the settings`() {
        val message = "Bitte aktiviere und bestätige die Zwei-Faktor-Authentifizierung für die Moderation."
        show { ModerationOverviewContent(ModerationOverviewUiState(Loadable.Failed(ApiError.Forbidden(message))), {}, {}, {}) }
        compose.onNodeWithText("Zwei-Faktor-Anmeldung nötig").assertIsDisplayed()
        compose.onNodeWithTag("staff.twofactor.settings").performClick()
        assertEquals(AccountRoute.Settings, navigator.stack(MainTab.Profile).last())
    }

    @Test
    fun `a chosen decision is confirmed before it goes out`() {
        var decisions = 0
        val statement = StatementOfReasons("no_action", "community_rules", "Regel 1", "Tatsachen", "Begründung")
        show {
            CaseDetailContent(
                2,
                CaseDetailUiState(Loadable.Loaded(caseDetail), statement = statement),
                CaseDetailActions(onDecide = { decisions++ })
            )
        }
        compose.onNodeWithText("Wer gemeldet hat, ist im Moderationsbereich bewusst nicht sichtbar.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("case.decide").performScrollTo().performClick()
        compose.onNodeWithText(
            "Keine Maßnahme · Community-Regeln. Die betroffene Person bekommt die Begründung per E-Mail."
        ).assertIsDisplayed()
        assertEquals("nothing goes out before the confirmation", 0, decisions)
        compose.onNodeWithTag("case.decide.confirm").performClick()
        compose.waitForIdle()
        assertEquals(1, decisions)
    }

    @Test
    fun `an incomplete decision goes straight to the server`() {
        var decisions = 0
        var chosen: StatementOfReasons? = null
        show {
            CaseDetailContent(
                2,
                CaseDetailUiState(Loadable.Loaded(caseDetail)),
                CaseDetailActions(onDecide = { decisions++ }, onStatement = { chosen = it })
            )
        }
        compose.onNodeWithTag("case.outcome.no_action").performScrollTo().performClick()
        assertEquals("no_action", chosen?.outcome)
        compose.onNodeWithTag("case.decide").performScrollTo().performClick()
        compose.waitForIdle()
        assertEquals(1, decisions)
    }

    @Test
    fun `secured text evidence opens behind its disclosure`() {
        val escalated = data(Fixtures.caseEscalated, ModerationCaseDetail.serializer())
        show { CaseDetailContent(escalated.id, CaseDetailUiState(Loadable.Loaded(escalated)), CaseDetailActions()) }
        compose.onNodeWithTag("case.escalated").performScrollTo().assertIsDisplayed()
        compose.onNodeWithText("Gemeldeter Text").assertDoesNotExist()
        compose.onNodeWithText("Text anzeigen").performScrollTo().performClick()
        compose.onNodeWithText("Gemeldeter Text").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `a lock is released after confirming`() {
        val list = data(Fixtures.suspensions, SuspensionList.serializer())
        val released = mutableListOf<Int>()
        show { SuspensionsContent(SuspensionsUiState(Loadable.Loaded(list)), {}, { released += it.id }) }
        compose.onNodeWithText("Es tut mir leid.").performScrollTo().assertIsDisplayed()
        compose.onNodeWithTag("suspension.release.1").performScrollTo().performClick()
        compose.onNodeWithTag("suspension.release.confirm").performClick()
        compose.waitForIdle()
        assertEquals(listOf(1), released)
    }
}
