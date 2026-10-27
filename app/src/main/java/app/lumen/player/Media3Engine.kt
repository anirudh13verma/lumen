package app.lumen.player

import android.content.Context
import androidx.media3.common.AudioAttributes
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.exoplayer.ExoPlayer

/** [PlaybackEngine] backed by Media3 ExoPlayer. [player] is exposed so the view can render it. */
class Media3Engine(context: Context) : PlaybackEngine {

    val player: ExoPlayer = ExoPlayer.Builder(context)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE)
                .build(),
            /* handleAudioFocus = */ true,
        )
        .setHandleAudioBecomingNoisy(true) // pause when headphones are unplugged
        .build()

    override fun load(uri: String, startMs: Long) {
        player.setMediaItem(MediaItem.fromUri(uri), startMs)
        player.prepare()
    }

    override fun play() = player.play()

    override fun pause() = player.pause()

    override val isPlaying: Boolean get() = player.isPlaying

    override val positionMs: Long get() = player.currentPosition

    override val durationMs: Long get() = player.duration.takeIf { it != C.TIME_UNSET } ?: 0L

    override fun release() = player.release()
}
