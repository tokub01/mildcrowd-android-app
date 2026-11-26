package de.velwebsolutions.oasis.network

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import de.velwebsolutions.oasis.core.network.DataEnvelope
import de.velwebsolutions.oasis.core.network.OasisJson
import de.velwebsolutions.oasis.design.components.LocalScreenChrome
import de.velwebsolutions.oasis.design.components.ScreenChrome
import de.velwebsolutions.oasis.design.navigation.LocalNavigator
import de.velwebsolutions.oasis.design.navigation.Navigator
import de.velwebsolutions.oasis.design.theme.OasisColors
import de.velwebsolutions.oasis.design.theme.OasisTheme
import de.velwebsolutions.oasis.design.theme.OasisType
import de.velwebsolutions.oasis.network.api.SwipeDeck
import de.velwebsolutions.oasis.network.ui.feeds.SourceScreen
import de.velwebsolutions.oasis.network.ui.feeds.SourceViewModel
import de.velwebsolutions.oasis.network.ui.feeds.SwipeContent
import de.velwebsolutions.oasis.network.ui.oases.OasesScreen
import de.velwebsolutions.oasis.network.ui.oases.OasesViewModel
import de.velwebsolutions.oasis.network.ui.oases.OasisDetailScreen
import de.velwebsolutions.oasis.network.ui.oases.OasisViewModel
import de.velwebsolutions.oasis.testing.MemoryOnboardingCardStore
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.After
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The phone screenshots of the Play listing (1080 × 1920, app/src/main/play/listings/de-DE/graphics/phone-screenshots):
 * real screens with friendly sample content under a short headline. `./gradlew :feature:network:testDebugUnitTest
 * --tests '*StoreScreenshotsTest*'` writes them to feature/network/build/outputs/roborazzi/store-*.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshotsTest {
    @get:Rule
    val compose = createComposeRule()

    private val navigator = Navigator()
    private val models = ViewModelKeeper()

    @After
    fun clear() = models.clear()

    /** The screen under a headline on the accent colour, like a phone held up in the listing. */
    private fun shoot(name: String, headline: String, line: String, waitFor: String, screen: @Composable () -> Unit) {
        compose.setContent {
            OasisTheme {
                CompositionLocalProvider(LocalNavigator provides navigator, LocalScreenChrome provides ScreenChrome()) {
                    Column(
                        Modifier.fillMaxSize().background(OasisColors.Sun).padding(top = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            headline,
                            Modifier.padding(horizontal = 26.dp),
                            style = OasisType.display(28).copy(lineHeight = 32.sp),
                            color = OasisColors.SunInk
                        )
                        Text(line, Modifier.padding(horizontal = 26.dp), style = OasisType.Lead, color = OasisColors.SunInk)
                        Box(
                            Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp).fillMaxWidth().weight(1f)
                                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(OasisColors.Sand)
                        ) { screen() }
                    }
                }
            }
        }
        compose.settle { compose.onAllNodes(hasText(waitFor, substring = true)).fetchSemanticsNodes().isNotEmpty() }
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/store-$name.png")
    }

    @Test
    fun `1 the Start`() {
        val network = StubbedNetwork(
            stub(
                "GET",
                "network/source",
                pageJson(
                    story(
                        1,
                        LENA,
                        "Handy blieb heute am See in der Tasche. Einfach nur Wasser, Wind und ein Buch. Herrlich.",
                        minutesAgo = 12,
                        reactions = true
                    ),
                    story(
                        2,
                        JONAS,
                        "Samstag Setzling-Tausch im Hof! Wer bringt Tomaten mit? Ich hab Basilikum und Minze übrig.",
                        60,
                        GARDEN
                    ),
                    story(3, AYLIN, "Endlich wieder Zeit für die Kamera. Heute früh am Kanal, alles voller Nebel.", 180, PHOTO),
                    story(4, BEN, "Danke für die vielen Tipps zum Sauerteig – das dritte Brot ist was geworden.", 300),
                    context = """{"show_all":false,"follows_anyone":true,"can_post":true,
                        |"week":{"followers":2,"reactions":9,"karawane":3},"oases":[]}
                    """.trimMargin()
                )
            )
        )
        val model = models.keep(
            SourceViewModel(network.repository, network.session, MemoryOnboardingCardStore(Instant.now().plus(30, ChronoUnit.DAYS)))
        )
        shoot("1-start", "Deine Leute.\nDer Reihe nach.", "Ohne Werbung, ohne Algorithmus.", "Handy blieb heute") { SourceScreen(model) }
    }

    @Test
    fun `2 the end of the news`() {
        val deck = OasisJson.snake.decodeFromString(DataEnvelope.serializer(SwipeDeck.serializer()), Fixtures.swipe).data
        shoot("2-ende", "Und dann ist\nauch mal gut.", "Mildcrowd hat ein Ende – versprochen.", "Du bist durch") {
            SwipeContent(deck.copy(chapters = emptyList())) { _, _ -> }
        }
    }

    @Test
    fun `3 the crowds`() {
        val network = StubbedNetwork(stub("GET", "network/oases", CROWDS))
        val model = models.keep(OasesViewModel(network.repository))
        shoot("3-crowds", "Crowds für alles,\nwas dich interessiert.", "Kleine Runden statt großer Bühne.", "Fotografie") {
            OasesScreen(model)
        }
    }

    @Test
    fun `4 a crowd`() {
        val summary =
            """{"id": 11, "slug": "fotografie", "name": "Fotografie", "description": "Handyfoto oder Analogkamera, egal: Bilder zeigen, über Technik quatschen, voneinander lernen.", "color": "night", "visibility": "members", "banner_url": null, "members_count": 214, "role": "member"}"""
        val network = StubbedNetwork(
            stub(
                "GET",
                "network/oases/fotografie",
                """{"data": {"oasis": $summary, "url": "http://localhost/crowds/fotografie", "house_rules": ["Zeig deine Fotos und schreib dazu, was dir daran gefällt.", "Kritik nur, wenn jemand danach fragt – und dann nett und konkret."], "hosts": [], "is_member": true, "is_host": false, "can_follow": false, "can_post": true}}"""
            ),
            stub(
                "GET",
                "network/oases/fotografie/posts",
                pageJson(
                    story(
                        21,
                        AYLIN,
                        "Welches Foto aus den letzten Wochen magst du selbst am liebsten? Bei mir: Nebel über dem Kanal, 6:10 Uhr.",
                        40,
                        PHOTO
                    ),
                    story(
                        22,
                        BEN,
                        "Frage an die Runde: Entwickelt ihr selbst oder gebt ihr ab? Ich überlege, mir einen Wechselsack zu holen.",
                        150,
                        PHOTO
                    ),
                    story(23, LENA, "Mein erster Film ist zurück – 24 von 36 Bildern sind was geworden. Ich bin stolz!", 400, PHOTO)
                )
            ),
            stub("GET", "network/oases/fotografie/members", """{"data":{"members":[],"hidden_members":0,"own_list_audience":"members"}}""")
        )
        val model = models.keep(OasisViewModel("fotografie", network.repository))
        shoot(
            "4-crowd",
            "Echte Gespräche\nstatt Likes jagen.",
            "Wie viele reagiert haben, sieht nur, wer geschrieben hat.",
            "Welches Foto"
        ) {
            OasisDetailScreen("fotografie", model)
        }
    }

    private companion object {
        val LENA = person(12, "lena", "Lena Berger", "LB", "sun")
        val JONAS = person(13, "jonas", "Jonas Weber", "JW", "spring")
        val AYLIN = person(14, "aylin", "Aylin Kaya", "AK", "night")
        val BEN = person(15, "ben", "Ben Hoffmann", "BH", "dune")

        /** An author as the API sends it. */
        fun person(id: Int, username: String, name: String, initials: String, color: String) =
            """{"id":,"username":"","display_name":"","initials":"","avatar_color":"","photo_url":null}"""

        const val GARDEN = """{"id":12,"slug":"pflanzen-garten","name":"Pflanzen & Garten"}"""
        const val PHOTO = """{"id":11,"slug":"fotografie","name":"Fotografie"}"""

        private fun crowd(id: Int, slug: String, name: String, description: String, color: String, members: Int, role: String? = null) =
            """{"id": $id, "slug": "$slug", "name": "$name", "description": "$description", "color": "$color", "visibility": "members", "banner_url": null, "members_count": $members${role?.let {
                ", \"role\": \"$it\""
            }.orEmpty()}}"""

        val CROWDS = """{"data": {"mine": [${crowd(
            11,
            "fotografie",
            "Fotografie",
            "Handyfoto oder Analogkamera, egal: Bilder zeigen, über Technik quatschen, voneinander lernen.",
            "night",
            214,
            "member"
        )}, ${crowd(
            12,
            "pflanzen-garten",
            "Pflanzen & Garten",
            "Balkon, Fensterbank oder Schrebergarten: säen, pflegen, ernten – und Pflanzen retten.",
            "spring",
            168,
            "member"
        )}], "others": [${crowd(
            13,
            "buecher",
            "Bücher",
            "Was ihr gerade lest, was euch nicht loslässt und was ihr weiterempfehlen würdet.",
            "dune",
            301
        )}, ${crowd(
            14,
            "kochen-backen",
            "Kochen & Backen",
            "Rezepte, Küchenpannen und was heute bei euch auf den Tisch kommt.",
            "sun",
            276
        )}, ${crowd(
            15,
            "spiele",
            "Spiele",
            "Brettspiele, Videospiele, Rätsel – was bei euch gerade auf dem Tisch oder Bildschirm läuft.",
            "night",
            133
        )}], "can_create": true, "options": {"visibilities": [{"value": "members", "label": "Nur für Mitglieder"}], "colors": [{"value": "sun", "label": "Blau"}]}}}"""

        /** A Post card with a real author, written [minutesAgo], optionally in a crowd. */
        fun story(id: Int, author: String, body: String, minutesAgo: Long, oasis: String? = null, reactions: Boolean = false): String {
            val at = Instant.now().minus(minutesAgo, ChronoUnit.MINUTES).toString()
            return """
                {"id":$id,"url":"http://localhost/posts/$id","body":"$body","segments":[{"type":"text","text":"$body"}],"kind":"post",
                "visibility":"members","created_at":"$at","edited_at":null,"author":$author,"oasis":${oasis ?: "null"},"reply_to":null,
                "reply_unavailable":false,"images_locked":false,"images":[],"carried":null,"removed_from_oasis":false,"held":null,"stats":null,
                "conversation":null,"viewer":{"is_author":false,"reacted":$reactions,"bookmarked":false,"can_interact":true,"can_reply":true,
                "can_carry":true,"can_write_reply":true,"can_quick_reply":false,"can_edit":false,"can_delete":false,"can_remove_from_oasis":false,
                "has_muted_author":false,"has_restricted_author":false}}
            """.trimIndent()
        }
    }
}
