package de.velwebsolutions.mildcrowd.design

import android.content.ComponentName
import android.content.Intent
import android.content.IntentFilter
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.compose.ui.unit.dp
import com.github.takahirom.roborazzi.captureRoboImage
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.AvatarView
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.BannerKind
import de.velwebsolutions.mildcrowd.design.components.Bullets
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.CountBadge
import de.velwebsolutions.mildcrowd.design.components.CrowdCard
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.EmptyStateView
import de.velwebsolutions.mildcrowd.design.components.ErrorStateView
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.LoadingView
import de.velwebsolutions.mildcrowd.design.components.LocalScreenChrome
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdCheckbox
import de.velwebsolutions.mildcrowd.design.components.MildcrowdLogo
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdSwitch
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.components.Panel
import de.velwebsolutions.mildcrowd.design.components.PanelTone
import de.velwebsolutions.mildcrowd.design.components.Pill
import de.velwebsolutions.mildcrowd.design.components.PillTone
import de.velwebsolutions.mildcrowd.design.components.RelativeTimeText
import de.velwebsolutions.mildcrowd.design.components.ScreenChrome
import de.velwebsolutions.mildcrowd.design.components.SectionCard
import de.velwebsolutions.mildcrowd.design.components.SettingToggle
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.components.SkeletonLayout
import de.velwebsolutions.mildcrowd.design.components.SkeletonView
import de.velwebsolutions.mildcrowd.design.components.SupportContacts
import de.velwebsolutions.mildcrowd.design.components.TierLoadableView
import de.velwebsolutions.mildcrowd.design.components.TierLockedView
import de.velwebsolutions.mildcrowd.design.components.limitText
import de.velwebsolutions.mildcrowd.design.components.lockedTier
import de.velwebsolutions.mildcrowd.design.components.mildcrowdScreenBackground
import de.velwebsolutions.mildcrowd.design.components.openWebPage
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.MembershipRoute
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdMotion
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.GraphicsMode

