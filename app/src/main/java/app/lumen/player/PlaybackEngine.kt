package app.lumen.player

/**
 * The small surface the rest of the app needs from a video player. Media3 implements it today;
 * a libmpv engine can be added later without touching the screens, which only use this interface
 * (plus a per-engine video surface in PlayerScreen).
 *
 * All members must be called on the main thread.
 */
interface PlaybackEngine {
    /** Opens [uri] (a SAF content URI) and starts preparing at [startMs]. Does not start playing. */
    fun load(uri: String, startMs: Long)

    fun play()
    fun pause()

    val isPlaying: Boolean

    /** Current position in milliseconds. */
    val positionMs: Long

    /** Length in milliseconds, or 0 while unknown. */
    val durationMs: Long

    fun release()
}
