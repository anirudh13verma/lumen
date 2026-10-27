package app.lumen.data.playback

import app.lumen.data.model.ItemFile
import app.lumen.data.model.LibraryItem

/** How far into one file the user got. */
data class Progress(
    val uri: String,
    val positionMs: Long,
    val durationMs: Long,
    /** True once the file was watched to (nearly) the end. */
    val completed: Boolean,
    val updatedAt: Long,
) {
    /** 0..1, for progress bars. */
    val fraction: Float
        get() = if (durationMs > 0) (positionMs.toFloat() / durationMs).coerceIn(0f, 1f) else 0f
}

object ResumeRules {
    /** Below this the user barely started, so "resume" would just be noise. */
    const val MinResumeMs = 10_000L

    /** Past this fraction the film or episode counts as watched (skips the credits). */
    const val CompletedFraction = 0.92

    fun isCompleted(positionMs: Long, durationMs: Long): Boolean =
        durationMs > 0 && positionMs >= durationMs * CompletedFraction
}

fun Progress.isResumable(): Boolean =
    !completed && durationMs > 0 && positionMs >= ResumeRules.MinResumeMs &&
        !ResumeRules.isCompleted(positionMs, durationMs)

/** What pressing Play on a title should start: which file, and where in it. */
data class PlayTarget(val file: ItemFile, val startMs: Long) {
    val isResume: Boolean get() = startMs > 0
}

/**
 * Decides what Play does for a movie or show.
 *
 * 1. Something half-watched: continue the most recently watched such file.
 * 2. Otherwise, if the most recently watched file was finished: the file after it
 *    (or the first file again once the last one is done).
 * 3. Otherwise: the first file.
 */
fun pickPlayTarget(files: List<ItemFile>, progress: Map<String, Progress>): PlayTarget? {
    if (files.isEmpty()) return null

    val resumable = files.mapNotNull { progress[it.uri] }.filter { it.isResumable() }.maxByOrNull { it.updatedAt }
    if (resumable != null) {
        files.firstOrNull { it.uri == resumable.uri }?.let { return PlayTarget(it, resumable.positionMs) }
    }

    val lastFinished = files.mapNotNull { progress[it.uri] }.filter { it.completed }.maxByOrNull { it.updatedAt }
    if (lastFinished != null) {
        val index = files.indexOfFirst { it.uri == lastFinished.uri }
        files.getOrNull(index + 1)?.let { return PlayTarget(it, 0) }
    }
    return PlayTarget(files.first(), 0)
}

/** Where a specific file should start: its saved position if resumable, otherwise the beginning. */
fun startPositionFor(file: ItemFile, progress: Map<String, Progress>): Long =
    progress[file.uri]?.takeIf { it.isResumable() }?.positionMs ?: 0L

/** One "Continue watching" card: the title, the file in progress, and how far along it is. */
data class ContinueEntry(val item: LibraryItem, val file: ItemFile, val progress: Progress)

/** Half-watched files, most recent first, one card per title (a show shows only its latest episode). */
fun buildContinueWatching(items: List<LibraryItem>, progress: Map<String, Progress>, limit: Int = 20): List<ContinueEntry> {
    val itemByFile = HashMap<String, Pair<LibraryItem, ItemFile>>()
    for (item in items) for (file in item.files) itemByFile[file.uri] = item to file

    val seen = HashSet<String>()
    return progress.values
        .filter { it.isResumable() }
        .sortedByDescending { it.updatedAt }
        .mapNotNull { p ->
            val (item, file) = itemByFile[p.uri] ?: return@mapNotNull null
            if (seen.add(item.id)) ContinueEntry(item, file, p) else null
        }
        .take(limit)
}
