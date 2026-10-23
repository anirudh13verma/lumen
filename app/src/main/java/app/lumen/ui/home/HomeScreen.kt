package app.lumen.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.scan.ScanState
import app.lumen.ui.components.EmptyLibraryCard
import app.lumen.ui.components.PosterCard
import app.lumen.ui.components.ScanStatusCard
import app.lumen.ui.components.rememberFileSize
import app.lumen.ui.library.LibraryUiState

private const val RecentlyAddedLimit = 20

@Composable
fun HomeScreen(
    state: LibraryUiState,
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (!state.loaded) return

    val scan = state.scan

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp),
    ) {
        if (state.folders.isEmpty()) {
            item {
                EmptyLibraryCard(
                    onAddFolder = onAddFolder,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
            return@LazyColumn
        }

        if (scan is ScanState.Scanning) {
            item { ScanStatusCard(scan, Modifier.padding(horizontal = 16.dp)) }
        }

        if (state.files.isNotEmpty()) {
            item {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = stringResource(R.string.home_recently_added),
                        style = MaterialTheme.typography.titleLarge,
                        modifier = Modifier.padding(horizontal = 16.dp),
                    )
                    LazyRow(
                        contentPadding = PaddingValues(horizontal = 16.dp),
                        horizontalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(state.files.take(RecentlyAddedLimit), key = { it.uri }) { file ->
                            PosterCard(
                                title = file.title,
                                subtitle = rememberFileSize(file.sizeBytes),
                                modifier = Modifier.width(120.dp),
                            )
                        }
                    }
                }
            }
        } else if (scan !is ScanState.Scanning) {
            item {
                Text(
                    text = stringResource(R.string.library_no_videos),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp),
                )
            }
        }
    }
}
