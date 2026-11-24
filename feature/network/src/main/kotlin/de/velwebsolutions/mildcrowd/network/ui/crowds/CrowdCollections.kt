package de.velwebsolutions.mildcrowd.network.ui.crowds

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.core.network.ApiError
import de.velwebsolutions.mildcrowd.design.components.AsyncMildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.ConsentCheck
import de.velwebsolutions.mildcrowd.design.components.LoadableView
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButtonKind
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.MildcrowdTextField
import de.velwebsolutions.mildcrowd.design.components.PageHeader
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.NetworkSignals
import de.velwebsolutions.mildcrowd.network.api.CrowdCollection
import de.velwebsolutions.mildcrowd.network.api.CrowdCollections
import de.velwebsolutions.mildcrowd.network.api.NoContext
import de.velwebsolutions.mildcrowd.network.data.NetworkRepository
import de.velwebsolutions.mildcrowd.network.ui.NetworkViewModel
import de.velwebsolutions.mildcrowd.network.ui.PostListState
import de.velwebsolutions.mildcrowd.network.ui.apiError
import de.velwebsolutions.mildcrowd.network.ui.attempt
import de.velwebsolutions.mildcrowd.network.ui.components.ButtonFlow
import de.velwebsolutions.mildcrowd.network.ui.components.ConfirmDialog
import de.velwebsolutions.mildcrowd.network.ui.components.FeedHandlers
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.feedSection
import de.velwebsolutions.mildcrowd.network.ui.retry
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** What the crowd page can do with its Sammlungen. */
class CrowdCollectionActions(
    val onChoose: (Int?) -> Unit,
    val onOpenForm: (CrowdCollection?) -> Unit,
    val onEditForm: ((CollectionForm) -> CollectionForm) -> Unit,
    val onCloseForm: () -> Unit,
    val onSave: suspend () -> Unit,
    val onDelete: (CrowdCollection) -> Unit,
    val onAllowMembers: (Boolean) -> Unit
) {
    companion object {
        val None = CrowdCollectionActions({}, {}, {}, {}, {}, {}, {})
    }
}

/** The collection part of the crowd page: the list, the chosen one with its Posts, and the form. */
class CrowdCollectionsUi(
    val collections: CrowdCollections?,
    val chosenId: Int?,
    val shown: PostListState<NoContext>?,
    val shownHandlers: FeedHandlers?,
    val form: CollectionForm?,
    val actions: CrowdCollectionActions
) {
    /** The chosen collection as listed. */
    val chosen: CrowdCollection? get() = collections?.collections?.firstOrNull { it.id == chosenId }

    companion object {
        val None = CrowdCollectionsUi(null, null, null, null, null, CrowdCollectionActions.None)
    }
}

/**
 * „Sammlungen“ (`#sammlungen`): „Sammlung anlegen“ for those allowed, the form, the collections as chips („Rezepte · 4“),
 * the chosen one with its Posts, and – for hosts – „Mitglieder dürfen Sammlungen anlegen“.
 */
internal fun LazyListScope.collectionsSection(ui: CrowdCollectionsUi, isHost: Boolean) {
    val collections = ui.collections ?: return
    val actions = ui.actions
    block("collections") {
        var confirmDelete by remember { mutableStateOf<CrowdCollection?>(null) }
        Column(Modifier.fillMaxWidth().testTag("crowd.collections"), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                SectionTitle("Sammlungen", Modifier.weight(1f))
                if (collections.canCreate && ui.form == null) {
                    MildcrowdButton(
                        "Sammlung anlegen",
                        { actions.onOpenForm(null) },
                        kind = MildcrowdButtonKind.Secondary,
                        fullWidth = false,
                        modifier = Modifier.testTag("collection.create")
                    )
                }
            }
            ui.form?.let { CollectionFormView(it, actions) }
            if (collections.collections.isEmpty()) {
                Text(
                    "Noch keine Sammlung. In Sammlungen hebt die Crowd gemeinsam Posts auf, etwa Rezepte oder Tipps.",
                    style = MildcrowdType.Body,
                    color = MildcrowdColors.Muted
                )
            }
            ButtonFlow {
                collections.collections.forEach { collection ->
                    val chosen = collection.id == ui.chosenId
                    MildcrowdButton(
                        "${collection.name} · ${collection.postsCount}",
                        { actions.onChoose(collection.id) },
                        kind = if (chosen) MildcrowdButtonKind.Primary else MildcrowdButtonKind.Secondary,
                        fullWidth = false,
                        modifier = Modifier.semantics { selected = chosen }.testTag("collection.${collection.id}")
                    )
                }
            }
            ui.chosen?.let { chosen ->
                Text(chosen.name, style = MildcrowdType.heading(19), color = MildcrowdColors.Night)
                chosen.description?.let { Text(it, style = MildcrowdType.Body, color = MildcrowdColors.Ink2) }
                if (chosen.canEdit) {
                    ButtonFlow {
                        MildcrowdButton("Ändern", {
                            actions.onOpenForm(chosen)
                        }, kind = MildcrowdButtonKind.Secondary, fullWidth = false, modifier = Modifier.testTag("collection.edit"))
                        MildcrowdButton("Sammlung löschen", {
                            confirmDelete = chosen
                        }, kind = MildcrowdButtonKind.Secondary, fullWidth = false, modifier = Modifier.testTag("collection.delete"))
                    }
                }
            }
            if (isHost) {
                ConsentCheck(
                    "Mitglieder dürfen Sammlungen anlegen",
                    collections.membersCreateCollections,
                    actions.onAllowMembers,
                    Modifier.testTag("collection.allowMembers")
                )
            }
        }
        confirmDelete?.let { collection ->
            ConfirmDialog(
                "Sammlung löschen?",
                "Löschen",
                onConfirm = { actions.onDelete(collection) },
                onDismiss = { confirmDelete = null },
                message = "Ihre Posts bleiben in der Crowd."
            )
        }
    }
    val shown = ui.shown
    val handlers = ui.shownHandlers
    if (ui.chosen != null && shown != null && handlers != null) {
        feedSection(
            shown,
            handlers,
            empty = "Noch kein Post in dieser Sammlung. Füge einen über „⋯“ an einem Post hinzu.",
            keyPrefix = "collection"
        )
    }
}

