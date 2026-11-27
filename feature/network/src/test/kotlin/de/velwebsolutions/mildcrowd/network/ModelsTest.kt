package de.velwebsolutions.mildcrowd.network

import de.velwebsolutions.mildcrowd.core.network.MildcrowdJson
import de.velwebsolutions.mildcrowd.network.api.CarrySource
import de.velwebsolutions.mildcrowd.network.api.Choice
import de.velwebsolutions.mildcrowd.network.api.ComposerContext
import de.velwebsolutions.mildcrowd.network.api.CrowdClosing
import de.velwebsolutions.mildcrowd.network.api.CrowdDetail
import de.velwebsolutions.mildcrowd.network.api.CrowdDirectory
import de.velwebsolutions.mildcrowd.network.api.CrowdEditForm
import de.velwebsolutions.mildcrowd.network.api.CrowdInput
import de.velwebsolutions.mildcrowd.network.api.CrowdMembers
import de.velwebsolutions.mildcrowd.network.api.CrowdOptions
import de.velwebsolutions.mildcrowd.network.api.CrowdSlug
import de.velwebsolutions.mildcrowd.network.api.CrowdStatistics
import de.velwebsolutions.mildcrowd.network.api.CrowdSummary
import de.velwebsolutions.mildcrowd.network.api.DiscoverContext
import de.velwebsolutions.mildcrowd.network.api.ExternalLink
import de.velwebsolutions.mildcrowd.network.api.FeedPage
import de.velwebsolutions.mildcrowd.network.api.FriendLists
import de.velwebsolutions.mildcrowd.network.api.GroupInput
import de.velwebsolutions.mildcrowd.network.api.GroupLanding
import de.velwebsolutions.mildcrowd.network.api.ImageUpload
import de.velwebsolutions.mildcrowd.network.api.InvitationLanding
import de.velwebsolutions.mildcrowd.network.api.InvitationOverview
import de.velwebsolutions.mildcrowd.network.api.Member
import de.velwebsolutions.mildcrowd.network.api.MemberStatistics
import de.velwebsolutions.mildcrowd.network.api.NoContext
import de.velwebsolutions.mildcrowd.network.api.OwnProfile
import de.velwebsolutions.mildcrowd.network.api.Post
import de.velwebsolutions.mildcrowd.network.api.PostDetail
import de.velwebsolutions.mildcrowd.network.api.PostEditForm
import de.velwebsolutions.mildcrowd.network.api.PostRevision
import de.velwebsolutions.mildcrowd.network.api.Profile
import de.velwebsolutions.mildcrowd.network.api.PublishResult
import de.velwebsolutions.mildcrowd.network.api.RoleChange
import de.velwebsolutions.mildcrowd.network.api.SearchContext
import de.velwebsolutions.mildcrowd.network.api.SourceContext
import de.velwebsolutions.mildcrowd.network.api.SwipeDeck
import de.velwebsolutions.mildcrowd.network.api.TagContext
import de.velwebsolutions.mildcrowd.network.api.TextSegment
import java.time.Instant
import kotlinx.serialization.KSerializer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * The API models with the defaults the server may leave out: every model is built with its defaults, written and read
 * back, and compared like the screens compare state.
 */
class ModelsTest {
    private val now = Instant.parse("2026-10-24T12:00:00Z")
    private val member = Member(1, "mira", "Mira Sol", "MS")
    private val summary = CrowdSummary(2, "garten", "Garten")

    private fun <T> roundTrip(serializer: KSerializer<T>, value: T) {
        val json = MildcrowdJson.snake.encodeToString(serializer, value)
        val read = MildcrowdJson.snake.decodeFromString(serializer, json)
        assertEquals(value, read)
        assertEquals(value.hashCode(), read.hashCode())
        assertTrue(value.toString().isNotEmpty())
        assertNotEquals(value, Any())
    }

