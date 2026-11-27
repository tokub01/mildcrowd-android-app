package de.velwebsolutions.mildcrowd.chat.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.chat.api.ChatAvatar
import de.velwebsolutions.mildcrowd.chat.api.ChatConversation
import de.velwebsolutions.mildcrowd.design.components.AvatarView
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors

/**
 * The avatar of someone in the chat (ui/avatar.js `avatarElement`), the network's [AvatarView]: the photo where the
 * server sends one, otherwise the initials in the chosen colour. Decorative – the name is always written next to it.
 */
@Composable
internal fun ChatAvatarView(avatar: ChatAvatar, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    AvatarView(avatar.initials, avatar.avatarColor, modifier, photoUrl = avatar.photoUrl, size = size)
}

/**
 * The avatar of a conversation (ui/avatar.js `conversationAvatar`): the other person (1:1 chat or a group with one other
 * member), the first two other members overlapping (group), or the "?" avatar when nobody else is in it.
 */
@Composable
internal fun ConversationAvatar(conversation: ChatConversation, myId: String, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    val others = conversation.others(myId)
    if (others.size < 2) {
        ChatAvatarView(others.firstOrNull()?.shownAvatar ?: ChatAvatar.Unknown, modifier, size)
        return
    }
    val small = size * 0.7f
    Box(modifier.size(size).clearAndSetSemantics {}) {
        others.take(2).forEachIndexed { index, member ->
            ChatAvatarView(
                member.shownAvatar,
                Modifier
                    .align(if (index == 0) Alignment.TopStart else Alignment.BottomEnd)
                    .border(2.dp, MildcrowdColors.Paper, CircleShape),
                small
            )
        }
    }
}
