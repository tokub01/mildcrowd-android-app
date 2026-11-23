package de.velwebsolutions.mildcrowd.network.ui.crowds

import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.velwebsolutions.mildcrowd.core.format.GermanTime
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.core.network.Confirmed
import de.velwebsolutions.mildcrowd.core.network.MildcrowdJson
import de.velwebsolutions.mildcrowd.network.NetworkSignals
import de.velwebsolutions.mildcrowd.network.api.CollectionInput
import de.velwebsolutions.mildcrowd.network.api.CrowdClosing
import de.velwebsolutions.mildcrowd.network.api.CrowdCollection
import de.velwebsolutions.mildcrowd.network.api.CrowdCollections
import de.velwebsolutions.mildcrowd.network.api.CrowdDetail
import de.velwebsolutions.mildcrowd.network.api.CrowdDirectory
import de.velwebsolutions.mildcrowd.network.api.CrowdEditForm
import de.velwebsolutions.mildcrowd.network.api.CrowdInput
import de.velwebsolutions.mildcrowd.network.api.CrowdMeetups
import de.velwebsolutions.mildcrowd.network.api.CrowdMembers
import de.velwebsolutions.mildcrowd.network.api.CrowdOptions
import de.velwebsolutions.mildcrowd.network.api.CrowdStatistics
import de.velwebsolutions.mildcrowd.network.api.FeedPage
import de.velwebsolutions.mildcrowd.network.api.ImageUpload
import de.velwebsolutions.mildcrowd.network.api.Meetup
import de.velwebsolutions.mildcrowd.network.api.MeetupInput
import de.velwebsolutions.mildcrowd.network.api.NoContext
import de.velwebsolutions.mildcrowd.network.data.NetworkRepository
import de.velwebsolutions.mildcrowd.network.ui.NetworkViewModel
import de.velwebsolutions.mildcrowd.network.ui.PostListModel
import de.velwebsolutions.mildcrowd.network.ui.apiError
import de.velwebsolutions.mildcrowd.network.ui.attempt
import java.time.Instant
import javax.inject.Inject
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** "Crowds" (/app/crowds): the member's crowds and all others. */
@HiltViewModel
class CrowdsViewModel @Inject constructor(private val repository: NetworkRepository) : NetworkViewModel() {
    private val mutableDirectory = MutableStateFlow<Loadable<CrowdDirectory>>(Loadable.Idle)
    val directory: StateFlow<Loadable<CrowdDirectory>> = mutableDirectory.asStateFlow()

    init {
        observe(NetworkSignals.crowds) { load() }
    }

    suspend fun load() {
        mutableDirectory.load { repository.crowds() }
    }

    suspend fun loadIfNeeded() {
        if (directory.value.valueOrNull == null) load()
    }
}

/**
 * A crowd page (/crowds/{slug}): the crowd, its Posts, joining and leaving and – for members – the member list with a
 * name filter.
 */
