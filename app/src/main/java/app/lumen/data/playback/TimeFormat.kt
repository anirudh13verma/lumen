package app.lumen.data.playback

import java.util.Locale
import kotlin.math.abs

/** "1:05" or "1:02:03", for the seek bar and gesture overlay. */
fun formatClock(ms: Long): String {
    val totalSeconds = ms.coerceAtLeast(0) / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) {
        String.format(Locale.ROOT, "%d:%02d:%02d", hours, minutes, seconds)
    } else {
        String.format(Locale.ROOT, "%d:%02d", minutes, seconds)
    }
}

/** "+0:23" or "\u22120:15", the distance of a swipe-seek. */
fun formatSeekDelta(deltaMs: Long): String =
    (if (deltaMs < 0) "\u2212" else "+") + formatClock(abs(deltaMs))
