package app.lumen.ui.components

import android.text.format.Formatter
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Movie
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage

/** A file size such as "1.4 GB", formatted for the user's locale. */
@Composable
fun rememberFileSize(bytes: Long): String {
    val context = LocalContext.current
    return remember(bytes) { Formatter.formatShortFileSize(context, bytes) }
}

/**
 * Stand-in artwork: a gradient built from the current theme. This is replaced by a
 * Coil image once posters are fetched, and doubles as the loading/fallback state.
 */
@Composable
fun ArtworkPlaceholder(
    aspectRatio: Float,
    modifier: Modifier = Modifier,
) {
    val colors = MaterialTheme.colorScheme
    Box(
        modifier = modifier
            .aspectRatio(aspectRatio)
            .clip(RoundedCornerShape(16.dp))
            .background(Brush.linearGradient(listOf(colors.primaryContainer, colors.tertiaryContainer))),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = Icons.Rounded.Movie,
            contentDescription = null,
            tint = colors.onPrimaryContainer.copy(alpha = 0.5f),
        )
    }
}

/** 2:3 poster (TMDB artwork when [posterUrl] is set) with title and subtitle underneath. */
@Composable
fun PosterCard(
    title: String,
    subtitle: String,
    posterUrl: String?,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            // The placeholder stays underneath, so it shows while the poster loads or if it fails.
            ArtworkPlaceholder(aspectRatio = 2f / 3f, modifier = Modifier.fillMaxWidth())
            if (posterUrl != null) {
                AsyncImage(
                    model = posterUrl,
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(2f / 3f)
                        .clip(RoundedCornerShape(16.dp)),
                )
            }
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}

/** 16:9 card with a resume-progress bar, used for "Continue watching" once playback exists. */
@Composable
fun ContinueWatchingCard(
    title: String,
    subtitle: String,
    progress: Float,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier.width(240.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Box {
            ArtworkPlaceholder(aspectRatio = 16f / 9f, modifier = Modifier.fillMaxWidth())
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 10.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                trackColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.6f),
            )
        }
        Text(
            text = title,
            style = MaterialTheme.typography.labelLarge,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
        )
    }
}