@HiltViewModel(assistedFactory = CrowdViewModel.Factory::class)
class CrowdViewModel @AssistedInject constructor(@Assisted val slug: String, private val repository: NetworkRepository) :
    NetworkViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(slug: String): CrowdViewModel
    }

    private val mutableDetail = MutableStateFlow<Loadable<CrowdDetail>>(Loadable.Idle)
    val detail: StateFlow<Loadable<CrowdDetail>> = mutableDetail.asStateFlow()
    val posts = PostListModel<NoContext>(repository) { _, cursor -> crowdPosts(slug, cursor) }
    private val mutableMembers = MutableStateFlow<CrowdMembers?>(null)
    val members: StateFlow<CrowdMembers?> = mutableMembers.asStateFlow()
    private val mutableMemberQuery = MutableStateFlow("")
    val memberQuery: StateFlow<String> = mutableMemberQuery.asStateFlow()
    private val mutableMeetups = MutableStateFlow<CrowdMeetups?>(null)

    /** „Treffen“ for members (null for others, or while not loaded). */
    val meetups: StateFlow<CrowdMeetups?> = mutableMeetups.asStateFlow()
    private val mutableCollections = MutableStateFlow<CrowdCollections?>(null)

    /** „Sammlungen“ for everyone who sees the crowd (null while not loaded, or when they cannot load). */
    val collections: StateFlow<CrowdCollections?> = mutableCollections.asStateFlow()
    private val mutableShownCollection = MutableStateFlow<PostListModel<NoContext>?>(null)

    /** The Posts of the chosen collection, newest added first (null: none chosen). */
    val shownCollection: StateFlow<PostListModel<NoContext>?> = mutableShownCollection.asStateFlow()
    private val mutableShownCollectionId = MutableStateFlow<Int?>(null)

    /** The chosen collection. */
    val shownCollectionId: StateFlow<Int?> = mutableShownCollectionId.asStateFlow()
    private val mutableCollectionForm = MutableStateFlow<CollectionForm?>(null)

    /** The form to create or rename a collection, while it is open. */
    val collectionForm: StateFlow<CollectionForm?> = mutableCollectionForm.asStateFlow()
    private val mutableMeetupForm = MutableStateFlow<MeetupForm?>(null)

    /** The form to create or change a meetup, while it is open. */
    val meetupForm: StateFlow<MeetupForm?> = mutableMeetupForm.asStateFlow()

    init {
        observe(NetworkSignals.published) { posts.reload() }
        observe(NetworkSignals.crowds) { load() }
        observe(NetworkSignals.collections) {
            loadCollections()
            posts.reload()
            shownCollection.value?.reload()
        }
    }

    fun updateMemberQuery(query: String) {
        mutableMemberQuery.value = query
    }

    suspend fun load() {
        val loaded = mutableDetail.load { repository.crowd(slug) } ?: return
        posts.reload()
        loadCollections()
        if (loaded.isMember) {
            loadMembers()
            loadMeetups()
        } else {
            mutableMembers.value = null
            mutableMeetups.value = null
        }
    }

    /**
     * Loads the page once. What a load cancelled with the screen (a rotation) did not get to loads again with it: the
     * Posts, and for members the member list.
     */
    suspend fun loadIfNeeded() {
        val loaded = detail.value.valueOrNull
        if (loaded == null) {
            load()
            return
        }
        posts.loadIfNeeded()
        if (loaded.isMember && members.value == null) loadMembers()
        if (loaded.isMember && meetups.value == null) loadMeetups()
        if (collections.value == null) loadCollections()
    }

    /** The collections; a list that cannot load is simply not shown. */
    suspend fun loadCollections() {
        mutableCollections.value = attempt { repository.collections(slug) }.getOrNull()
    }

    /** Shows the Posts of collection [id] (the chosen one again: none). */
    suspend fun chooseCollection(id: Int?) {
        val chosen = id?.takeIf { it != shownCollectionId.value }
        mutableShownCollectionId.value = chosen
        val list = chosen?.let { collection ->
            PostListModel<NoContext>(repository, collectionId = collection) { _, cursor -> collectionPage(collection, cursor) }
        }
        mutableShownCollection.value = list
        list?.reload()
    }

    /** „Sammlung anlegen“ (null) or „Ändern“. */
    fun openCollectionForm(collection: CrowdCollection?) {
        mutableCollectionForm.value = collection?.let { CollectionForm(it.id, it.name, it.description.orEmpty()) } ?: CollectionForm()
    }

    fun editCollectionForm(change: (CollectionForm) -> CollectionForm) = mutableCollectionForm.update { it?.let(change) }

    fun closeCollectionForm() {
        mutableCollectionForm.value = null
    }

    /** „Sammlung anlegen“ / „Änderung speichern“; refused fields stay on the form. */
    suspend fun saveCollection() {
        val form = collectionForm.value ?: return
        val id = form.editing
        val input = CollectionInput(form.name.trim(), form.description.trim())
        val result = attempt { if (id == null) repository.createCollection(slug, input) else repository.updateCollection(id, input) }
        val saved = result.getOrNull()
        if (saved == null) {
            mutableCollectionForm.value = form.copy(error = result.apiError)
            return
        }
        mutableStatus.value = saved.message
        mutableCollectionForm.value = null
        loadCollections()
    }

    /** „Sammlung löschen“ (after confirming): its Posts stay in the crowd. */
    suspend fun deleteCollection(collection: CrowdCollection) {
        if (!confirm { repository.deleteCollection(collection.id) }) return
        if (shownCollectionId.value == collection.id) chooseCollection(null)
        loadCollections()
        posts.reload()
    }

    /** „Mitglieder dürfen Sammlungen anlegen“ (hosts). */
    suspend fun setMembersCreateCollections(allowed: Boolean) {
        val result = attempt { repository.setMembersCreateCollections(slug, allowed) }
        mutableError.value = result.apiError
        val stored = result.getOrNull() ?: return
        mutableCollections.update { it?.copy(membersCreateCollections = stored.membersCreateCollections) }
    }

    /** The meetups of the crowd; a list that cannot load is simply not shown. */
    suspend fun loadMeetups() {
        mutableMeetups.value = attempt { repository.meetups(slug) }.getOrNull()
    }

    /** „Treffen anlegen“ (null) or „Ändern“ of [meetup]: the form, filled with the meetup or empty for tomorrow evening. */
    fun openMeetupForm(meetup: Meetup?, now: Instant = Instant.now()) {
        mutableMeetupForm.value = meetup?.let(MeetupForm::of) ?: MeetupForm(startsAt = MeetupForm.tomorrowEvening(now))
    }

    fun editMeetupForm(change: (MeetupForm) -> MeetupForm) = mutableMeetupForm.update { it?.let(change) }

    /** „Abbrechen“. */
    fun closeMeetupForm() {
        mutableMeetupForm.value = null
    }

    /** „Treffen anlegen“ / „Änderung speichern“; refused fields stay on the form with their messages. */
    suspend fun saveMeetup() {
        val form = meetupForm.value ?: return
        val id = form.editing
        val result = attempt { if (id == null) repository.createMeetup(slug, form.input) else repository.updateMeetup(id, form.input) }
        val saved = result.getOrNull()
        if (saved == null) {
            mutableMeetupForm.value = form.copy(error = result.apiError)
            return
        }
        mutableStatus.value = saved.message
        mutableMeetupForm.value = null
        loadMeetups()
    }

    /** „Ich komme“, „Vielleicht“, „Nein“. */
    suspend fun answerMeetup(meetup: Meetup, answer: String) = changeMeetup { repository.answerMeetup(meetup.id, answer) }

    /** „Treffen absagen“ (after confirming); who said yes or maybe gets a hint. */
    suspend fun cancelMeetup(meetup: Meetup) = changeMeetup { repository.cancelMeetup(meetup.id) }

    private suspend fun changeMeetup(request: suspend () -> Confirmed<Meetup>) {
        val result = attempt(request)
        mutableError.value = result.apiError
        val answer = result.getOrNull() ?: return
        mutableStatus.value = answer.message
        mutableMeetups.update { list ->
            list?.copy(
                upcoming = list.upcoming.map { if (it.id == answer.data.id) answer.data else it },
                past = list.past.map { if (it.id == answer.data.id) answer.data else it }
            )
        }
    }

    /** „Mitglieder dürfen Treffen anlegen“ (hosts). */
    suspend fun setMembersCreateMeetups(allowed: Boolean) {
        val result = attempt { repository.setMembersCreateMeetups(slug, allowed) }
        mutableError.value = result.apiError
        val stored = result.getOrNull() ?: return
        mutableMeetups.update { it?.copy(membersCreateMeetups = stored.membersCreateMeetups) }
    }

    /** „Ich bin gern Willkommenspate oder -patin“. */
    suspend fun setWelcomeVolunteer(volunteer: Boolean) {
        val result = attempt { repository.setWelcomeVolunteer(slug, volunteer) }
        mutableError.value = result.apiError
        val stored = result.getOrNull() ?: return
        val loaded = detail.value.valueOrNull ?: return
        mutableDetail.value = Loadable.Loaded(loaded.copy(welcome = loaded.welcome?.copy(volunteer = stored.volunteer)))
    }

    suspend fun loadMembers() {
        // A list that cannot load is simply not shown.
        mutableMembers.value = attempt { repository.crowdMembers(slug, memberQuery.value.take(60)) }.getOrNull()
    }

    /** Follow (= become a member) or unfollow; the last host cannot leave (the server explains why). */
    suspend fun setFollowing(follow: Boolean) {
        if (confirm { repository.followCrowd(slug, follow) }) {
            NetworkSignals.didChangeCrowds()
            load()
        }
    }
}