/** „Neue Sammlung“ / „Sammlung ändern“: name and description. */
@Composable
internal fun CollectionFormView(form: CollectionForm, actions: CrowdCollectionActions) {
    val creating = form.editing == null
    Column(Modifier.fillMaxWidth().testTag("collection.form"), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(if (creating) "Neue Sammlung" else "Sammlung ändern", style = MildcrowdType.heading(17), color = MildcrowdColors.Night)
        MildcrowdTextField(
            "Name",
            form.name,
            { value -> actions.onEditForm { it.copy(name = value) } },
            Modifier.testTag("collection.name"),
            prompt = "z. B. Rezepte",
            error = form.error?.message("name"),
            maxLength = 60
        )
        MildcrowdTextField(
            "Beschreibung (optional)",
            form.description,
            { value -> actions.onEditForm { it.copy(description = value) } },
            Modifier.testTag("collection.description"),
            error = form.error?.message("description"),
            lines = 2..5,
            maxLength = 300
        )
        form.error?.takeIf {
            it !is ApiError.Validation
        }?.let { Text(it.userMessage, style = MildcrowdType.Small, color = MildcrowdColors.Danger) }
        ButtonFlow {
            AsyncMildcrowdButton(
                if (creating) "Sammlung anlegen" else "Änderung speichern",
                actions.onSave,
                fullWidth = false,
                modifier = Modifier.testTag("collection.save")
            )
            MildcrowdButton(
                "Abbrechen",
                actions.onCloseForm,
                kind = MildcrowdButtonKind.Secondary,
                fullWidth = false,
                modifier = Modifier.testTag("collection.close")
            )
        }
    }
}

/** „Zu Sammlung hinzufügen“ (a sheet): the collections of the Post's crowd it may go into. */
@HiltViewModel(assistedFactory = CollectPostViewModel.Factory::class)
class CollectPostViewModel @AssistedInject constructor(
    @Assisted val postId: Int,
    @Assisted val slug: String,
    private val repository: NetworkRepository
) : NetworkViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(postId: Int, slug: String): CollectPostViewModel
    }

    private val mutableCollections = MutableStateFlow<Loadable<List<CrowdCollection>>>(Loadable.Idle)

    /** The collections a Post may go into. */
    val collections: StateFlow<Loadable<List<CrowdCollection>>> = mutableCollections.asStateFlow()
    private val mutableDone = MutableStateFlow(false)

    /** Set once the Post went into a collection: the sheet closes. */
    val done: StateFlow<Boolean> = mutableDone.asStateFlow()

    suspend fun load() {
        mutableCollections.load { repository.collections(slug).collections.filter { it.canAdd } }
    }

    /** Adds the Post; the tab shows the confirmation, crowd pages show the collections again. */
    suspend fun add(collection: CrowdCollection) {
        val result = attempt { repository.collectPost(collection.id, postId) }
        mutableError.value = result.apiError
        val answer = result.getOrNull() ?: return
        NetworkSignals.confirmation.value = answer.message
        NetworkSignals.didChangeCollections()
        mutableDone.value = true
    }
}

/** The sheet „Zu Sammlung hinzufügen“. */
@Composable
fun CollectPostScreen(
    postId: Int,
    slug: String,
    viewModel: CollectPostViewModel = hiltViewModel<CollectPostViewModel, CollectPostViewModel.Factory>(
        key = "collect-$postId-$slug",
        creationCallback = { it.create(postId, slug) }
    )
) {
    val collections by viewModel.collections.collectAsStateWithLifecycle()
    val done by viewModel.done.collectAsStateWithLifecycle()
    val error by viewModel.error.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(Unit) { viewModel.load() }
    LaunchedEffect(done) { if (done) navigator.dismissSheet() }
    MildcrowdScreen(title = "Zu Sammlung hinzufügen", error = error?.userMessage, onDismissError = viewModel::dismissError) {
        LoadableView(collections, onRetry = viewModel.retry(viewModel::load)) { list ->
            NetworkPage {
                block("header") { PageHeader("Zu Sammlung hinzufügen", eyebrow = "Sammlungen") }
                block("list") {
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (list.isEmpty()) {
                            Text(
                                "Hier gibt es noch keine Sammlung, in die du ihn legen kannst.",
                                style = MildcrowdType.Body,
                                color = MildcrowdColors.Muted
                            )
                        }
                        list.forEach { collection ->
                            AsyncMildcrowdButton(
                                "${collection.name} · ${collection.postsCount}",
                                { viewModel.add(collection) },
                                kind = MildcrowdButtonKind.Secondary,
                                modifier = Modifier.testTag("collect.${collection.id}")
                            )
                        }
                    }
                }
            }
        }
    }
}
