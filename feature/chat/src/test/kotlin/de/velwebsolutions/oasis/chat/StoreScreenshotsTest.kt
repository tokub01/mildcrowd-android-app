package de.velwebsolutions.oasis.chat

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.github.takahirom.roborazzi.captureRoboImage
import de.velwebsolutions.oasis.chat.api.ChatAvatar
import de.velwebsolutions.oasis.chat.api.ChatConversation
import de.velwebsolutions.oasis.chat.api.ConversationMember
import de.velwebsolutions.oasis.chat.core.ChatBackground
import de.velwebsolutions.oasis.chat.core.ChatMessageItem
import de.velwebsolutions.oasis.chat.core.ChatTimeline
import de.velwebsolutions.oasis.chat.core.LookChoice
import de.velwebsolutions.oasis.chat.ui.ConversationContent
import de.velwebsolutions.oasis.chat.ui.ConversationState
import de.velwebsolutions.oasis.chat.ui.ReactionActions
import de.velwebsolutions.oasis.chat.ui.ReactionChip
import de.velwebsolutions.oasis.chat.ui.timelineRows
import de.velwebsolutions.oasis.chatcrypto.MessageContent
import de.velwebsolutions.oasis.chatcrypto.MessageEnvelope
import de.velwebsolutions.oasis.design.navigation.LocalNavigator
import de.velwebsolutions.oasis.design.navigation.Navigator
import de.velwebsolutions.oasis.design.theme.OasisColors
import de.velwebsolutions.oasis.design.theme.OasisTheme
import de.velwebsolutions.oasis.design.theme.OasisType
import java.time.Instant
import java.time.temporal.ChronoUnit
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * The chat screenshot of the Play listing (1080 × 1920, next to the ones of feature/network's StoreScreenshotsTest):
 * a group chat under a short headline. `./gradlew :feature:chat:testDebugUnitTest --tests '*StoreScreenshotsTest*'`
 * writes it to feature/chat/build/outputs/roborazzi/store-5-chat.png.
 */
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h640dp-xxhdpi")
class StoreScreenshotsTest {
    /** The chat screens keep the first test rule: their effects run as in the app. */
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createComposeRule()

    private val now = Instant.now().truncatedTo(ChronoUnit.MINUTES)

    private val members = listOf(
        ConversationMember("1", "mira", "Mira"),
        ConversationMember("2", "jonas", "Jonas", avatar = ChatAvatar("J", "spring")),
        ConversationMember("3", "aylin", "Aylin", avatar = ChatAvatar("A", "sun"))
    )

    private fun item(signature: String, sender: String, minutesAgo: Long, text: String) = ChatMessageItem(
        signature,
        MessageEnvelope(1, "7", 1, sender, "n", "c", signature, null),
        sender,
        now.minus(minutesAgo, ChronoUnit.MINUTES),
        ChatMessageItem.State.Readable,
        text,
        MessageContent(text)
    )

    @Test
    fun `5 a group chat`() {
        val timeline = ChatTimeline(
            listOf(
                item("a", "2", 52, "Gießen wir morgen früh? Es soll warm werden."),
                item("b", "3", 47, "Ich bin ab 9 da und bringe Kaffee mit."),
                item("c", "1", 40, "Super, ich komme gegen halb zehn."),
                item("d", "2", 33, "Dann nehm ich die Kannen aus dem Keller mit."),
                item("e", "3", 12, "Und danach Frühstück im Hof?"),
                item("f", "1", 5, "Unbedingt! Ich backe heute Abend noch was.")
            )
        )
        val state = ConversationState(
            conversation = ChatConversation("7", "group", "Gartenrunde", "1", 7, 1, false, members, ""),
            title = "Gartenrunde",
            timeline = timeline,
            canSend = true,
            look = LookChoice(ChatBackground.Standard, "default"),
            reactions = mapOf("e" to listOf(ReactionChip("❤️", 2, true, "❤️ Jonas, Du"))),
            rows = timelineRows(timeline.items, 7, now, "1", now)
        )
        compose.setContent {
            OasisTheme {
                CompositionLocalProvider(LocalNavigator provides Navigator()) {
                    Column(
                        Modifier.fillMaxSize().background(OasisColors.Sun).padding(top = 34.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            "Chats, die nur\nihr lesen könnt.",
                            Modifier.padding(horizontal = 26.dp),
                            style = OasisType.display(28).copy(lineHeight = 32.sp),
                            color = OasisColors.SunInk
                        )
                        Text(
                            "Ende-zu-Ende-verschlüsselt – nicht mal wir können mitlesen.",
                            Modifier.padding(horizontal = 26.dp),
                            style = OasisType.Lead,
                            color = OasisColors.SunInk
                        )
                        Box(
                            Modifier.padding(start = 18.dp, end = 18.dp, top = 18.dp).fillMaxWidth().weight(1f)
                                .clip(RoundedCornerShape(topStart = 30.dp, topEnd = 30.dp)).background(OasisColors.Sand)
                        ) {
                            ConversationContent(
                                state = state,
                                myId = "1",
                                senderName = { id -> members.first { it.userId == id }.name },
                                onLoadOlder = {},
                                onDraftChange = {},
                                onSend = {},
                                onReport = {},
                                onAccept = {},
                                onDecline = {},
                                onBlockFlagged = {},
                                onDismissFlagged = {},
                                onNoticeSpoken = {},
                                attachments = attachmentActions(mutableListOf()),
                                senderAvatar = { id -> members.first { it.userId == id }.shownAvatar },
                                reactions = ReactionActions(
                                    onReact = { _, _ -> },
                                    onReply = {},
                                    onCancelReply = {},
                                    onQuote = { null },
                                    onPin = {},
                                    onVote = { _, _ -> },
                                    onClosePoll = {}
                                )
                            )
                        }
                    }
                }
            }
        }
        compose.onNodeWithText("Unbedingt! Ich backe heute Abend noch was.").assertExists()
        compose.mainClock.advanceTimeBy(1_000)
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("build/outputs/roborazzi/store-5-chat.png")
    }
}