    @Test
    fun `the role list names how many members a host cannot see`() {
        assertEquals(null, CrowdEditForm(summary).hiddenMembersNote)
        assertEquals(
            "Ein weiteres Mitglied zeigt sich hier nur Freund:innen und Menschen, denen es folgt, oder niemandem. " +
                "Eine Rolle kannst du nur Mitgliedern geben, die du hier siehst.",
            CrowdEditForm(summary, hiddenMembers = 1).hiddenMembersNote
        )
        assertTrue(CrowdEditForm(summary, hiddenMembers = 3).hiddenMembersNote!!.startsWith("3 weitere Mitglieder zeigen sich hier"))
    }

    @Test
    fun `Posts and their parts`() {
        val post = Post(1, "u", "b", createdAt = now, author = member)
        roundTrip(Member.serializer(), member)
        roundTrip(TextSegment.serializer(), TextSegment(text = "Hallo"))
        roundTrip(Post.serializer(), post)
        roundTrip(Post.CrowdRef.serializer(), Post.CrowdRef("garten", "Garten", "spring"))
        roundTrip(Post.ReplyRef.serializer(), Post.ReplyRef(1, "jonas"))
        roundTrip(Post.Image.serializer(), Post.Image(1, "u"))
        roundTrip(Post.Carried.serializer(), Post.Carried())
        roundTrip(Post.Carried.Original.serializer(), Post.Carried.Original(1, member, "b"))
        roundTrip(Post.Held.serializer(), Post.Held("Beleidigung"))
        roundTrip(Post.Stats.serializer(), Post.Stats())
        roundTrip(Post.Conversation.serializer(), Post.Conversation("Gespräch"))
        roundTrip(Post.Conversation.Reply.serializer(), Post.Conversation.Reply(1, member, "b"))
        roundTrip(Post.Viewer.serializer(), Post.Viewer())
        roundTrip(PostRevision.serializer(), PostRevision(1, "b", now))
        roundTrip(PostDetail.serializer(), PostDetail(post))
        roundTrip(PostEditForm.serializer(), PostEditForm("b", now))
        roundTrip(ExternalLink.serializer(), ExternalLink("https://example.org", "example.org"))
        roundTrip(CarrySource.serializer(), CarrySource(1, member, "b"))
        roundTrip(Choice.serializer(), Choice("public", "Öffentlich"))
        roundTrip(ComposerContext.serializer(), ComposerContext("public"))
        roundTrip(ComposerContext.CrowdName.serializer(), ComposerContext.CrowdName("garten", "Garten"))
        roundTrip(PublishResult.serializer(), PublishResult("Da.", data = post))
        assertFalse(post.isQuestion)
        assertFalse(post.isMembersOnly)
        assertEquals(1.5f, Post.Image(1, "u", width = 3, height = 2).aspectRatio)
    }

    @Test
    fun `lists, contexts and the swipe deck`() {
        roundTrip(FeedPage.serializer(NoContext.serializer()), FeedPage<NoContext>())
        assertNull(FeedPage<NoContext>().nextCursor)
        roundTrip(NoContext.serializer(), NoContext())
        assertEquals(NoContext().hashCode(), NoContext().hashCode())
        roundTrip(CrowdSummary.serializer(), summary)
        roundTrip(SourceContext.serializer(), SourceContext())
        roundTrip(SourceContext.Week.serializer(), SourceContext.Week())
        roundTrip(DiscoverContext.serializer(), DiscoverContext())
        roundTrip(SearchContext.serializer(), SearchContext())
        roundTrip(TagContext.serializer(), TagContext())
        roundTrip(SwipeDeck.serializer(), SwipeDeck())
        roundTrip(SwipeDeck.Chapter.serializer(), SwipeDeck.Chapter("freunde", "Freunde"))
        roundTrip(MemberStatistics.serializer(), MemberStatistics(MemberStatistics.Totals()))
        roundTrip(MemberStatistics.Week.serializer(), MemberStatistics.Week(now))
        roundTrip(MemberStatistics.TopPost.serializer(), MemberStatistics.TopPost(1, "b", createdAt = now))
    }

