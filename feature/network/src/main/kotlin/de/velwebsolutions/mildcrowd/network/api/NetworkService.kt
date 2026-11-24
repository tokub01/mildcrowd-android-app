package de.velwebsolutions.mildcrowd.network.api

import de.velwebsolutions.mildcrowd.core.network.Confirmed
import de.velwebsolutions.mildcrowd.core.network.DataEnvelope
import de.velwebsolutions.mildcrowd.core.network.MessageResponse
import de.velwebsolutions.mildcrowd.core.network.Page
import kotlinx.serialization.Serializable
import okhttp3.MultipartBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.HTTP
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

@Serializable
internal data class BodyInput(val body: String)

@Serializable
internal data class ProfileInput(
    val bio: String,
    val location: String,
    val avatarColor: String,
    val toneConfirmed: Boolean,
    /** "" clears it. */
    val status: String
)

@Serializable
internal data class RoleInput(val role: String)

@Serializable
internal data class ConfirmInput(val confirm: Boolean)

@Serializable
internal data class PollVoteBody(val options: List<Int>)

/** The network endpoints (`/api/v1/network`, Laravel `routes/api/v1/members/network.php`). */
internal interface NetworkService {
    // Feeds

    @GET("network/source")
    suspend fun source(@Query("view") view: String?, @Query("cursor") cursor: String?): FeedPage<SourceContext>

    @GET("network/memories")
    suspend fun memories(): DataEnvelope<List<MemoryYear>>

    @POST("network/memories/dismiss")
    suspend fun dismissMemories(): MessageResponse

    @GET("network/discover")
    suspend fun discover(@Query("cursor") cursor: String?): FeedPage<DiscoverContext>

    @GET("network/swipe")
    suspend fun swipe(): DataEnvelope<SwipeDeck>

    @GET("network/bookmarks")
    suspend fun bookmarks(@Query("cursor") cursor: String?): FeedPage<NoContext>

    @GET("network/statistics")
    suspend fun statistics(): DataEnvelope<MemberStatistics>

    // Zeitkapseln and Jahrbuch (from Mittendrin)

    @GET("network/time-capsules")
    suspend fun timeCapsules(): DataEnvelope<TimeCapsules>

    /** Answers 201 with the capsule as well; the list reloads, so only the confirmation is read. */
    @POST("network/time-capsules")
    suspend fun sealTimeCapsule(@Body input: TimeCapsuleInput): MessageResponse

    @POST("network/time-capsules/{id}/open")
    suspend fun openTimeCapsule(@Path("id") id: Int): MessageResponse

    @DELETE("network/time-capsules/{id}")
    suspend fun deleteTimeCapsule(@Path("id") id: Int): MessageResponse

    /** The Karte der Crowds: all 16 Bundesländer in tile order with the crowds the viewer can see. */
    @GET("network/regions")
    suspend fun regions(): DataEnvelope<List<MapRegion>>

    @GET("network/regions/{code}/crowds")
    suspend fun regionCrowds(@Path("code") code: String): DataEnvelope<List<CrowdSummary>>

    /** The last 12 Crowd-Wochenbriefe, newest first. */
    @GET("network/weekly-letters")
    suspend fun weeklyLetters(): DataEnvelope<List<WeeklyLetterSummary>>

    /** The newest Wochenbrief, none before the first. */
    @GET("network/weekly-letters/latest")
    suspend fun latestWeeklyLetter(): LatestWeeklyLetter

    @GET("network/weekly-letters/{id}")
    suspend fun weeklyLetter(@Path("id") id: Int): DataEnvelope<WeeklyLetter>

    /** „Ideen für Mildcrowd“: 20 per page, `sort` popular|new, `status` one of [Idea.statuses]. */
    @GET("ideas")
    suspend fun ideas(@Query("sort") sort: String, @Query("status") status: String?, @Query("page") page: Int): Page<Idea>

    @GET("ideas/{id}")
    suspend fun idea(@Path("id") id: Int): DataEnvelope<Idea>