/** What the crowd form shows. */
data class CrowdFormState(
    val input: CrowdInput = CrowdInput(),
    val options: CrowdOptions? = null,
    val edit: CrowdEditForm? = null,
    val loadError: ApiError? = null,
    /** The banner picked while founding; sent with "Crowd eröffnen". */
    val banner: ImageUpload? = null,
    /** Where the form goes after saving. */
    val outcome: CrowdFormOutcome? = null
)

/** Where the crowd form goes after saving. */
sealed interface CrowdFormOutcome {
    /** Back to the crowd (edited), or back after giving up the own host role (the confirmation goes to the tab). */
    data object Close : CrowdFormOutcome

    /** The new crowd opens in place of the form. */
    data class Open(val slug: String) : CrowdFormOutcome
}

/**
 * Found a crowd (/app/crowds/neu) or edit it as host (/crowds/{slug}/bearbeiten): texts, house rules, audience, colour,
 * the optional banner (stored at once when editing, with "Crowd eröffnen" when founding), closing and roles.
 */
@HiltViewModel(assistedFactory = CrowdFormViewModel.Factory::class)
class CrowdFormViewModel @AssistedInject constructor(@Assisted val slug: String?, private val repository: NetworkRepository) :
    NetworkViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(slug: String?): CrowdFormViewModel
    }

    private val mutableState = MutableStateFlow(CrowdFormState())
    val state: StateFlow<CrowdFormState> = mutableState.asStateFlow()
    val editing: Boolean get() = slug != null

    fun updateInput(change: (CrowdInput) -> CrowdInput) = mutableState.update { it.copy(input = change(it.input)) }

    /** The form: an edited crowd with its saved texts, banner and roles, or the choices for founding one. */
    suspend fun load() {
        val result = attempt {
            if (slug != null) {
                val form = repository.crowdEditForm(slug)
                val crowd = form.crowd
                val input =
                    CrowdInput(
                        crowd.name,
                        crowd.description,
                        form.houseRules,
                        crowd.visibility,
                        crowd.color,
                        crowd.region?.code,
                        crowd.city.orEmpty()
                    )
                mutableState.update { it.copy(edit = form, options = form.options, input = input) }
            } else {
                val options = repository.crowds().options
                mutableState.update { it.copy(options = options) }
            }
        }
        mutableState.update { it.copy(loadError = result.apiError) }
    }

    suspend fun loadIfNeeded() {
        if (state.value.options == null) load()
    }

    suspend fun save() {
        val result = attempt {
            if (slug != null) {
                repository.updateCrowd(slug, state.value.input)
                CrowdFormOutcome.Close
            } else {
                CrowdFormOutcome.Open(repository.createCrowd(state.value.input, state.value.banner))
            }
        }
        mutableError.value = result.apiError
        result.onSuccess { outcome ->
            NetworkSignals.didChangeCrowds()
            mutableState.update { it.copy(outcome = outcome) }
        }
    }

    /** A picked banner (null when it could not be read): stored at once when editing, kept for "Crowd eröffnen" when founding. */
    suspend fun pickBanner(upload: ImageUpload?) {
        upload ?: return
        mutableError.value = null
        if (slug != null) {
            if (confirm { repository.uploadBanner(slug, upload) }) {
                NetworkSignals.didChangeCrowds()
                refresh(slug)
            }
        } else {
            mutableState.update { it.copy(banner = upload) }
            mutableStatus.value = "Das Banner wird mit der Crowd gespeichert."
        }
    }

    suspend fun removeBanner() {
        mutableState.update { it.copy(banner = null) }
        if (slug == null) {
            mutableStatus.value = "Das Banner ist entfernt. Jetzt zeigen wir wieder die Farbe der Crowd."
            return
        }
        if (confirm { repository.removeBanner(slug) }) {
            NetworkSignals.didChangeCrowds()
            refresh(slug)
        }
    }

    suspend fun setRole(userId: Int, role: String) {
        val slug = slug ?: return
        val result = attempt { repository.setRole(slug, userId, role) }
        mutableError.value = result.apiError
        val change = result.getOrNull() ?: return
        NetworkSignals.didChangeCrowds()
        if (change.data.stillHost) {
            mutableStatus.value = change.message
            refresh(slug)
        } else {
            NetworkSignals.confirmation.value = change.message
            mutableState.update { it.copy(outcome = CrowdFormOutcome.Close) }
        }
    }

    fun consumeOutcome() = mutableState.update { it.copy(outcome = null) }

    /**
     * The banner and the roles again after a stored banner or a changed role. What the member typed into the form and has
     * not saved yet stays, and a refresh that fails keeps the form (its error shows above it).
     */
    private suspend fun refresh(slug: String) {
        val result = attempt { repository.crowdEditForm(slug) }
        result.onSuccess { form -> mutableState.update { it.copy(edit = form, options = form.options) } }
        mutableError.value = result.apiError
    }

    companion object {
        /** The fields with their own error line; anything else shows as a banner above the form. */
        val FIELDS = listOf("name", "description", "house_rules", "visibility", "color", "banner", "role", "region", "city")
    }
}

