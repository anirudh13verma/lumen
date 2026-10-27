package app.lumen.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.model.ItemKind
import app.lumen.data.model.LibraryItem
import app.lumen.data.scan.isBusy
import app.lumen.ui.components.EmptyLibraryCard
import app.lumen.ui.components.HeroBanner
import app.lumen.ui.components.PosterRow
import app.lumen.ui.components.ScanStatusCard
import app.lumen.ui.library.LibraryUiState

private const val RowLimit = 20

private class HomeSections(
    val featured: LibraryItem?,
    val recent: List<LibraryItem>,
    val movies: List<LibraryItem>,
    val shows: List<LibraryItem>,
)

private fun buildSections(items: List<LibraryItem>): HomeSections {
    val recent = items.sortedByDescending { it.addedAt }
    return HomeSections(
        // Newest title that has a backdrop to show; the banner is skipped when none do.
        featured = recent.firstOrNull { it.backdropPath != null },
        recent = recent.take(RowLimit),
        movies = items.filter { it.kind == ItemKind.Movie }.sortedBy { it.title.lowercase() }.take(RowLimit),
        shows = items.filter { it.kind == ItemKind.Show }.sortedBy { it.title.lowercase() }.take(RowLimit),
    )
}

@Composable
fun HomeScreen(
    state: LibraryUiState,
    onAddFolder: () -> Unit,
    onItemClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return

    val scan = state.scan
    val sections = remember(state.items) { buildSections(state.items) }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(bottom = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (state.folders.isEmpty()) {
            item {
                EmptyLibraryCard(
                    onAddFolder = onAddFolder,
                    modifier = Modifier.padding(16.dp),
                )
            }
            return@LazyColumn
        }

        sections.featured?.let { featured ->
            item(key = "hero") { HeroBanner(item = featured, onClick = { onItemClick(featured) }) }
        }

        if (scan.isBusy) {
            item(key = "scan") {
                ScanStatusCard(
                    scan,
                    Modifier.padding(start = 16.dp, end = 16.dp, top = if (sections.featured == null) 16.dp else 0.dp),
                )
            }
        }

        if (state.items.isEmpty()) {
            if (!scan.isBusy) {
                item(key = "empty") {
                    Text(
                        text = stringResource(R.string.library_no_videos),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                }
            }
            return@LazyColumn
        }

        item(key = "recent") {
            PosterRow(stringResource(R.string.home_recently_added), sections.recent, onItemClick)
        }
        if (sections.movies.isNotEmpty()) {
            item(key = "movies") {
                PosterRow(stringResource(R.string.home_movies), sections.movies, onItemClick)
            }
        }
        if (sections.shows.isNotEmpty()) {
            item(key = "shows") {
                PosterRow(stringResource(R.string.home_shows), sections.shows, onItemClick)
            }
        }
    }
}
