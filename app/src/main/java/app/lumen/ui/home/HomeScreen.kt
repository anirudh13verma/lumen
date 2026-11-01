package app.lumen.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.BoxWithConstraints
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
import app.lumen.data.playback.ContinueEntry
import app.lumen.data.playback.PlayTarget
import app.lumen.data.playback.buildHeroEntries
import app.lumen.data.playback.buildContinueWatching
import app.lumen.data.scan.isBusy
import app.lumen.ui.components.ContinueRow
import app.lumen.ui.components.EmptyLibraryCard
import app.lumen.ui.components.HeroCarousel
import app.lumen.ui.components.PosterRow
import app.lumen.ui.components.ScanStatusCard
import app.lumen.ui.library.LibraryUiState

private const val RowLimit = 20

private class HomeSections(
    val recent: List<LibraryItem>,
    val movies: List<LibraryItem>,
    val shows: List<LibraryItem>,
)

private fun buildSections(items: List<LibraryItem>): HomeSections {
    val recent = items.sortedByDescending { it.addedAt }
    return HomeSections(
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
    onContinueClick: (ContinueEntry) -> Unit,
    onHeroPlay: (PlayTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return

    val scan = state.scan
    val sections = remember(state.items) { buildSections(state.items) }
    val continueEntries = remember(state.items, state.progress) { buildContinueWatching(state.items, state.progress) }
    val heroEntries = remember(state.items, state.progress) { buildHeroEntries(state.items, state.progress) }

    // The carousel gets half of what is left after the status bar and bottom navigation.
    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val heroHeight = maxHeight * 0.5f

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
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

        if (heroEntries.isNotEmpty()) {
            item(key = "hero") {
                HeroCarousel(entries = heroEntries, height = heroHeight, onOpen = onItemClick, onPlay = onHeroPlay)
            }
        }

        if (scan.isBusy) {
            item(key = "scan") {
                ScanStatusCard(
                    scan,
                    Modifier.padding(start = 16.dp, end = 16.dp, top = if (heroEntries.isEmpty()) 16.dp else 0.dp),
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

        if (continueEntries.isNotEmpty()) {
            item(key = "continue") { ContinueRow(continueEntries, onContinueClick) }
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
    } // LazyColumn
    } // BoxWithConstraints
}