    @POST("ideas")
    suspend fun suggestIdea(@Body input: IdeaInput): Confirmed<Idea>

    @DELETE("ideas/{id}")
    suspend fun deleteIdea(@Path("id") id: Int): MessageResponse

    @POST("ideas/{id}/vote")
    suspend fun voteIdea(@Path("id") id: Int): Confirmed<Idea>

    @DELETE("ideas/{id}/vote")
    suspend fun unvoteIdea(@Path("id") id: Int): Confirmed<Idea>

    @GET("network/yearbook")
    suspend fun yearbook(): DataEnvelope<Yearbook>

    @GET("network/yearbook/{year}")
    suspend fun yearbook(@Path("year") year: Int): DataEnvelope<Yearbook>

    @GET("network/search")
    suspend fun search(@Query("q") query: String, @Query("cursor") cursor: String?): FeedPage<SearchContext>

    @GET("network/tags/{tag}")
    suspend fun tag(@Path("tag") tag: String, @Query("cursor") cursor: String?): FeedPage<TagContext>

    // Posts

    @GET("network/posts/{id}")
    suspend fun post(@Path("id") id: Int): DataEnvelope<PostDetail>

    @GET("network/posts/{id}/edit")
    suspend fun editForm(@Path("id") id: Int): DataEnvelope<PostEditForm>

    @PUT("network/posts/{id}")
    suspend fun updatePost(@Path("id") id: Int, @Body body: BodyInput): MessageResponse

    @GET("network/posts/{id}/carry")
    suspend fun carrySource(@Path("id") id: Int): DataEnvelope<CarrySource>

    @GET("network/composer")
    suspend fun composer(
        @Query("crowd") crowd: String?,
        @Query("reply_to") replyTo: Int?,
        @Query("carried") carried: Int?
    ): DataEnvelope<ComposerContext>

    @POST("network/posts/{id}/poll/vote")
    suspend fun votePoll(@Path("id") id: Int, @Body body: PollVoteBody): Confirmed<Post>

    @POST("network/posts/{id}/poll/close")
    suspend fun closePoll(@Path("id") id: Int): Confirmed<Post>

    @GET("network/crowds/{slug}/collections")
    suspend fun collections(@Path("slug") slug: String): DataEnvelope<CrowdCollections>

    @POST("network/crowds/{slug}/collections")
    suspend fun createCollection(@Path("slug") slug: String, @Body body: CollectionInput): Confirmed<CrowdCollection>

    @PUT("network/collections/{id}")
    suspend fun updateCollection(@Path("id") id: Int, @Body body: CollectionInput): Confirmed<CrowdCollection>

    @DELETE("network/collections/{id}")
    suspend fun deleteCollection(@Path("id") id: Int): MessageResponse

    @GET("network/collections/{id}/posts")
    suspend fun collectionPosts(@Path("id") id: Int, @Query("page") page: Int): Page<Post>

    @POST("network/collections/{id}/posts")
    suspend fun collectPost(@Path("id") id: Int, @Body body: CollectPostBody): Confirmed<CrowdCollection>

    @DELETE("network/collections/{id}/posts/{post}")
    suspend fun uncollectPost(@Path("id") id: Int, @Path("post") post: Int): Confirmed<CrowdCollection>

    @PUT("network/crowds/{slug}/collection-settings")
    suspend fun collectionSettings(@Path("slug") slug: String, @Body body: CollectionSettings): DataEnvelope<CollectionSettings>

    @GET("network/time-capsules/invitees")
    suspend fun capsuleInvitees(@Query("crowd_id") crowdId: Int?): DataEnvelope<List<Member>>

    @PUT("network/time-capsules/invitations/{id}")
    suspend fun contribute(@Path("id") id: Int, @Body body: ContributionBody): Confirmed<CapsuleInvitation>

    @DELETE("network/time-capsules/invitations/{id}/contribution")
    suspend fun removeContribution(@Path("id") id: Int): Confirmed<CapsuleInvitation>

    @POST("network/time-capsules/invitations/{id}/decline")
    suspend fun declineInvitation(@Path("id") id: Int): Confirmed<CapsuleInvitation>

