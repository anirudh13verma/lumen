package app.lumen.ui.player

import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.pointerInput
import kotlin.math.abs

enum class TapRegion { Left, Center, Right }

/** Swiping the full width of the screen moves this far through the video. */
private const val SeekFullWidthMs = 120_000L

/** Movement (in pixels) before a drag is classed as horizontal or vertical. */
private const val AxisLockPx = 24f

/**
 * Touch controls for the video surface:
 * - tap: show or hide the controls
 * - double tap: left third back, right third forward, middle play/pause
 * - drag sideways: seek, with a live preview that only commits when the finger lifts
 * - drag up/down: volume on the right half, brightness on the left half
 */
fun Modifier.playerGestures(
    onTap: () -> Unit,
    onDoubleTap: (TapRegion) -> Unit,
    /** Returns (position, duration) in ms when a seek-drag starts. */
    seekStart: () -> Pair<Long, Long>,
    onSeekPreview: (deltaMs: Long, targetMs: Long, durationMs: Long) -> Unit,
    onSeekCommit: (targetMs: Long) -> Unit,
    onSeekCancel: () -> Unit,
    onLevelStart: (rightSide: Boolean) -> Unit,
    onLevelChange: (rightSide: Boolean, delta: Float) -> Unit,
): Modifier = this
    .pointerInput(Unit) {
        detectTapGestures(
            onTap = { onTap() },
            onDoubleTap = { offset ->
                onDoubleTap(
                    when {
                        offset.x < size.width / 3f -> TapRegion.Left
                        offset.x > size.width * 2f / 3f -> TapRegion.Right
                        else -> TapRegion.Center
                    },
                )
            },
        )
    }
    .pointerInput(Unit) {
        var axis = 0 // 0 = undecided, 1 = horizontal (seek), 2 = vertical (volume / brightness)
        var totalX = 0f
        var totalY = 0f
        var rightSide = false
        var startPosition = 0L
        var duration = 0L
        var target = 0L

        detectDragGestures(
            onDragStart = { offset ->
                axis = 0
                totalX = 0f
                totalY = 0f
                rightSide = offset.x > size.width / 2f
            },
            onDrag = { change, dragAmount ->
                change.consume()
                totalX += dragAmount.x
                totalY += dragAmount.y

                if (axis == 0 && (abs(totalX) > AxisLockPx || abs(totalY) > AxisLockPx)) {
                    if (abs(totalX) > abs(totalY)) {
                        axis = 1
                        val (position, length) = seekStart()
                        startPosition = position
                        duration = length
                        target = position
                    } else {
                        axis = 2
                        onLevelStart(rightSide)
                    }
                }

                when (axis) {
                    1 -> if (duration > 0) {
                        val delta = (totalX / size.width * SeekFullWidthMs).toLong()
                        target = (startPosition + delta).coerceIn(0L, duration)
                        onSeekPreview(target - startPosition, target, duration)
                    }
                    2 -> onLevelChange(rightSide, -dragAmount.y / size.height * 1.2f) // swipe up = more
                }
            },
            onDragEnd = { if (axis == 1 && duration > 0) onSeekCommit(target) },
            onDragCancel = { if (axis == 1) onSeekCancel() },
        )
    }
