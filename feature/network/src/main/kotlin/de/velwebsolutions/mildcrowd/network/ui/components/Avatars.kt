package de.velwebsolutions.mildcrowd.network.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.clearAndSetSemantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import de.velwebsolutions.mildcrowd.design.components.AuthorizedImage
import de.velwebsolutions.mildcrowd.design.components.AvatarTint
import de.velwebsolutions.mildcrowd.design.components.AvatarView
import de.velwebsolutions.mildcrowd.design.components.pressable
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.CrowdSummary
import de.velwebsolutions.mildcrowd.network.api.Member

/** The avatar of a member as the server describes it (photo only where the viewer may see it). */
@Composable
fun MemberAvatar(member: Member, modifier: Modifier = Modifier, size: Dp = 44.dp) {
    AvatarView(member.initials, member.avatarColor, modifier, photoUrl = member.photoUrl, size = size)
}

/** The small mark of a crowd in compact lists (`x-network.crowd-mark`): a strip of its banner, or the colour dot. */
@Composable
fun CrowdMark(crowd: CrowdSummary, modifier: Modifier = Modifier) {
    val banner = crowd.bannerUrl
    if (banner != null) {
        Box(
            modifier
                .size(72.dp, 36.dp)
                .clip(RoundedCornerShape(MildcrowdRadius.CHIP.dp))
                .background(AvatarTint.surface(crowd.color))
                .clearAndSetSemantics {}
        ) {
            AuthorizedImage(banner, contentDescription = null, modifier = Modifier.size(72.dp, 36.dp), contentScale = ContentScale.Crop)
        }
    } else {
        Box(
            modifier
                .size(36.dp)
                .background(AvatarTint.surface(crowd.color), CircleShape)
                .border(1.dp, if (crowd.color == "dune") MildcrowdColors.DuneLine else Color.Transparent, CircleShape)
                .clearAndSetSemantics {}
        )
    }
}

/** "12 Mitglieder" / "1 Mitglied". */
fun membersLabel(count: Int): String = "$count ${if (count == 1) "Mitglied" else "Mitglieder"}"

/** "12 Posts" / "1 Post". */
fun postsLabel(count: Int): String = "$count ${if (count == 1) "Post" else "Posts"}"

/** What happens to the posts of a crowd that closes: one line of the closing page. */
fun postsStayLine(count: Int): String {
    if (count == 1) return "Ihr Post bleibt bei dem Menschen, der ihn geschrieben hat – nur ohne Crowd."
    return "Ihre $count Posts bleiben bei den Menschen, die sie geschrieben haben – nur ohne Crowd."
}

/** A row with a crowd mark, name and member count (Start, Entdecken, Suche, profile). */
@Composable
fun CrowdRow(crowd: CrowdSummary, modifier: Modifier = Modifier, detail: String? = null) {
    val navigator = LocalNavigator.current
    Row(
        modifier
            .fillMaxWidth()
            .pressable({ navigator.open(NetworkRoute.Crowd(crowd.slug)) })
            .testTag("crowd.row.${crowd.slug}"),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CrowdMark(crowd)
        Column(Modifier.weight(1f)) {
            Text(crowd.name, style = MildcrowdType.BodyBold, color = MildcrowdColors.Night, maxLines = 1, overflow = TextOverflow.Ellipsis)
            val text = detail ?: crowd.membersCount?.let(::membersLabel)
            if (text != null) Text(text, style = MildcrowdType.Small, color = MildcrowdColors.Muted)
        }
    }
}

/** A row with avatar, name and handle linking to the profile, with optional buttons at the end. */
@Composable
fun MemberRow(member: Member, modifier: Modifier = Modifier, size: Dp = 40.dp, trailing: @Composable RowScope.() -> Unit = {}) {
    val navigator = LocalNavigator.current
    Row(modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp), verticalAlignment = Alignment.CenterVertically) {
        Row(
            Modifier
                .weight(1f)
                .pressable({ navigator.open(NetworkRoute.Profile(member.username)) }),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            MemberAvatar(member, size = size)
            Column(Modifier.weight(1f)) {
                Text(
                    member.displayName,
                    style = MildcrowdType.BodyBold,
                    color = MildcrowdColors.Night,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Text(
                    "@${member.username}",
                    style = MildcrowdType.Small,
                    color = MildcrowdColors.Muted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        trailing()
    }
}