    @GET("network/crowds/{slug}/meetups")
    suspend fun meetups(@Path("slug") slug: String): DataEnvelope<CrowdMeetups>

    @POST("network/crowds/{slug}/meetups")
    suspend fun createMeetup(@Path("slug") slug: String, @Body body: MeetupInput): Confirmed<Meetup>

    @PUT("network/meetups/{id}")
    suspend fun updateMeetup(@Path("id") id: Int, @Body body: MeetupInput): Confirmed<Meetup>

    @DELETE("network/meetups/{id}")
    suspend fun cancelMeetup(@Path("id") id: Int): Confirmed<Meetup>

    @POST("network/meetups/{id}/answer")
    suspend fun answerMeetup(@Path("id") id: Int, @Body body: MeetupAnswerBody): Confirmed<Meetup>

    @PUT("network/crowds/{slug}/meetup-settings")
    suspend fun meetupSettings(@Path("slug") slug: String, @Body body: MeetupSettings): DataEnvelope<MeetupSettings>

    @GET("network/meetups/upcoming")
    suspend fun upcomingMeetups(): DataEnvelope<List<Meetup>>

    @PUT("network/crowds/{slug}/welcome")
    suspend fun welcomeVolunteer(@Path("slug") slug: String, @Body body: WelcomeVolunteer): DataEnvelope<WelcomeVolunteer>

    @Multipart
    @POST("network/posts")
    suspend fun publish(@Part parts: List<MultipartBody.Part>): PublishResult

    @DELETE("network/posts/{id}")
    suspend fun deletePost(@Path("id") id: Int): MessageResponse

    @POST("network/posts/{id}/reaction")
    suspend fun react(@Path("id") id: Int): MessageResponse

    @DELETE("network/posts/{id}/reaction")
    suspend fun unreact(@Path("id") id: Int): MessageResponse

    @POST("network/posts/{id}/bookmark")
    suspend fun bookmark(@Path("id") id: Int): MessageResponse

    @DELETE("network/posts/{id}/bookmark")
    suspend fun unbookmark(@Path("id") id: Int): MessageResponse

    @POST("network/posts/{id}/remove-from-crowd")
    suspend fun removeFromCrowd(@Path("id") id: Int): MessageResponse

    // Protecting oneself

    @POST("network/users/{id}/block")
    suspend fun block(@Path("id") userId: Int): MessageResponse

    @POST("network/users/{id}/mute")
    suspend fun mute(@Path("id") userId: Int): MessageResponse

    @DELETE("network/users/{id}/mute")
    suspend fun unmute(@Path("id") userId: Int): MessageResponse

    @POST("network/users/{id}/restrict")
    suspend fun restrict(@Path("id") userId: Int): MessageResponse

    @DELETE("network/users/{id}/restrict")
    suspend fun unrestrict(@Path("id") userId: Int): MessageResponse

    // Profiles

    @GET("network/profiles/{handle}")
    suspend fun profile(@Path("handle") handle: String): DataEnvelope<Profile>

    @GET("network/profiles/{handle}/posts")
    suspend fun profilePosts(@Path("handle") handle: String, @Query("cursor") cursor: String?): FeedPage<NoContext>

    @POST("network/profiles/{handle}/follow")
    suspend fun follow(@Path("handle") handle: String): MessageResponse

    @DELETE("network/profiles/{handle}/follow")
    suspend fun unfollow(@Path("handle") handle: String): MessageResponse

    @POST("network/profiles/{handle}/friendship")
    suspend fun requestFriendship(@Path("handle") handle: String): MessageResponse

    @POST("network/profiles/{handle}/friendship/accept")
    suspend fun acceptFriendship(@Path("handle") handle: String): MessageResponse

    @DELETE("network/profiles/{handle}/friendship")
    suspend fun removeFriendship(@Path("handle") handle: String): MessageResponse

    @GET("network/profile")
    suspend fun ownProfile(): DataEnvelope<OwnProfile>

    @PUT("network/profile")
    suspend fun updateOwnProfile(@Body body: ProfileInput): Confirmed<OwnProfile>

