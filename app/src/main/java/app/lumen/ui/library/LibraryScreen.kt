package app.lumen.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Clear
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.meta.TmdbImages
import app.lumen.data.model.LibraryFilter
import app.lumen.data.model.LibraryItem
import app.lumen.data.model.filterItems
import app.lumen.data.scan.isBusy
import app.lumen.ui.components.EmptyLibraryCard
import app.lumen.ui.components.PosterCard
import app.lumen.ui.components.ScanStatusCard
import app.lumen.ui.components.itemSubtitle

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAddFolder: () -> Unit,
    onItemClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return

    var query by rememberSaveable { mutableStateOf("") }
    var filter by rememberSaveable { mutableStateOf(LibraryFilter.All) }

    val sorted = remember(state.items) { state.items.sortedBy { it.title.lowercase() } }
    val visible = remember(sorted, filter, query) { filterItems(sorted, filter, query) }

    Column(modifier = modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.library_title),
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp),
        )

        if (state.folders.isEmpty()) {
            EmptyLibraryCard(
                onAddFolder = onAddFolder,
                modifier = Modifier.padding(16.dp),
            )
            return@Column
        }

        OutlinedTextField(
            value = query,
            onValueChange = { query = it },
            singleLine = true,
            placeholder = { Text(stringResource(R.string.library_search_hint)) },
            leadingIcon = { Icon(Icons.Rounded.Search, contentDescription = null) },
            trailingIcon = {
                if (query.isNotEmpty()) {
                    IconButton(onClick = { query = "" }) {
                        Icon(Icons.Rounded.Clear, contentDescription = stringResource(R.string.library_search_clear))
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
        )

        Row(
            modifier = Modifier.padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            val chips = listOf(
                LibraryFilter.All to R.string.filter_all,
                LibraryFilter.Movies to R.string.filter_movies,
                LibraryFilter.Shows to R.string.filter_shows,
            )
            chips.forEach { (value, labelRes) ->
                FilterChip(
                    selected = filter == value,
                    onClick = { filter = value },
                    label = { Text(stringResource(labelRes)) },
                )
            }
        }

        val scan = state.scan
        if (scan.isBusy) {
            ScanStatusCard(scan, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        } else {
            Text(
                text = when {
                    state.items.isEmpty() -> stringResource(R.string.library_no_videos)
                    visible.isEmpty() -> stringResource(R.string.library_no_results)
                    else -> pluralStringResource(R.plurals.library_item_count, visible.size, visible.size)
                },
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            )
        }

        LazyVerticalGrid(
            columns = GridCells.Adaptive(minSize = 110.dp),
            contentPadding = PaddingValues(start = 16.dp, end = 16.dp, bottom = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize(),
        ) {
            items(visible, key = { it.id }) { item ->
                PosterCard(
                    title = item.title,
                    subtitle = itemSubtitle(item),
                    posterUrl = TmdbImages.poster(item.posterPath),
                    onClick = { onItemClick(item) },
                )
            }
        }
    }
}
