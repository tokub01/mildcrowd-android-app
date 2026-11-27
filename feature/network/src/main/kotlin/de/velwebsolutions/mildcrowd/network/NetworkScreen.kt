package de.velwebsolutions.mildcrowd.network

import androidx.compose.runtime.Composable
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.network.ui.crowds.CloseCrowdScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CollectPostScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdDetailScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdFormScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdMapScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdStatisticsScreen
import de.velwebsolutions.mildcrowd.network.ui.crowds.CrowdsScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.BookmarksScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.DiscoverScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.IdeasScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.SearchScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.SourceScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.SwipeScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.TagScreen
import de.velwebsolutions.mildcrowd.network.ui.feeds.WeeklyLetterScreen
import de.velwebsolutions.mildcrowd.network.ui.posts.CarryScreen
import de.velwebsolutions.mildcrowd.network.ui.posts.ComposeScreen
import de.velwebsolutions.mildcrowd.network.ui.posts.EditPostScreen
import de.velwebsolutions.mildcrowd.network.ui.posts.PostDetailScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.EditProfileScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.FriendsScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.InvitationGroupScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.InvitationScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.InvitationsScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.ProfileScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.StatisticsScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.TimeCapsulesScreen
import de.velwebsolutions.mildcrowd.network.ui.profiles.YearbookScreen

/** Entry point of the Network module: the screen for each [NetworkRoute] (mapped in the app). */
@Composable
fun NetworkScreen(route: NetworkRoute) {
    when (route) {
        NetworkRoute.Source -> SourceScreen()

        NetworkRoute.Discover -> DiscoverScreen()

        NetworkRoute.Swipe -> SwipeScreen()

        is NetworkRoute.Post -> PostDetailScreen(route.id)

        is NetworkRoute.EditPost -> EditPostScreen(route.id)

        is NetworkRoute.CarryPost -> CarryScreen(route.id, quote = route.quote)

        is NetworkRoute.Compose -> ComposeScreen(route.crowdSlug, route.replyTo)

        NetworkRoute.Crowds -> CrowdsScreen()

        is NetworkRoute.Crowd -> CrowdDetailScreen(route.slug, atMeetups = route.meetups, atCollection = route.collection)

        is NetworkRoute.CollectPost -> CollectPostScreen(route.postId, route.slug)

        is NetworkRoute.CrowdMap -> CrowdMapScreen(route.region)

        NetworkRoute.CreateCrowd -> CrowdFormScreen(slug = null)

        is NetworkRoute.EditCrowd -> CrowdFormScreen(route.slug)

        is NetworkRoute.CloseCrowd -> CloseCrowdScreen(route.slug)

        is NetworkRoute.CrowdStatistics -> CrowdStatisticsScreen(route.slug)

        is NetworkRoute.Profile -> ProfileScreen(route.handle)

        NetworkRoute.EditProfile -> EditProfileScreen()

        NetworkRoute.Friends -> FriendsScreen()

        NetworkRoute.Statistics -> StatisticsScreen()

        NetworkRoute.TimeCapsules -> TimeCapsulesScreen()

        NetworkRoute.CapsuleInvitations -> TimeCapsulesScreen(atInvitations = true)

        is NetworkRoute.Yearbook -> YearbookScreen(route.year)

        NetworkRoute.Invitations -> InvitationsScreen()

        is NetworkRoute.Invitation -> InvitationScreen(route.token)

        is NetworkRoute.InvitationGroup -> InvitationGroupScreen(route.token)

        is NetworkRoute.Search -> SearchScreen(route.query.orEmpty())

        is NetworkRoute.Tag -> TagScreen(route.tag)

        is NetworkRoute.WeeklyLetter -> WeeklyLetterScreen(route.id)

        is NetworkRoute.Ideas -> IdeasScreen(route.id)

        // The last route (Merkliste) without a comparison: an exhaustive `when` would add an unreachable throw.
        else -> BookmarksScreen()
    }
}