    @Multipart
    @POST("network/profile/photo")
    suspend fun uploadPhoto(@Part photo: MultipartBody.Part): Confirmed<OwnProfile>

    @DELETE("network/profile/photo")
    suspend fun removePhoto(): Confirmed<OwnProfile>

    @Multipart
    @POST("network/profile/banner")
    suspend fun uploadProfileBanner(@Part banner: MultipartBody.Part): Confirmed<OwnProfile>

    @DELETE("network/profile/banner")
    suspend fun removeProfileBanner(): Confirmed<OwnProfile>

    @GET("network/friends")
    suspend fun friends(): DataEnvelope<FriendLists>

    @POST("network/friends/{id}/accept")
    suspend fun acceptFriend(@Path("id") userId: Int): MessageResponse

    @DELETE("network/friends/{id}")
    suspend fun removeFriend(@Path("id") userId: Int): MessageResponse

    // Crowds

    @GET("network/crowds")
    suspend fun crowds(): DataEnvelope<CrowdDirectory>

    @GET("network/crowds/{slug}")
    suspend fun crowd(@Path("slug") slug: String): DataEnvelope<CrowdDetail>

    @GET("network/crowds/{slug}/posts")
    suspend fun crowdPosts(@Path("slug") slug: String, @Query("cursor") cursor: String?): FeedPage<NoContext>

    @GET("network/crowds/{slug}/members")
    suspend fun crowdMembers(@Path("slug") slug: String, @Query("q") query: String?): DataEnvelope<CrowdMembers>

    @POST("network/crowds/{slug}/follow")
    suspend fun followCrowd(@Path("slug") slug: String): MessageResponse

    @DELETE("network/crowds/{slug}/follow")
    suspend fun unfollowCrowd(@Path("slug") slug: String): MessageResponse

    @Multipart
    @POST("network/crowds")
    suspend fun createCrowd(@Part parts: List<MultipartBody.Part>): DataEnvelope<CrowdSlug>

    @GET("network/crowds/{slug}/edit")
    suspend fun crowdEditForm(@Path("slug") slug: String): DataEnvelope<CrowdEditForm>

    @PUT("network/crowds/{slug}")
    suspend fun updateCrowd(@Path("slug") slug: String, @Body input: CrowdUpdate): DataEnvelope<CrowdSlug>

    @Multipart
    @POST("network/crowds/{slug}/banner")
    suspend fun uploadBanner(@Path("slug") slug: String, @Part banner: MultipartBody.Part): MessageResponse

    @DELETE("network/crowds/{slug}/banner")
    suspend fun removeBanner(@Path("slug") slug: String): MessageResponse

    @PUT("network/crowds/{slug}/members/{id}/role")
    suspend fun setRole(@Path("slug") slug: String, @Path("id") userId: Int, @Body body: RoleInput): Confirmed<RoleChange>

    @GET("network/crowds/{slug}/close")
    suspend fun crowdClosing(@Path("slug") slug: String): DataEnvelope<CrowdClosing>

    @HTTP(method = "DELETE", path = "network/crowds/{slug}", hasBody = true)
    suspend fun closeCrowd(@Path("slug") slug: String, @Body body: ConfirmInput): MessageResponse

    @GET("network/crowds/{slug}/statistics")
    suspend fun crowdStatistics(@Path("slug") slug: String): DataEnvelope<CrowdStatistics>

    // Invitations

    @GET("network/invitations")
    suspend fun invitations(): DataEnvelope<InvitationOverview>

    @POST("network/invitations")
    suspend fun createInvitation(): MessageResponse

    @GET("network/invitations/{token}")
    suspend fun invitation(@Path("token") token: String): DataEnvelope<InvitationLanding>

    @POST("network/invitations/groups")
    suspend fun createGroup(@Body body: GroupInput): Confirmed<InvitationOverview.Group>

    @GET("network/invitations/groups/{token}")
    suspend fun invitationGroup(@Path("token") token: String): DataEnvelope<GroupLanding>
}
