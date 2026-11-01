package app.lumen.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.collectIsDraggedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.lumen.data.meta.TmdbImages
import app.lumen.R
import app.lumen.data.model.ItemKind
import app.lumen.data.model.LibraryItem
import app.lumen.data.playback.ContinueEntry
import app.lumen.data.playback.HeroEntry
import app.lumen.data.playback.PlayTarget
import kotlinx.coroutines.delay
import coil.compose.AsyncImage

private const val AutoAdvanceMs = 5_000L

/** IMDb's brand yellow. Only ever used for real IMDb ratings. */
private val ImdbYellow = Color(0xFFF5C518)
private val TmdbBlue = Color(0xFF01B4E4)

/**
 * The Home carousel: up to a handful of titles, swipeable, advancing by itself every five seconds
 * (paused while the user is dragging). Each slide has a Play button; tapping elsewhere opens details.
 * Text is always white on a dark fade, so it reads in light and dark themes alike.
 */
@Composable
fun HeroCarousel(
    entries: List<HeroEntry>,
    height: Dp,
    onOpen: (LibraryItem) -> Unit,
    onPlay: (PlayTarget) -> Unit,
    modifier: Modifier = Modifier,
) {
    val pagerState = rememberPagerState(pageCount = { entries.size })
    val dragged by pagerState.interactionSource.collectIsDraggedAsState()

    LaunchedEffect(pagerState.currentPage, dragged, entries.size) {
        if (entries.size > 1 && !dragged) {
            delay(AutoAdvanceMs)
            val next = (pagerState.currentPage + 1) % entries.size
            if (next == 0) pagerState.scrollToPage(0) else pagerState.animateScrollToPage(next)
        }
    }

    Box(modifier = modifier.fillMaxWidth().height(height)) {
        HorizontalPager(
            state = pagerState,
            modifier = Modifier.fillMaxSize(),
            key = { entries[it].item.id },
        ) { page ->
            HeroSlide(entries[page], onOpen = onOpen, onPlay = onPlay)
        }

        if (entries.size > 1) {
            Row(
                modifier = Modifier.align(Alignment.BottomCenter).padding(bottom = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                repeat(entries.size) { index ->
                    val selected = index == pagerState.currentPage
                    Box(
                        Modifier
                            .height(6.dp)
                            .width(if (selected) 18.dp else 6.dp)
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = if (selected) 0.95f else 0.4f)),
                    )
                }
            }
        }
    }
}

@Composable
private fun HeroSlide(
    entry: HeroEntry,
    onOpen: (LibraryItem) -> Unit,
    onPlay: (PlayTarget) -> Unit,
) {
    val item = entry.item
    Box(
        modifier = Modifier
            .fillMaxSize()
            .clickable { onOpen(item) },
    ) {
        ArtworkPlaceholder(aspectRatio = 1f, modifier = Modifier.fillMaxSize(), rounded = false)
        AsyncImage(
            model = TmdbImages.backdropLarge(item.backdropPath) ?: TmdbImages.poster(item.posterPath),
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0.30f to Color.Transparent,
                        0.70f to Color.Black.copy(alpha = 0.65f),
                        1f to Color.Black.copy(alpha = 0.92f),
                    ),
                ),
        )

        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .padding(start = 16.dp, end = 16.dp, bottom = 30.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            TitleArt(item)
            HeroMeta(item)
            item.overview?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.85f),
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Button(
                onClick = { entry.target?.let(onPlay) },
                enabled = entry.target != null,
            ) {
                Icon(Icons.Rounded.PlayArrow, contentDescription = null, modifier = Modifier.size(20.dp))
                Text(
                    text = stringResource(if (entry.target?.isResume == true) R.string.detail_resume else R.string.detail_play),
                    modifier = Modifier.padding(start = 6.dp),
                )
            }
        }
    }
}

