package de.velwebsolutions.mildcrowd.chat

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import de.velwebsolutions.mildcrowd.chat.core.BiometricPrompter
import de.velwebsolutions.mildcrowd.chat.ui.ChatGate
import de.velwebsolutions.mildcrowd.chat.ui.ChatPanes
import de.velwebsolutions.mildcrowd.chat.ui.ChatSettingsScreen
import de.velwebsolutions.mildcrowd.chat.ui.ConversationDetailsScreen
import de.velwebsolutions.mildcrowd.chat.ui.ConversationListScreen
import de.velwebsolutions.mildcrowd.chat.ui.ConversationScreen
import de.velwebsolutions.mildcrowd.chat.ui.NewConversationScreen
import de.velwebsolutions.mildcrowd.chat.ui.ShareTargetScreen
import de.velwebsolutions.mildcrowd.chat.ui.closeDetails
import de.velwebsolutions.mildcrowd.core.auth.Session
import de.velwebsolutions.mildcrowd.core.auth.SessionState
import de.velwebsolutions.mildcrowd.core.model.Me
import de.velwebsolutions.mildcrowd.design.components.LoadingView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.navigation.ChatRoute
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn

/**
 * Entry point of the Chat module: the screen for each [ChatRoute] (mapped in the app). Web: /app/nachrichten. Every chat
 * screen sits behind the chat gate, which shows setup or unlock until the keys are open in memory.
 */
@Composable
fun ChatScreen(route: ChatRoute) {
    val root: ChatRootViewModel = hiltViewModel()
    val me by root.me.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    // The biometric prompt needs the activity; previews have none.
    val activity = LocalActivity.current
    DisposableEffect(activity) {
        activity?.let(root.prompter::attach)
        onDispose { activity?.let(root.prompter::detach) }
    }
    val member = me
    if (member == null) {
        MildcrowdScreen(title = "Nachrichten") { LoadingView() }
        return
    }
    key(member.id) {
        ChatGate {
            when (route) {
                // From 840 dp the list stays beside the open conversation.
                ChatRoute.Conversations -> ChatPanes(null, list = { ConversationListScreen() }) { ConversationScreen(it) }

                is ChatRoute.Conversation -> ChatPanes(route.id, list = { ConversationListScreen() }) { ConversationScreen(it) }

                ChatRoute.NewConversation -> NewConversationScreen(username = null)

                is ChatRoute.NewConversationWith -> NewConversationScreen(username = route.username)

                // From 840 dp the details open as a third pane beside the conversation; closing them leaves two.
                is ChatRoute.Details -> ChatPanes(
                    route.conversationId,
                    list = { ConversationListScreen() },
                    details = route.conversationId,
                    onCloseDetails = { navigator.closeDetails(route.conversationId) },
                    detailsPane = { id, onClose -> ConversationDetailsScreen(id, onClose) }
                ) { ConversationScreen(it, detailsOpen = true) }

                ChatRoute.Share -> ShareTargetScreen()

                // Settings and Devices (the web's two anchors of the same page).
                else -> ChatSettingsScreen()
            }
        }
    }
}

/** Who is signed in (the chat is bound to that member) and the biometric prompt of the visible activity. */
@HiltViewModel
class ChatRootViewModel @Inject constructor(session: Session, val prompter: BiometricPrompter) : ViewModel() {
    val me: StateFlow<Me?> = session.state
        .map { (it as? SessionState.SignedIn)?.me }
        .stateIn(viewModelScope, SharingStarted.Eagerly, session.me)
}
