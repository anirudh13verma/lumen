package app.lumen.ui.player

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.displayCutout
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.VolumeUp
import androidx.compose.material.icons.rounded.Audiotrack
import androidx.compose.material.icons.rounded.BrightnessMedium
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Forward10
import androidx.compose.material.icons.rounded.Pause
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Replay10
import androidx.compose.material.icons.rounded.Subtitles
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.playback.formatClock
import app.lumen.data.playback.formatSeekDelta
import kotlin.math.roundToInt

/** Transient feedback shown in the middle of the video while a gesture is in progress. */
sealed interface Hud {
    data class Seek(val deltaMs: Long, val targetMs: Long, val durationMs: Long) : Hud
    data class Level(val isBrightness: Boolean, val fraction: Float) : Hud
    data class Skip(val forward: Boolean) : Hud
}

/** One selectable audio or subtitle track. */
class TrackOption(val label: String, val selected: Boolean, val onSelect: () -> Unit)

@Composable
fun PlayerHud(hud: Hud?, modifier: Modifier = Modifier) {
    if (hud == null) return
    val alignment = when {
        hud is Hud.Skip && !hud.forward -> Alignment.CenterStart
        hud is Hud.Skip -> Alignment.CenterEnd
        else -> Alignment.Center
    }
    Box(modifier = modifier.fillMaxSize().padding(horizontal = 48.dp), contentAlignment = alignment) {
        Surface(color = Color.Black.copy(alpha = 0.6f), shape = RoundedCornerShape(16.dp), contentColor = Color.White) {
            when (hud) {
                is Hud.Seek -> Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = formatClock(hud.targetMs),
                        style = MaterialTheme.typography.headlineLarge,
                    )
                    Text(
                        text = formatSeekDelta(hud.deltaMs),
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White.copy(alpha = 0.8f),
                    )
                }
                is Hud.Level -> Row(
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Icon(
                        imageVector = if (hud.isBrightness) Icons.Rounded.BrightnessMedium else Icons.AutoMirrored.Rounded.VolumeUp,
                        contentDescription = null,
                    )
                    LinearProgressIndicator(progress = { hud.fraction }, modifier = Modifier.width(120.dp))
                    Text("${(hud.fraction * 100).roundToInt()}%")
                }
                is Hud.Skip -> Text(
                    text = if (hud.forward) "+10 s" else "−10 s",
                    style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp),
                )
            }
        }
    }
}

/** Top bar (close, title, audio, subtitles), centre transport buttons and a seek bar. */
@Composable
fun PlayerControls(
    visible: Boolean,
    title: String,
    isPlaying: Boolean,
    positionMs: Long,
    durationMs: Long,
    onClose: () -> Unit,
    onPlayPause: () -> Unit,
    onSkip: (deltaMs: Long) -> Unit,
    onScrub: (Long) -> Unit,
    onScrubFinished: () -> Unit,
    onAudio: () -> Unit,
    onSubtitles: () -> Unit,
    modifier: Modifier = Modifier,
) {
    AnimatedVisibility(visible = visible, enter = fadeIn(), exit = fadeOut(), modifier = modifier) {
        CompositionLocalProvider(LocalContentColor provides Color.White) {
            Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.displayCutout)) {
                Row(
                    modifier = Modifier
                        .align(Alignment.TopCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                        .padding(horizontal = 8.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onClose) {
                        Icon(Icons.Rounded.Close, contentDescription = stringResource(R.string.player_close))
                    }
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    IconButton(onClick = onAudio) {
                        Icon(Icons.Rounded.Audiotrack, contentDescription = stringResource(R.string.player_audio))
                    }
                    IconButton(onClick = onSubtitles) {
                        Icon(Icons.Rounded.Subtitles, contentDescription = stringResource(R.string.player_subtitles))
                    }
                }

                Row(
                    modifier = Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                ) {
                    IconButton(onClick = { onSkip(-10_000) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.Replay10, contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                    IconButton(onClick = onPlayPause, modifier = Modifier.size(72.dp)) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
                            contentDescription = stringResource(if (isPlaying) R.string.player_pause else R.string.player_play),
                            modifier = Modifier.size(56.dp),
                        )
                    }
                    IconButton(onClick = { onSkip(10_000) }, modifier = Modifier.size(56.dp)) {
                        Icon(Icons.Rounded.Forward10, contentDescription = null, modifier = Modifier.size(36.dp))
                    }
                }

                Row(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    Text(formatClock(positionMs), style = MaterialTheme.typography.labelLarge)
                    Slider(
                        value = positionMs.coerceIn(0L, maxOf(durationMs, 1L)).toFloat(),
                        onValueChange = { onScrub(it.toLong()) },
                        onValueChangeFinished = onScrubFinished,
                        valueRange = 0f..maxOf(durationMs, 1L).toFloat(),
                        modifier = Modifier.weight(1f),
                    )
                    Text(formatClock(durationMs), style = MaterialTheme.typography.labelLarge)
                }
            }
        }
    }
}

/** Side sheet listing audio or subtitle tracks. Drawn inside the screen, not as a dialog, so the system bars stay hidden. */
@Composable
fun TrackPanel(
    title: String,
    options: List<TrackOption>,
    onDismiss: () -> Unit,
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.5f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = onDismiss),
    ) {
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .align(Alignment.CenterEnd)
                .windowInsetsPadding(WindowInsets.displayCutout)
                .padding(16.dp)
                .widthIn(max = 320.dp)
                .fillMaxHeight(0.9f)
                .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null, onClick = {}),
        ) {
            Column {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.padding(start = 20.dp, top = 16.dp, end = 20.dp, bottom = 8.dp),
                )
                if (options.isEmpty()) {
                    Text(
                        text = stringResource(R.string.player_no_tracks),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                    )
                }
                LazyColumn {
                    items(options) { option ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    option.onSelect()
                                    onDismiss()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            RadioButton(selected = option.selected, onClick = null)
                            Text(
                                text = option.label,
                                style = MaterialTheme.typography.bodyLarge,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(start = 12.dp),
                            )
                        }
                    }
                }
            }
        }
    }
}