/** The title as written on the poster when TMDB has a logo for it, otherwise plain text. */
@Composable
private fun TitleArt(item: LibraryItem) {
    val logoUrl = TmdbImages.logo(item.logoPath)
    var logoFailed by remember(item.id) { mutableStateOf(false) }

    if (logoUrl != null && !logoFailed) {
        AsyncImage(
            model = logoUrl,
            contentDescription = item.title,
            contentScale = ContentScale.Fit,
            alignment = Alignment.CenterStart,
            onError = { logoFailed = true },
            modifier = Modifier.height(84.dp).widthIn(max = 280.dp),
        )
    } else {
        Text(
            text = item.title,
            style = MaterialTheme.typography.headlineMedium,
            color = Color.White,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

@Composable
private fun HeroMeta(item: LibraryItem) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        RatingBadge(imdb = item.imdbRating, tmdb = item.rating)
        val details = buildList {
            item.year?.let { add(it.toString()) }
            if (item.kind == ItemKind.Show) {
                add(pluralStringResource(R.plurals.episode_count, item.fileCount, item.fileCount))
            }
        }
        if (details.isNotEmpty()) {
            Text(
                text = details.joinToString(" · "),
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.85f),
            )
        }
    }
}

/**
 * A yellow "IMDb" box with the rating. TMDB has no IMDb data, so the yellow badge only appears when
 * a real IMDb rating was fetched (OMDb key set); otherwise TMDB's own rating gets a blue "TMDB" box.
 */
@Composable
fun RatingBadge(imdb: Double?, tmdb: Double?, modifier: Modifier = Modifier) {
    val (label, value, color) = when {
        imdb != null -> Triple("IMDb", imdb, ImdbYellow)
        tmdb != null -> Triple("TMDB", tmdb, TmdbBlue)
        else -> return
    }
    Row(modifier = modifier, verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(4.dp))
                .background(color)
                .padding(horizontal = 5.dp, vertical = 1.dp),
        ) {
            Text(text = label, color = Color.Black, fontWeight = FontWeight.ExtraBold, fontSize = 12.sp)
        }
        Text(
            text = stringResource(R.string.rating_value, value),
            style = MaterialTheme.typography.labelLarge,
            color = Color.White,
            fontWeight = FontWeight.SemiBold,
        )
    }
}

/** A titled, horizontally scrolling row of poster cards. */
@Composable
fun PosterRow(
    title: String,
    items: List<LibraryItem>,
    onItemClick: (LibraryItem) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(items, key = { it.id }) { item ->
                PosterCard(
                    title = item.title,
                    subtitle = itemSubtitle(item),
                    posterUrl = TmdbImages.poster(item.posterPath),
                    onClick = { onItemClick(item) },
                    modifier = Modifier.width(120.dp),
                )
            }
        }
    }
}

/** "Continue watching": half-watched titles with a progress bar, newest first. */
@Composable
fun ContinueRow(
    entries: List<ContinueEntry>,
    onEntryClick: (ContinueEntry) -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.home_continue_watching),
            style = MaterialTheme.typography.titleLarge,
            modifier = Modifier.padding(horizontal = 16.dp),
        )
        LazyRow(
            contentPadding = PaddingValues(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            items(entries, key = { it.item.id }) { entry ->
                val season = entry.file.season
                val episode = entry.file.episode
                val minutesLeft = ((entry.progress.durationMs - entry.progress.positionMs) / 60_000L)
                    .toInt().coerceAtLeast(1)
                ContinueWatchingCard(
                    title = entry.item.title,
                    subtitle = if (season != null && episode != null) {
                        stringResource(R.string.continue_episode, season, episode)
                    } else {
                        pluralStringResource(R.plurals.continue_minutes_left, minutesLeft, minutesLeft)
                    },
                    progress = entry.progress.fraction,
                    imageUrl = TmdbImages.backdrop(entry.item.backdropPath ?: entry.item.posterPath),
                    onClick = { onEntryClick(entry) },
                )
            }
        }
    }
}
