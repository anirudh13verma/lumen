package app.lumen.ui.library

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.meta.TmdbImages
import app.lumen.data.scan.isBusy
import app.lumen.ui.components.EmptyLibraryCard
import app.lumen.ui.components.PosterCard
import app.lumen.ui.components.ScanStatusCard
import app.lumen.ui.components.itemSubtitle

@Composable
fun LibraryScreen(
    state: LibraryUiState,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return

    val sorted = remember(state.items) { state.items.sortedBy { it.title.lowercase() } }

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

        val scan = state.scan
        if (scan.isBusy) {
            ScanStatusCard(scan, Modifier.padding(horizontal = 16.dp, vertical = 12.dp))
        } else {
            Text(
                text = if (state.items.isEmpty()) {
                    stringResource(R.string.library_no_videos)
                } else {
                    pluralStringResource(R.plurals.library_item_count, state.items.size, state.items.size)
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
            items(sorted, key = { it.id }) { item ->
                PosterCard(
                    title = item.title,
                    subtitle = itemSubtitle(item),
                    posterUrl = TmdbImages.poster(item.posterPath),
                )
            }
        }
    }
}