    @Test
    fun `crowds`() {
        roundTrip(CrowdOptions.serializer(), CrowdOptions())
        roundTrip(CrowdDirectory.serializer(), CrowdDirectory())
        roundTrip(CrowdDetail.serializer(), CrowdDetail(summary, "u"))
        roundTrip(CrowdMembers.serializer(), CrowdMembers())
        roundTrip(CrowdMembers.Entry.serializer(), CrowdMembers.Entry(1, "mira", "Mira Sol", "MS"))
        roundTrip(CrowdEditForm.serializer(), CrowdEditForm(summary))
        roundTrip(CrowdEditForm.MemberRole.serializer(), CrowdEditForm.MemberRole(1, "mira", "Mira Sol", "host", "Gastgeber:in"))
        roundTrip(CrowdInput.serializer(), CrowdInput())
        roundTrip(CrowdClosing.serializer(), CrowdClosing("Garten"))
        roundTrip(CrowdStatistics.serializer(), CrowdStatistics("Garten", CrowdStatistics.Totals()))
        roundTrip(CrowdStatistics.Week.serializer(), CrowdStatistics.Week(now))
        roundTrip(CrowdSlug.serializer(), CrowdSlug("garten"))
        roundTrip(RoleChange.serializer(), RoleChange())
    }

    @Test
    fun `a held crowd tells only those who read its real texts`() {
        val lenient = MildcrowdJson.snake.decodeFromString(CrowdSummary.serializer(), """{"id":2,"slug":"garten","name":"Garten"}""")
        assertEquals("servers before the crowd check", false, lenient.held)
        assertFalse(lenient.showsHeldTexts)
        val held = MildcrowdJson.snake.decodeFromString(
            CrowdSummary.serializer(),
            """{"id":2,"slug":"garten","name":"Garten","held":true}"""
        )
        assertTrue(held.held)
        assertTrue("the founder, the hosts and staff read the real name", held.showsHeldTexts)
        assertFalse("everyone else got the placeholder", held.copy(name = CrowdSummary.HELD_NAME).showsHeldTexts)
    }

    @Test
    fun `profiles, friendships and invitations`() {
        roundTrip(Profile.serializer(), Profile(member, "u", joinedAt = now))
        roundTrip(OwnProfile.serializer(), OwnProfile(member))
        roundTrip(FriendLists.serializer(), FriendLists())
        roundTrip(InvitationOverview.serializer(), InvitationOverview())
        roundTrip(InvitationOverview.Link.serializer(), InvitationOverview.Link(1, expiresAt = now))
        roundTrip(InvitationOverview.Link.Reward.serializer(), InvitationOverview.Link.Reward())
        roundTrip(InvitationLanding.serializer(), InvitationLanding())
        val group = InvitationOverview.Group(1, "WG", target = 3, joined = 1, progress = "1 von 3 haben angenommen", expiresAt = now)
        roundTrip(InvitationOverview.Group.serializer(), group)
        roundTrip(GroupInput.serializer(), GroupInput("WG", 3))
        roundTrip(GroupLanding.serializer(), GroupLanding())
        roundTrip(GroupLanding.Group.serializer(), GroupLanding.Group(member, 1, "WG", progress = "1 dabei", expiresAt = now))
        assertFalse("an open group is not complete", group.isComplete)
        assertFalse("a closed group below its target expired", group.copy(open = false).isComplete)
        assertTrue("a closed group at its target is complete", group.copy(open = false, joined = 3).isComplete)
        assertFalse("a closed group without a target expired", group.copy(open = false, target = null).isComplete)
    }

    @Test
    fun `an image upload compares its bytes, name and type`() {
        val image = ImageUpload(byteArrayOf(1, 2), "a.jpg", "image/jpeg")
        assertEquals(image, ImageUpload(byteArrayOf(1, 2), "a.jpg", "image/jpeg"))
        assertEquals(image.hashCode(), ImageUpload(byteArrayOf(1, 2), "a.jpg", "image/jpeg").hashCode())
        assertNotEquals(image, ImageUpload(byteArrayOf(1, 2), "b.jpg", "image/jpeg"))
        assertNotEquals(image, ImageUpload(byteArrayOf(1, 2), "a.jpg", "image/png"))
        assertNotEquals(image, ImageUpload(byteArrayOf(3), "a.jpg", "image/jpeg"))
        assertNotEquals(image, Any())
    }
}
