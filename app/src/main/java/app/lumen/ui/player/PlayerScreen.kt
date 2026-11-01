package app.lumen.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import android.graphics.Color as AndroidColor
import android.media.AudioManager
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.common.Player
import androidx.media3.common.Tracks
import androidx.media3.ui.AspectRatioFrameLayout
import androidx.media3.ui.CaptionStyleCompat
import androidx.media3.ui.PlayerView
import app.lumen.LumenApplication
import app.lumen.R
import app.lumen.player.Media3Engine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

private const val SaveIntervalMs = 5_000L
private const val ControlsTimeoutMs = 3_500L
private const val SkipMs = 10_000L

private enum class Panel { Audio, Subtitles }

/**
 * Fullscreen video with Lumen's own controls: swipe sideways to seek (with a live preview),
 * swipe up/down for volume (right) and brightness (left), double-tap to skip 10 s, a close button,
 * and audio/subtitle pickers. Subtitles are drawn as white text with a dark outline instead of a
 * solid box. The position is saved every few seconds, when the app is backgrounded and on exit.
 */
@Composable
fun PlayerScreen(
    documentUri: String,
    title: String,
    startMs: Long,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val container = (context.applicationContext as LumenApplication).container

    val engine = remember(documentUri) { Media3Engine(context).also { it.load(documentUri, startMs) } }
    val player = engine.player

    fun saveNow() {
        val position = engine.positionMs
        val duration = engine.durationMs
        container.applicationScope.launch { container.playbackRepository.save(documentUri, position, duration) }
    }

    var isPlaying by remember(engine) { mutableStateOf(false) }
    var position by remember(engine) { mutableLongStateOf(0L) }
    var duration by remember(engine) { mutableLongStateOf(0L) }
    var scrub by remember(engine) { mutableStateOf<Long?>(null) }
    var tracks by remember(engine) { mutableStateOf(Tracks.EMPTY) }
    var controlsVisible by remember { mutableStateOf(true) }
    var interaction by remember { mutableIntStateOf(0) }
    var panel by remember { mutableStateOf<Panel?>(null) }
    var hud by remember { mutableStateOf<Hud?>(null) }

    DisposableEffect(engine) {
        engine.play()
        val listener = object : Player.Listener {
            override fun onIsPlayingChanged(playing: Boolean) {
                isPlaying = playing
            }

            override fun onTracksChanged(newTracks: Tracks) {
                tracks = newTracks
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            saveNow()
            engine.release()
        }
    }

    // Keep the seek bar and times fresh.
    LaunchedEffect(engine) {
        while (true) {
            position = engine.positionMs
            duration = engine.durationMs
            delay(250)
        }
    }

    // Periodic save while playing.
    LaunchedEffect(engine) {
        while (true) {
            delay(SaveIntervalMs)
            if (engine.isPlaying) saveNow()
        }
    }

    // Pause and save when the app is backgrounded; the user presses play when they return.
    LifecycleEventEffect(Lifecycle.Event.ON_STOP) {
        engine.pause()
        saveNow()
    }

    // Hide the controls a few seconds after the last touch, but only while playing.
    LaunchedEffect(controlsVisible, isPlaying, interaction, panel, scrub) {
        if (controlsVisible && isPlaying && panel == null && scrub == null) {
            delay(ControlsTimeoutMs)
            controlsVisible = false
        }
    }

    // Short-lived overlays (skip, volume, brightness) fade after a moment; the seek preview does not.
    LaunchedEffect(hud) {
        if (hud != null && hud !is Hud.Seek) {
            delay(800)
            hud = null
        }
    }

    // Immersive landscape playback, restored on exit.
    DisposableEffect(Unit) {
        val activity = context.findActivity()
        val window = activity?.window
        val controller = window?.let { WindowCompat.getInsetsController(it, view) }
        val previousOrientation = activity?.requestedOrientation

        activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_SENSOR_LANDSCAPE
        controller?.systemBarsBehavior = WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
        controller?.hide(WindowInsetsCompat.Type.systemBars())
        view.keepScreenOn = true

        onDispose {
            view.keepScreenOn = false
            controller?.show(WindowInsetsCompat.Type.systemBars())
            window?.let {
                val params = it.attributes
                params.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE
                it.attributes = params
            }
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    val audioManager = remember { context.getSystemService(Context.AUDIO_SERVICE) as AudioManager }
    var volumeLevel by remember { mutableFloatStateOf(0f) }
    var brightnessLevel by remember { mutableFloatStateOf(0.5f) }

    fun skip(deltaMs: Long) {
        val limit = if (engine.durationMs > 0) engine.durationMs else Long.MAX_VALUE
        engine.seekTo((engine.positionMs + deltaMs).coerceIn(0L, limit))
        interaction++
    }

    fun togglePlay() {
        if (engine.isPlaying) engine.pause() else engine.play()
        interaction++
    }

    BackHandler {
        if (panel != null) panel = null else onExit()
    }

    Box(Modifier.fillMaxSize().background(Color.Black)) {
        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { ctx ->
                PlayerView(ctx).apply {
                    this.player = player
                    useController = false
                    resizeMode = AspectRatioFrameLayout.RESIZE_MODE_FIT
                    // White text with a dark outline: readable on any scene without the solid box.
                    subtitleView?.apply {
                        setApplyEmbeddedStyles(false)
                        setStyle(
                            CaptionStyleCompat(
                                AndroidColor.WHITE,
                                AndroidColor.TRANSPARENT,
                                AndroidColor.TRANSPARENT,
                                CaptionStyleCompat.EDGE_TYPE_OUTLINE,
                                AndroidColor.BLACK,
                                null,
                            ),
                        )
                    }
                }
            },
            onRelease = { it.player = null },
        )

        Box(
            Modifier
                .fillMaxSize()
                .then(
                    if (panel != null) {
                        Modifier
                    } else {
                        Modifier.playerGestures(
                            onTap = {
                                controlsVisible = !controlsVisible
                                interaction++
                            },
                            onDoubleTap = { region ->
                                when (region) {
                                    TapRegion.Left -> {
                                        skip(-SkipMs)
                                        hud = Hud.Skip(forward = false)
                                    }
                                    TapRegion.Right -> {
                                        skip(SkipMs)
                                        hud = Hud.Skip(forward = true)
                                    }
                                    TapRegion.Center -> togglePlay()
                                }
                            },
                            seekStart = { engine.positionMs to engine.durationMs },
                            onSeekPreview = { delta, target, length -> hud = Hud.Seek(delta, target, length) },
                            onSeekCommit = { target ->
                                engine.seekTo(target)
                                position = target
                                hud = null
                            },
                            onSeekCancel = { hud = null },
                            onLevelStart = { rightSide ->
                                if (rightSide) {
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC).coerceAtLeast(1)
                                    volumeLevel = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC).toFloat() / max
                                } else {
                                    val current = context.findActivity()?.window?.attributes?.screenBrightness ?: -1f
                                    brightnessLevel = if (current >= 0f) current else 0.5f
                                }
                            },
                            onLevelChange = { rightSide, delta ->
                                if (rightSide) {
                                    volumeLevel = (volumeLevel + delta).coerceIn(0f, 1f)
                                    val max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
                                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, (volumeLevel * max).roundToInt(), 0)
                                    hud = Hud.Level(isBrightness = false, fraction = volumeLevel)
                                } else {
                                    brightnessLevel = (brightnessLevel + delta).coerceIn(0.01f, 1f)
                                    context.findActivity()?.window?.let {
                                        val params = it.attributes
                                        params.screenBrightness = brightnessLevel
                                        it.attributes = params
                                    }
                                    hud = Hud.Level(isBrightness = true, fraction = brightnessLevel)
                                }
                            },
                        )
                    },
                ),
        )

        PlayerHud(hud)

        PlayerControls(
            visible = controlsVisible && hud !is Hud.Seek,
            title = title,
            isPlaying = isPlaying,
            positionMs = scrub ?: position,
            durationMs = duration,
            onClose = onExit,
            onPlayPause = ::togglePlay,
            onSkip = ::skip,
            onScrub = { scrub = it },
            onScrubFinished = {
                scrub?.let {
                    engine.seekTo(it)
                    position = it
                }
                scrub = null
            },
            onAudio = {
                panel = Panel.Audio
            },
            onSubtitles = {
                panel = Panel.Subtitles
            },
        )

        when (panel) {
            Panel.Audio -> TrackPanel(
                title = stringResource(R.string.player_audio),
                options = buildTrackOptions(player, tracks, AudioTrackType, null) {
                    context.getString(R.string.player_track_n, it)
                },
                onDismiss = { panel = null },
            )
            Panel.Subtitles -> TrackPanel(
                title = stringResource(R.string.player_subtitles),
                options = buildTrackOptions(player, tracks, TextTrackType, stringResource(R.string.player_subtitles_off)) {
                    context.getString(R.string.player_track_n, it)
                },
                onDismiss = { panel = null },
            )
            null -> Unit
        }
    }
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
