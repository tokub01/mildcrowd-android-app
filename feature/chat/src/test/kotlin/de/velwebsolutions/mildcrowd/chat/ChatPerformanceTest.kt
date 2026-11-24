package de.velwebsolutions.mildcrowd.chat

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.junit4.createComposeRule
import de.velwebsolutions.mildcrowd.chat.api.ChatConversation
import de.velwebsolutions.mildcrowd.chat.api.ConversationMember
import de.velwebsolutions.mildcrowd.chat.core.ChatMessageItem
import de.velwebsolutions.mildcrowd.chat.core.ChatTimeline
import de.velwebsolutions.mildcrowd.chat.ui.ChatCompositions
import de.velwebsolutions.mildcrowd.chat.ui.ConversationContent
import de.velwebsolutions.mildcrowd.chat.ui.ConversationState
import de.velwebsolutions.mildcrowd.chatcrypto.MessageContent
import de.velwebsolutions.mildcrowd.chatcrypto.MessageEnvelope
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.Navigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdTheme
import java.time.Instant
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** How much of a long chat is composed again while typing (docs/mildcrowd-chat-client.md, „Chat design and gestures“). */
@RunWith(RobolectricTestRunner::class)
@Config(qualifiers = "w360dp-h740dp")
class ChatPerformanceTest {
    // The chat screens keep the first test rule: their effects (scrolling to „Neue Nachrichten“) run as in the app.
    @Suppress("DEPRECATION")
    @get:Rule
    val compose = createComposeRule()

    private val members = listOf(ConversationMember("1", "mira", "Mira"), ConversationMember("2", "jonas", "Jonas"))
    private val conversation = ChatConversation("7", "direct", null, "1", 7, 1, false, members, "")

    private fun items(count: Int) = ChatTimeline(
        (0 until count).map { index ->
            val sender = if (index % 3 == 0) "1" else "2"
            val sent = Instant.parse("2026-11-01T08:00:00Z").plusSeconds(index * 90L)
            ChatMessageItem(
                "s$index",
                MessageEnvelope(1, "7", 1, sender, "n", "c", "s$index", null),
                sender,
                sent.plusSeconds(7 * 86_400),
                ChatMessageItem.State.Readable,
                "Nachricht $index",
                MessageContent("Nachricht $index")
            )
        }
    )

    @Test
    fun typingInALongChatComposesNoBubbleAgain() {
        var state by mutableStateOf(ConversationState(conversation = conversation, timeline = items(500), canSend = true))
        // As the screen does it: the actions are made once.
        val actions = attachmentActions(mutableListOf())
        compose.setContent {
            MildcrowdTheme {
                CompositionLocalProvider(LocalNavigator provides Navigator()) {
                    ConversationContent(
                        state = state,
                        myId = "1",
                        senderName = { "Jonas" },
                        onLoadOlder = {},
                        onDraftChange = {},
                        onSend = {},
                        onReport = {},
                        onAccept = {},
                        onDecline = {},
                        onBlockFlagged = {},
                        onDismissFlagged = {},
                        onNoticeSpoken = {},
                        attachments = actions
                    )
                }
            }
        }
        compose.waitForIdle()
        val opened = ChatCompositions.bubbles
        ChatCompositions.bubbles = 0
        "Hallo Jonas, bis gleich".forEachIndexed { index, _ ->
            state = state.copy(draft = "Hallo Jonas, bis gleich".take(index + 1))
            compose.waitForIdle()
        }
        val typing = ChatCompositions.bubbles
        println("PERF opened=$opened typing=$typing")
        org.junit.Assert.assertEquals("typing composes no bubble again", 0, typing)
    }
}
