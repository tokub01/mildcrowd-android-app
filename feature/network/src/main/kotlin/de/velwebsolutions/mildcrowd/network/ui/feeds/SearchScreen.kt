package de.velwebsolutions.mildcrowd.network.ui.feeds

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Cancel
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.Tag
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import de.velwebsolutions.mildcrowd.core.model.Loadable
import de.velwebsolutions.mildcrowd.design.components.Banner
import de.velwebsolutions.mildcrowd.design.components.DashedNote
import de.velwebsolutions.mildcrowd.design.components.Eyebrow
import de.velwebsolutions.mildcrowd.design.components.FieldError
import de.velwebsolutions.mildcrowd.design.components.MildcrowdButton
import de.velwebsolutions.mildcrowd.design.components.MildcrowdScreen
import de.velwebsolutions.mildcrowd.design.components.SideCard
import de.velwebsolutions.mildcrowd.design.navigation.LocalNavigator
import de.velwebsolutions.mildcrowd.design.navigation.NetworkRoute
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdColors
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdRadius
import de.velwebsolutions.mildcrowd.design.theme.MildcrowdType
import de.velwebsolutions.mildcrowd.network.api.SearchContext
import de.velwebsolutions.mildcrowd.network.ui.PostListState
import de.velwebsolutions.mildcrowd.network.ui.components.CrowdRow
import de.velwebsolutions.mildcrowd.network.ui.components.FeedHandlers
import de.velwebsolutions.mildcrowd.network.ui.components.MemberRow
import de.velwebsolutions.mildcrowd.network.ui.components.NetworkPage
import de.velwebsolutions.mildcrowd.network.ui.components.SectionTitle
import de.velwebsolutions.mildcrowd.network.ui.components.block
import de.velwebsolutions.mildcrowd.network.ui.components.feedSection
import de.velwebsolutions.mildcrowd.network.ui.components.handlers
import kotlinx.coroutines.delay

/**
 * The member search (/app/suche): people and crowds (exact handle first, then alphabetical) and Posts newest first,
 * updated while typing – the results stay on screen while the next ones load. A term that is exactly one #Thema leads to
 * its topic page; every request counts against the search budget of 30 per minute.
 */
@Composable
fun SearchScreen(
    query: String,
    viewModel: SearchViewModel = hiltViewModel<SearchViewModel, SearchViewModel.Factory>(creationCallback = { it.create(query) })
) {
    val state by viewModel.list.state.collectAsStateWithLifecycle()
    val text by viewModel.query.collectAsStateWithLifecycle()
    val navigator = LocalNavigator.current
    LaunchedEffect(text) {
        delay(300)
        viewModel.search()
    }
    MildcrowdScreen(
        title = "Suche",
        message = state.status,
        error = state.actionError?.userMessage,
        onDismissMessage = viewModel.list::dismissStatus,
        onDismissError = viewModel.list::dismissError
    ) {
        val handlers = viewModel.list.handlers(viewModel::launch)
        NetworkPage {
            searchContent(
                query = text,
                state = state,
                handlers = handlers,
                onQuery = viewModel::updateQuery,
                // Return opens a topic directly, like the web's redirect for a single #Thema.
                onSubmit = { state.context?.topic?.let { navigator.open(NetworkRoute.Tag(it)) } }
            )
        }
    }
}

