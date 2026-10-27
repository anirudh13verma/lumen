package app.lumen.ui.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.rounded.CheckCircle
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
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
import app.lumen.data.playback.PlayTarget
import app.lumen.data.playback.Progress
import app.lumen.data.playback.isResumable
import app.lumen.data.playback.pickPlayTarget
import app.lumen.data.playback.startPositionFor
import app.lumen.ui.components.ArtworkPlaceholder
import app.lumen.ui.components.itemMeta
import app.lumen.ui.components.rememberFileSize
import coil.compose.AsyncImage

@Composable
fun DetailScreen(
    item: LibraryItem?,
    progress: Map<String, Progress>,
    onPlay: (file: ItemFile, startMs: Long) -> Unit,
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
    val target = remember(item.files, progress) { pickPlayTarget(item.files, progress) }

    LazyColumn(modifier = modifier.fillMaxSize(), contentPadding = PaddingValues(bottom = 24.dp)) {
        item(key = "header") { Header(item, onBack) }

        item(key = "play") {
            Button(
                onClick = { target?.let { onPlay(it.file, it.startMs) } },
                enabled = target != null,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 12.dp),
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(playLabel(item, target), modifier = Modifier.padding(start = 8.dp))
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
            items(currentSeason.episodes, key = { "ep:" + it.uri }) { file ->
                EpisodeRow(file, progress[file.uri]) { onPlay(file, startPositionFor(file, progress)) }
            }
        } else if (item.kind == ItemKind.Movie) {
            item(key = "files-title") {
                Text(
                    text = stringResource(R.string.detail_files),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp),
                )
            }
            items(item.files, key = { "file:" + it.uri }) { file ->
                FileRow(file, progress[file.uri]) { onPlay(file, startPositionFor(file, progress)) }
            }
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
private fun playLabel(item: LibraryItem, target: PlayTarget?): String {
    val episode = target?.file?.episode
    val season = target?.file?.season
    val resume = target?.isResume == true
    return when {
        item.kind == ItemKind.Show && episode != null && season != null ->
            stringResource(
                if (resume) R.string.detail_resume_episode else R.string.detail_play_episode,
                season,
                episode,
            )
        resume -> stringResource(R.string.detail_resume)
        else -> stringResource(R.string.detail_play)
    }
}

@Composable
private fun EpisodeRow(file: ItemFile, progress: Progress?, onClick: () -> Unit) {
    val episode = file.episode
    val end = file.episodeEnd
    val number = when {
        episode == null -> null
        end != null -> stringResource(R.string.detail_episode_range, episode, end)
        else -> stringResource(R.string.detail_episode, episode)
    }
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        leadingContent = number?.let {
            { Text(it, style = MaterialTheme.typography.titleMedium, modifier = Modifier.width(56.dp)) }
        },
        headlineContent = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { FileDetails(file, progress) },
        trailingContent = if (progress?.completed == true) {
            { Icon(Icons.Rounded.CheckCircle, contentDescription = stringResource(R.string.detail_watched)) }
        } else {
            null
        },
    )
}

@Composable
private fun FileRow(file: ItemFile, progress: Progress?, onClick: () -> Unit) {
    ListItem(
        modifier = Modifier.clickable(onClick = onClick),
        headlineContent = { Text(file.name, maxLines = 2, overflow = TextOverflow.Ellipsis) },
        supportingContent = { FileDetails(file, progress) },
    )
}

/** File size, plus a progress bar when the file is half-watched. */
@Composable
private fun FileDetails(file: ItemFile, progress: Progress?) {
    Column {
        Text(rememberFileSize(file.sizeBytes))
        if (progress != null && progress.isResumable()) {
            LinearProgressIndicator(
                progress = { progress.fraction },
                modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            )
        }
    }
}