@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentsTest {
    @get:Rule
    val compose = createComposeRule()

    private fun show(content: @Composable () -> Unit) = compose.setContent {
        MildcrowdTheme { Column(Modifier.verticalScroll(rememberScrollState())) { content() } }
    }

    @Test
    fun `buttons in every kind run their action`() {
        val taps = mutableListOf<String>()
        show {
            MildcrowdButtonKind.entries.forEach { kind -> MildcrowdButton(kind.name, onClick = { taps += kind.name }, kind = kind) }
            MildcrowdButton("Schmal", onClick = {}, fullWidth = false, leading = { Text("+") })
            MildcrowdButton("Aus", onClick = { taps += "Aus" }, enabled = false)
            MildcrowdButton("Lädt", onClick = { taps += "Lädt" }, loading = true)
            MildcrowdTextButton("Fertig", onClick = { taps += "Fertig" })
            MildcrowdTextButton("Gesperrt", onClick = { taps += "Gesperrt" }, enabled = false)
            AsyncMildcrowdButton("Später", onClick = { taps += "Später" })
            Text("Karte", Modifier.pressable(onClick = { taps += "Karte" }))
        }
        MildcrowdButtonKind.entries.forEach { compose.onNodeWithText(it.name).performClick() }
        listOf("Aus", "Lädt", "Gesperrt", "Fertig", "Später", "Karte").forEach {
            compose.onNodeWithText(it).performScrollTo().performClick()
        }
        compose.waitForIdle()
        compose.onNodeWithText("Später").performClick()
        compose.waitForIdle()
        assertEquals(listOf("Primary", "Accent", "Secondary", "Destructive", "Fertig", "Später", "Karte", "Später"), taps)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-buttons.png")
    }

    @Test
    fun `surfaces, pills and layout`() {
        show {
            MildcrowdLogo()
            MildcrowdLogo(wordmark = false)
            PageHeader("Mitgliedschaft", eyebrow = "Konto", lead = "Verwalte deinen Beitrag.")
            PageHeader("Ohne alles")
            PillTone.entries.forEach { Pill(it.name, tone = it) }
            Pill("Mit Symbol", icon = Icons.Outlined.Star)
            PanelTone.entries.forEach { tone -> Panel(tone = tone) { Text("Panel $tone") } }
            SideCard("Seite") { Text("Text") }
            SideCard("Nacht", tone = PanelTone.Night) { Text("Text") }
            SectionCard("Abschnitt", detail = "Erklärung", count = 3) { Text("Inhalt") }
            SectionCard("Nacht", tone = PanelTone.Night, detail = "Erklärung") { Text("Inhalt") }
            DashedNote("Noch keine Posts.")
            DashedNote("Du bist durch.", highlighted = true)
            Bullets(listOf("Eins", "Zwei"))
            CrowdCard { Text("Karte") }
            Eyebrow("Start")
            Eyebrow("Nacht", onNight = true)
            CountBadge(0)
            CountBadge(5)
            CountBadge(120)
            SupportContacts()
            Banner("Fehler")
            Banner("Info", kind = BannerKind.Info)
            Banner("Geschafft", kind = BannerKind.Success)
            EmptyStateView("Leer", message = "Nichts da.") { Text("Aktion") }
            EmptyStateView("Ohne Text")
        }
        compose.onNodeWithText("99+").assertExists()
        compose.onNodeWithContentDescription("5 neu").assertExists()
        compose.onNodeWithText("START").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-surfaces.png")
    }

    @Test
    fun `forms keep their values and show errors and hints`() {
        var text by mutableStateOf("")
        var on by mutableStateOf(false)
        show {
            MildcrowdTextField("Name", text, { text = it }, prompt = "Dein Name", hint = "So sehen dich andere.")
            MildcrowdTextField("Passwort", "geheim", {}, isSecure = true, error = "Zu kurz.")
            MildcrowdTextField("Beschreibung", "", {}, lines = 2..4)
            FieldError("Pflichtfeld")
            SettingToggle("Hinweise", on, { on = it }, detail = "Per E-Mail")
            SettingToggle("Ohne Detail", false, {}, enabled = false)
        }
        compose.onNodeWithContentDescription("Name").performTextInput("Mira")
        compose.onNodeWithContentDescription("Hinweise").performClick()
        assertEquals("Mira", text)
        assertEquals(true, on)
        compose.onNodeWithText("Zu kurz.").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-forms.png")
    }

    @Test
    fun `loading states, avatars and time`() {
        var retries = 0
        show {
            LoadableView<String>(Loadable.Loaded("Inhalt"), onRetry = {}) { Text(it) }
            LoadableView<String>(Loadable.Failed(ApiError.NotFound), onRetry = { retries++ }) { Text(it) }
            Column(Modifier.fillMaxSize()) {
                LoadableView<String>(Loadable.Loading, onRetry = {}, skeleton = SkeletonLayout.Rows) { Text(it) }
            }
            LoadableView<String>(Loadable.Idle, onRetry = {}) { Text(it) }
            SkeletonView()
            LoadingView(label = "Mildcrowd startet …")
            listOf("sun", "spring", "night", "dune").forEach { AvatarView("MS", it) }
            AvatarView("MS", "sun", photoUrl = "/bild.png", size = 64.dp)
            AuthorizedImage("https://example.org/bild.png", contentDescription = "Bild")
            RelativeTimeText(Instant.now().minusSeconds(120))
            ErrorStateView(ApiError.Transport("offline"), onRetry = { retries++ })
        }
        compose.onNodeWithText("Inhalt").assertExists()
        compose.onNodeWithText("vor 2 Min.").assertExists()
        compose.onNodeWithText("Das gibt es nicht (mehr) oder es ist nicht für dich sichtbar.").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-states.png")
        assertEquals(MildcrowdColors.Dune, AvatarTint.surface("dune"))
        assertEquals(MildcrowdColors.Sun, AvatarTint.surface("sun"))
        assertEquals(listOf("sun", "spring", "night", "dune", "sun"), (0..4).map(AvatarTint::automatic))
        assertEquals("night", AvatarTint.shown(AvatarTint.AUTOMATIC, 6))
        assertEquals("dune", AvatarTint.shown("dune", 6))
        assertEquals(listOf("sun", "spring", "night", "dune", "sun"), (0..4).map(AvatarTint::automatic))
        assertEquals("night", AvatarTint.shown(AvatarTint.AUTOMATIC, 6))
        assertEquals("dune", AvatarTint.shown("dune", 6))
        assertEquals(MildcrowdColors.Night, AvatarTint.foreground("dune"))
        assertEquals(MildcrowdColors.OnNight, AvatarTint.foreground("night"))
        assertEquals(MildcrowdColors.SunInk, AvatarTint.foreground("sun"))
        assertEquals(MildcrowdColors.SpringInk, AvatarTint.foreground("spring"))
        assertEquals(MildcrowdColors.Night, AvatarTint.foreground("unbekannt"))
        MildcrowdMotion.spec<Float>()
    }

    @Test
    fun `a feature of a higher tier shows the server's message and leads to the membership`() {
        val navigator = Navigator()
        compose.setContent {
            MildcrowdTheme {
                CompositionLocalProvider(LocalNavigator provides navigator) {
                    Column {
                        // The locked view fills the screen, so it comes first and the others still exist below it.
                        TierLoadableView<String>(
                            Loadable.Failed(ApiError.Forbidden("Das gibt es ab der Mitgliedschaft Mittendrin.")),
                            "Dein Jahrbuch",
                            onRetry = {}
                        ) { Text(it) }
                        TierLoadableView<String>(Loadable.Loaded("Zahlen"), "Deine Zahlen", onRetry = {}) { Text(it) }
                        TierLoadableView<String>(Loadable.Failed(ApiError.NotFound), "Deine Zahlen", onRetry = {}) { Text(it) }
                    }
                }
            }
        }
        compose.onNodeWithText("Zahlen").assertExists()
        compose.onNodeWithText("Noch einmal versuchen").assertExists()
        compose.onNodeWithText("Dein Jahrbuch").assertExists()
        compose.onNodeWithText("Das gibt es ab der Mitgliedschaft Mittendrin.").assertExists()
        compose.onNodeWithText("Mitgliedschaft ansehen").performClick()
        assertEquals(listOf(MembershipRoute.Overview), navigator.stack(navigator.selectedTab).toList())
    }

    @Test
    fun `fields stop at the server's limit like the web's maxlength`() {
        assertEquals("abc", limitText("abc", null))
        assertEquals("ab", limitText("abc", 2))
        assertEquals("🌴🌴", limitText("🌴🌴🌴", 2))
        assertEquals("ab", limitText("ab", 2))
        var typed = ""
        compose.setContent { MildcrowdTheme { MildcrowdTextField("Ort", typed, { typed = it }, Modifier.testTag("field"), maxLength = 3) } }
        compose.onNode(hasSetTextAction()).performTextInput("Leipzig")
        assertEquals("Lei", typed)
    }

    @Test
    fun `a locked feature names its tier and what it offers, like the web`() {
        assertEquals("Mittendrin", lockedTier("Das gibt es ab der Mitgliedschaft Mittendrin."))
        assertEquals("Mäzen", lockedTier("Das gibt es ab der Mitgliedschaft Mäzen"))
        assertNull(lockedTier("Das darfst du leider nicht."))
        compose.setContent {
            MildcrowdTheme {
                CompositionLocalProvider(LocalNavigator provides Navigator()) {
                    TierLockedView("Zeitkapseln", "Das gibt es ab der Mitgliedschaft Mittendrin.", feature = "Schreib heute einen Post.")
                }
            }
        }
        compose.onNodeWithText("AB MITTENDRIN", ignoreCase = true).assertExists()
        compose.onNodeWithText("Schreib heute einen Post.").assertExists()
        compose.onNodeWithText("Das gibt es ab der Mitgliedschaft Mittendrin.").assertDoesNotExist()
    }

    @Test
    fun `screens frame their content for each place in the app`() {
        var backs = 0
        var closes = 0
        var message by mutableStateOf<String?>("Gespeichert.")
        var error by mutableStateOf<String?>("Keine Verbindung.")
        compose.setContent {
            MildcrowdTheme {
                Column {
                    CompositionLocalProvider(
                        LocalScreenChrome provides ScreenChrome(onBack = { backs++ })
                    ) {
                        MildcrowdScreen("Gepusht", Modifier.weight(1f), message = message, error = error, onDismissMessage = {
                            message = null
                        }, onDismissError = {
                            error =
                                null
                        }) {
                            Text("Inhalt")
                        }
                    }
                    CompositionLocalProvider(LocalScreenChrome provides ScreenChrome(onClose = { closes++ })) {
                        MildcrowdScreen("Sheet", Modifier.weight(1f)) { Text("Sheet-Inhalt") }
                    }
                    CompositionLocalProvider(LocalScreenChrome provides ScreenChrome(onClose = { closes++ })) {
                        MildcrowdScreen("Schließt selbst", Modifier.weight(1f), closesItself = true, navigationAction = {
                            Text("Eigenes X")
                        }) { Text("C") }
                    }
                    CompositionLocalProvider(
                        LocalScreenChrome provides ScreenChrome(rootHeader = { actions ->
                            Row {
                                Text("Kopf")
                                actions()
                            }
                        })
                    ) {
                        MildcrowdScreen("Wurzel", Modifier.weight(1f), actions = { Text("Aktion") }) { Text("D") }
                    }
                    MildcrowdScreen(null, Modifier.weight(1f)) { Text("Ohne Leiste") }
                }
            }
        }
        compose.onNodeWithContentDescription("Zurück").performClick()
        compose.onAllNodesWithContentDescription("Schließen")[0].assertExists()
        compose.onNodeWithText("Eigenes X").assertIsDisplayed()
        compose.onNodeWithText("Kopf").assertIsDisplayed()
        compose.onNodeWithTag("status.error").assertExists()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-screens.png")
        assertEquals(1, backs)
        compose.mainClock.advanceTimeBy(9_000)
        compose.waitForIdle()
        assertEquals(null, error)
        assertEquals("the confirmation that waited behind the error gets its own seconds", "Gespeichert.", message)
        compose.onNodeWithTag("status.message").assertExists()
        compose.mainClock.advanceTimeBy(5_000)
        compose.waitForIdle()
        assertEquals(null, message)
    }

    /** Without a browser named, Android would hand a page of the app's own App Links straight back to the app. */
    @Test
    fun `web pages open in a browser with Custom Tabs, and crowd links never leave the app`() {
        val app = RuntimeEnvironment.getApplication()
        val browser = ComponentName("org.beispiel.browser", "org.beispiel.browser.Browser")
        val tabs = ComponentName("org.beispiel.browser", "org.beispiel.browser.Tabs")
        shadowOf(app.packageManager).apply {
            addActivityIfNotPresent(browser)
            addIntentFilterForActivity(
                browser,
                IntentFilter(Intent.ACTION_VIEW).apply {
                    addCategory(Intent.CATEGORY_DEFAULT)
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addDataScheme("http")
                    addDataScheme("https")
                }
            )
            addServiceIfNotPresent(tabs)
            addIntentFilterForService(tabs, IntentFilter("android.support.customtabs.action.CustomTabsService"))
        }
        openWebPage(app, "https://mildcrowd.com/app/mitgliedschaft/rechnungen/9")
        assertEquals("org.beispiel.browser", shadowOf(app).nextStartedActivity.`package`)
        openWebPage(app, "mildcrowd://unbekannt")
        assertNull("the app is the only handler of its scheme", shadowOf(app).nextStartedActivity)
    }

    @Test
    fun `web pages open in a custom tab, other links in their app`() {
        val app = RuntimeEnvironment.getApplication()
        openWebPage(app, "https://mildcrowd.com/impressum")
        openWebPage(app, "tel:116111")
        val started = generateSequence { shadowOf(app).nextStartedActivity }.map { it.dataString }.toList()
        assertEquals(true, "tel:116111" in started)
    }

    @Test
    fun `the logo, a dashed note and big figures are drawn`() {
        compose.setContent {
            MildcrowdTheme {
                Column {
                    MildcrowdLogo(size = 48.dp)
                    DashedNote("Ende", highlighted = true)
                    Text("42", style = MildcrowdType.figure())
                }
            }
        }
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-logo.png")
        compose.onNodeWithContentDescription("Mildcrowd").assertIsDisplayed()
    }

    @Test(expected = IllegalStateException::class)
    fun `screens need the app's navigator`() {
        compose.setContent { LocalNavigator.current }
    }

    @Test
    fun `images without an API client use the address as given, and pressing shrinks`() {
        compose.setContent {
            MildcrowdTheme {
                CompositionLocalProvider(
                    de.velwebsolutions.mildcrowd.design.components.LocalApiClient provides
                        de.velwebsolutions.mildcrowd.testing.StubTransport().client
                ) {
                    Column(Modifier.mildcrowdScreenBackground()) {
                        AuthorizedImage("/a.png", contentDescription = "A")
                        AuthorizedImage("ftp:x", contentDescription = "B")
                        MildcrowdButton("Drück mich", onClick = {})
                    }
                }
            }
        }
        compose.onNodeWithText("Drück mich").performTouchInput { down(center) }
        compose.waitForIdle()
        compose.onNodeWithText("Drück mich").performTouchInput { up() }
    }

    @Test
    fun `a running async button ignores more taps`() {
        var taps = 0
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        show {
            AsyncMildcrowdButton("Langsam", onClick = {
                taps++
                gate.await()
            })
        }
        compose.onNodeWithText("Langsam").performClick()
        compose.onNodeWithText("Langsam").performClick()
        gate.complete(Unit)
        compose.waitForIdle()
        assertEquals(1, taps)
    }

    @Test
    fun `plain http pages also open in a custom tab`() {
        openWebPage(RuntimeEnvironment.getApplication(), "http://localhost/hilfe")
    }

    @Test
    fun `bars without a title and sheets that close themselves`() {
        compose.setContent {
            MildcrowdTheme {
                Column {
                    CompositionLocalProvider(LocalScreenChrome provides ScreenChrome(onBack = {})) {
                        MildcrowdScreen(null, Modifier.weight(1f)) { Text("1") }
                    }
                    CompositionLocalProvider(LocalScreenChrome provides ScreenChrome(onClose = {})) {
                        MildcrowdScreen(null, Modifier.weight(1f), closesItself = true) { Text("2") }
                    }
                    MildcrowdScreen(null, Modifier.weight(1f), navigationAction = { Text("X") }) { Text("3") }
                }
            }
        }
        compose.onNodeWithText("X").assertExists()
    }

    @Test
    fun `consents, checkboxes and switches stay visible in both states`() {
        var accepted by mutableStateOf(false)
        var on by mutableStateOf(true)
        show {
            ConsentCheck("Ich akzeptiere die Nutzungsbedingungen.", accepted, { accepted = it })
            ConsentCheck("Gesperrt", false, {}, enabled = false)
            ConsentCheck(true, {}, Modifier, true) { Text("Mit eigenem Text") }
            MildcrowdCheckbox(true, null)
            MildcrowdCheckbox(false, {}, enabled = false)
            MildcrowdSwitch(on, { on = it })
            MildcrowdSwitch(false, null, enabled = false)
        }
        compose.onNodeWithText("Ich akzeptiere die Nutzungsbedingungen.").performClick()
        assertEquals(true, accepted)
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/design-consents.png")
    }
}