/** The search page: field, people and crowds, then the Posts (or the topic button). */
fun LazyListScope.searchContent(
    query: String,
    state: PostListState<SearchContext>,
    handlers: FeedHandlers,
    onQuery: (String) -> Unit,
    onSubmit: () -> Unit
) {
    val trimmed = query.trim()
    val isSearching = trimmed.length >= 2 && state.context?.searching == true
    block("header") {
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Eyebrow("Suche")
            Text(
                if (isSearching) "Ergebnisse für „$trimmed“" else "Wonach suchst du?",
                style = MildcrowdType.display(28),
                color = MildcrowdColors.Night,
                modifier = Modifier.semantics { heading() }
            )
        }
    }
    block("field") {
        SearchField(query, onQuery, onSubmit, searching = state.isReloading)
        if (query.length > SearchViewModel.MAX_LENGTH) FieldError("Dein Suchbegriff darf höchstens 100 Zeichen lang sein.")
        Spacer(Modifier.size(8.dp))
        Text(
            "Mindestens 2 Zeichen. Mit # davor findest du alle Posts zu einem Thema.",
            style = MildcrowdType.Caption,
            color = MildcrowdColors.Muted
        )
    }
    val topic = state.context?.topic
    if (topic != null && trimmed.length >= 2) {
        block("topic") {
            val navigator = LocalNavigator.current
            MildcrowdButton(
                "Alle Posts zu #$topic",
                { navigator.open(NetworkRoute.Tag(topic)) },
                fullWidth = false,
                leading = {
                    Icon(Icons.Outlined.Tag, contentDescription = null, tint = MildcrowdColors.OnNight, modifier = Modifier.size(18.dp))
                },
                modifier = Modifier.testTag("search.topic")
            )
        }
        return
    }
    val people = if (isSearching) state.context.people.orEmpty() else emptyList()
    val crowds = if (isSearching) state.context.crowds.orEmpty() else emptyList()
    block("people") {
        SideCard("Menschen") {
            if (people.isEmpty()) Text(if (isSearching) "Niemand gefunden." else "Such nach Namen oder @Benutzernamen.")
            people.forEach { MemberRow(it) }
        }
    }
    block("crowds") {
        SideCard("Crowds") {
            if (crowds.isEmpty()) Text(if (isSearching) "Keine Crowd gefunden." else "Such nach Name oder Thema einer Crowd.")
            crowds.forEach { CrowdRow(it) }
        }
    }
    val failure = (state.state as? Loadable.Failed)?.error
    when {
        trimmed.length < 2 -> block("how") {
            DashedNote {
                Text("So sucht Mildcrowd", style = MildcrowdType.BodyBold, color = MildcrowdColors.Night)
                Text(
                    "Neueste Posts zuerst. Keine Ergebnisse, die nach Klicks sortiert sind, und keine, die jemand bezahlt hat.",
                    style = MildcrowdType.Body,
                    color = MildcrowdColors.Ink2
                )
            }
        }

        failure != null -> block("failed") { Banner(SearchViewModel.message(failure)) }

        else -> {
            block("posts.title") { SectionTitle("Posts") }
            feedSection(state, handlers, empty = "Keine Posts mit „$trimmed“.", end = "Das sind alle Treffer.")
        }
    }
}

@Composable
private fun SearchField(query: String, onQuery: (String) -> Unit, onSubmit: () -> Unit, searching: Boolean) {
    val focus = remember { FocusRequester() }
    LaunchedEffect(Unit) { if (query.isEmpty()) runCatching { focus.requestFocus() } }
    val shape = RoundedCornerShape(MildcrowdRadius.XS.dp)
    Row(
        Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .background(MildcrowdColors.Field, shape)
            .border(1.dp, MildcrowdColors.FieldLine, shape)
            .padding(start = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Search, contentDescription = null, tint = MildcrowdColors.Muted)
        Spacer(Modifier.width(8.dp))
        Box(Modifier.weight(1f)) {
            if (query.isEmpty()) Text("Menschen, Crowds, Posts oder #Thema", style = MildcrowdType.Body, color = MildcrowdColors.Muted)
            BasicTextField(
                value = query,
                onValueChange = onQuery,
                singleLine = true,
                textStyle = MildcrowdType.Body.copy(color = MildcrowdColors.Ink),
                cursorBrush = SolidColor(MildcrowdColors.Night),
                keyboardOptions = KeyboardOptions(
                    capitalization = KeyboardCapitalization.None,
                    autoCorrectEnabled = false,
                    imeAction = ImeAction.Search
                ),
                keyboardActions = KeyboardActions(onSearch = { onSubmit() }),
                modifier = Modifier
                    .fillMaxWidth()
                    .focusRequester(focus)
                    .semantics { contentDescription = "Suchbegriff" }
                    .testTag("search.field")
            )
        }
        if (searching) {
            CircularProgressIndicator(
                Modifier.padding(horizontal = 8.dp).size(18.dp).semantics { contentDescription = "Sucht …" },
                color = MildcrowdColors.Night,
                strokeWidth = 2.dp
            )
        }
        if (query.isNotEmpty()) {
            IconButton(onClick = { onQuery("") }) {
                Icon(Icons.Filled.Cancel, contentDescription = "Suchbegriff löschen", tint = MildcrowdColors.Muted)
            }
        }
    }
}