/** "„…“ schließen?" (/crowds/{slug}/schliessen): host-only confirmation with an explicit checkbox. */
@HiltViewModel(assistedFactory = CloseCrowdViewModel.Factory::class)
class CloseCrowdViewModel @AssistedInject constructor(@Assisted val slug: String, private val repository: NetworkRepository) :
    NetworkViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(slug: String): CloseCrowdViewModel
    }

    private val mutableClosing = MutableStateFlow<Loadable<CrowdClosing>>(Loadable.Idle)
    val closing: StateFlow<Loadable<CrowdClosing>> = mutableClosing.asStateFlow()
    private val mutableConfirmed = MutableStateFlow(false)
    val confirmed: StateFlow<Boolean> = mutableConfirmed.asStateFlow()
    private val mutableClosed = MutableStateFlow(false)

    /** Set after closing: the app goes to the crowd overview, which shows the confirmation. */
    val closed: StateFlow<Boolean> = mutableClosed.asStateFlow()

    fun setConfirmed(value: Boolean) {
        mutableConfirmed.value = value
    }

    suspend fun load() {
        mutableClosing.load { repository.crowdClosing(slug) }
    }

    suspend fun close() {
        val result = attempt { repository.closeCrowd(slug, confirmed.value) }
        mutableError.value = result.apiError
        result.onSuccess { message ->
            NetworkSignals.confirmation.value = message
            NetworkSignals.didChangeCrowds()
            mutableClosed.value = true
        }
    }
}

