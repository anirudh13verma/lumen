package app.lumen.ui.player

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.pm.ActivityInfo
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LifecycleEventEffect
import androidx.media3.ui.PlayerView
import app.lumen.LumenApplication
import app.lumen.player.Media3Engine
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private const val SaveIntervalMs = 5_000L

/**
 * Fullscreen video. Landscape, system bars hidden, screen kept on. The position is saved every few
 * seconds, when the app goes to the background and when the screen closes, so "Continue watching"
 * and Resume stay accurate.
 */
@Composable
fun PlayerScreen(
    documentUri: String,
    startMs: Long,
    onExit: () -> Unit,
) {
    val context = LocalContext.current
    val view = LocalView.current
    val container = (context.applicationContext as LumenApplication).container

    val engine = remember(documentUri) { Media3Engine(context).also { it.load(documentUri, startMs) } }

    fun saveNow() {
        val position = engine.positionMs
        val duration = engine.durationMs
        container.applicationScope.launch { container.playbackRepository.save(documentUri, position, duration) }
    }

    DisposableEffect(engine) {
        engine.play()
        onDispose {
            saveNow()
            engine.release()
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
            activity?.requestedOrientation = previousOrientation ?: ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED
        }
    }

    BackHandler(onBack = onExit)

    AndroidView(
        modifier = Modifier.fillMaxSize(),
        factory = { ctx ->
            PlayerView(ctx).apply {
                player = engine.player
                setShowSubtitleButton(true)
                controllerShowTimeoutMs = 3_000
            }
        },
        onRelease = { it.player = null },
    )
}

private fun Context.findActivity(): Activity? {
    var current: Context? = this
    while (current is ContextWrapper) {
        if (current is Activity) return current
        current = current.baseContext
    }
    return null
}
