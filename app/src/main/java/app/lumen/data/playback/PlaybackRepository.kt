package app.lumen.data.playback

import android.database.sqlite.SQLiteConstraintException
import app.lumen.data.db.LumenDatabase
import app.lumen.data.db.ProgressEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PlaybackRepository(private val database: LumenDatabase) {
    private val dao get() = database.progressDao()

    /** Progress for every file the user has started, keyed by document URI. */
    val progress: Flow<Map<String, Progress>> = dao.observeAll().map { list ->
        list.associate {
            it.documentUri to Progress(it.documentUri, it.positionMs, it.durationMs, it.completed, it.updatedAt)
        }
    }

    /** Stores the current position. Ignored until the duration is known. */
    suspend fun save(uri: String, positionMs: Long, durationMs: Long) {
        if (durationMs <= 0 || positionMs < 0) return
        try {
            dao.upsert(
                ProgressEntity(
                    documentUri = uri,
                    positionMs = positionMs.coerceAtMost(durationMs),
                    durationMs = durationMs,
                    completed = ResumeRules.isCompleted(positionMs, durationMs),
                    updatedAt = System.currentTimeMillis(),
                ),
            )
        } catch (e: SQLiteConstraintException) {
            // The file was removed from the library while playing; nothing to remember.
        }
    }
}