/** Host statistics (/crowds/{slug}/statistik): sums only, never lists of people. */
@HiltViewModel(assistedFactory = CrowdStatisticsViewModel.Factory::class)
class CrowdStatisticsViewModel @AssistedInject constructor(@Assisted val slug: String, private val repository: NetworkRepository) :
    NetworkViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(slug: String): CrowdStatisticsViewModel
    }

    private val mutableStatistics = MutableStateFlow<Loadable<CrowdStatistics>>(Loadable.Idle)
    val statistics: StateFlow<Loadable<CrowdStatistics>> = mutableStatistics.asStateFlow()

    suspend fun load() {
        mutableStatistics.load { repository.crowdStatistics(slug) }
    }
}

/**
 * The form of a meetup: title (≤ 120), date and time in German time (in the future), place (≤ 200), link (http(s),
 * ≤ 500) and description (≤ 2000); [editing] names the meetup being changed, [error] the server's refusal.
 */
data class MeetupForm(
    val editing: Int? = null,
    val title: String = "",
    val startsAt: Instant,
    val place: String = "",
    val link: String = "",
    val description: String = "",
    val error: ApiError? = null
) {
    /** What is sent: every field, empty ones as "". */
    val input: MeetupInput get() = MeetupInput(
        title.trim(),
        MildcrowdJson.formatInstant(startsAt),
        place.trim(),
        link.trim(),
        description.trim()
    )

    companion object {
        fun of(meetup: Meetup) = MeetupForm(
            editing = meetup.id,
            title = meetup.title,
            startsAt = meetup.startsAt,
            place = meetup.place.orEmpty(),
            link = meetup.link.orEmpty(),
            description = meetup.description.orEmpty()
        )

        /** A start to begin with: tomorrow at 18:00 German time. */
        fun tomorrowEvening(now: Instant): Instant =
            now.atZone(GermanTime.zone).toLocalDate().plusDays(1).atTime(18, 0).atZone(GermanTime.zone).toInstant()
    }
}

/** The form of a collection: name (1–60) and description (≤ 300); [editing] names the one being renamed. */
data class CollectionForm(val editing: Int? = null, val name: String = "", val description: String = "", val error: ApiError? = null)

/** A page of a collection's Posts as a list page: the next page number as the cursor while there is one. */
internal suspend fun NetworkRepository.collectionPage(id: Int, cursor: String?): FeedPage<NoContext> {
    val page = collectionPosts(id, cursor?.toIntOrNull() ?: 1)
    return FeedPage(page.data, page.meta)
}
