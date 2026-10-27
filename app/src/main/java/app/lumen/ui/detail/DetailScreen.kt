package app.lumen.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.meta.TmdbImages
import app.lumen.data.model.ItemFile
import app.lumen.data.model.ItemKind
import app.lumen.data.model.LibraryItem
import app.lumen.data.model.groupBySeason
import app.lumen.ui.components.ArtworkPlaceholder
import app.lumen.ui.components.itemMeta
import app.lumen.ui.components.rememberFileSize
import coil.compose.AsyncImage

@Composable
fun DetailScreen(
    item: LibraryItem?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    if (item == null) {
        // The title vanished (for example the folder was removed while this screen was open).
        Column(
            modifier = modifier.fillMaxSize().padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp, Alignment.CenterVertically),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            Text(stringResource(R.string.detail_missing), style = MaterialTheme.typography.bodyLarge)
            Button(onClick = onBack) { Text(stringResource(R.string.detail_back)) }
        }
        return
    }

    val seasons = remember(item.files) { if (item.kind == ItemKind.Show) groupBySeason(item.files) else emptyList() }
    var selectedSeason by rememberSaveable(item.id) { mutableIntStateOf(seasons.firstOrNull()?.season ?: 0) }
    val currentSeason = seasons.firstOrNull { it.season == selectedSeason } ?: seasons.firstOrNull()

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { Header(item, onBack) }

        item(key = "play") {
            Column(
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                // Playback arrives with the player milestone.
                Button(onClick = {}, enabled = false, modifier = Modifier.fillMaxWidth()) {
                    Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                    Text(stringResource(R.string.detail_play), modifier = Modifier.padding(start = 8.dp))
                }
                Text(
                    text = stringResource(R.string.detail_play_soon),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        item(key = "overview") {
            Text(
                text = item.overview ?: stringResource(
                    if (item.matched) R.string.detail_no_overview else R.string.detail_unmatched,
                ),
                style = MaterialTheme.typography.bodyLarge,
                color = if (item.overview == null) {
                    MaterialTheme.colorScheme.onSurfaceVariant
                } else {
                    MaterialTheme.colorScheme.onSurface
                },
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            )
        }

        if (item.kind == ItemKind.Show && currentSeason != null) {
            item(key = "seasons") {
                LazyRow(
                    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                ) {
                    items(seasons, key = { it.season }) { group ->
                        FilterChip(
                            selected = group.season == currentSeason.season,
                            onClick = { selectedSeason = group.season },
                            label = {
                                Text(
                                    if (group.season == 0) {
                                        stringResource(R.string.detail_season_other)
                                    } else {
                                        stringResource(R.string.detail_season, group.season)
                                    },
                                )
                            },
                        )
                    }
                }
            }
            items(currentSeason.episodes, key = { "ep:" + it.uri }) { file -> EpisodeRow(file) }
        } else if (item.kind == ItemKind.Movie) {
            item(key = "files-title") {
                Text(
                    text = stringResource(R.string.detail_files),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }
            items(item.files, key = { "file:" + it.uri }) { file -> FileRow(file) }
        }
    }
}

@Composable
private fun Header(item: LibraryItem, onBack: () -> Unit) {
    val background = MaterialTheme.colorScheme.background
    Column {
        Box(Modifier.fillMaxWidth().aspectRatio(16f / 9f)) {
            ArtworkPlaceholder(aspectRatio = 16f / 9f, modifier = Modifier.fillMaxSize(), rounded = false)
            AsyncImage(
                model = TmdbImages.backdrop(item.backdropPath ?: item.posterPath),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize(),
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(Brush.verticalGradient(0.5f to Color.Transparent, 1f to background)),
            )
            IconButton(
                onClick = onBack,
                modifier = Modifier.align(Alignment.TopStart).padding(4.dp),
            ) {
                Icon(Icons.AutoMirrored.Rounded.ArrowBack, contentDescription = stringResource(R.string.detail_back))
            }
        }
        Column(
            modifier = Modifier.padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(text = item.title, style = MaterialTheme.typography.headlineMedium)
            Text(
                text = itemMeta(item),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun EpisodeRow(file: ItemFile) {
    val episode = file.episode
    val end = file.episodeEnd
    val number = when {
        episode == null -> null
        end != null -> stringResource(R.string.detail_episode_range, episode, end)
        else -> stringResource(R.string.detail_episode, episode)
    }
    ListItem(
        leadingContent = number?.let {
            { Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(56.dp)) }
        },
        headlineContent = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(rememberFileSize(file.sizeBytes)) },
    )
}

@Composable
private fun FileRow(file: ItemFile) {
    ListItem(
        headlineContent = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { Text(rememberFileSize(file.sizeBytes)) },
    )
}
