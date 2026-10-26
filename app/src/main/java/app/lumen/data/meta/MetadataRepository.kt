package app.lumen.data.meta

import android.database.sqlite.SQLiteConstraintException
import androidx.room.withTransaction
import app.lumen.data.db.FileMatchEntity
import app.lumen.data.db.LumenDatabase
import app.lumen.data.db.MatchStatus
import app.lumen.data.db.MetadataEntity
import app.lumen.data.db.PendingFile
import app.lumen.data.parse.normalizeTitle
import app.lumen.data.parse.parseFileName
import kotlinx.coroutines.CancellationException
import java.io.IOException
import java.util.concurrent.TimeUnit

class MetadataRepository(
    private val database: LumenDatabase,
    private val tmdb: TmdbClient,
) {
    private val mediaDao get() = database.mediaDao()

    val isConfigured: Boolean get() = tmdb.isConfigured

    /**
     * Looks up every file that has no match yet (and retries old misses). Files of the same film
     * or show share one lookup. Network trouble leaves the affected files pending for next time.
     *
     * @param onProgress called with (files handled, files in total).
     */
    suspend fun matchPending(onProgress: suspend (done: Int, total: Int) -> Unit = { _, _ -> }) {
        if (!tmdb.isConfigured) return

        val retryBefore = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(RetryMissesAfterDays)
        val pending = mediaDao.filesNeedingMatch(retryBefore)
        if (pending.isEmpty()) {
            mediaDao.pruneMetadata()
            return
        }

        val groups = pending.groupBy { LookupKey.of(it) }
        var done = 0
        var consecutiveFailures = 0
        onProgress(0, pending.size)

        for ((key, files) in groups) {
            val outcome = try {
                lookup(key).also { consecutiveFailures = 0 }
            } catch (e: CancellationException) {
                throw e
            } catch (e: TmdbAuthException) {
                break // wrong key: nothing will work, and retrying would only hammer the API
            } catch (e: IOException) {
                if (++consecutiveFailures >= MaxConsecutiveFailures) break // offline or TMDB down
                done += files.size
                onProgress(done, pending.size)
                continue // leave these files pending
            }
            persist(outcome, files)
            done += files.size
            onProgress(done, pending.size)
        }
        mediaDao.pruneMetadata()
    }

    /** Forgets every match so the next scan looks everything up again. */
    suspend fun clearMatches() {
        database.withTransaction {
            mediaDao.clearMatches()
            mediaDao.pruneMetadata()
        }
    }

    private class LookupKey(val kind: MediaKind, val title: String, val year: Int?) {
        private val normalized = normalizeTitle(title)

        override fun equals(other: Any?) =
            other is LookupKey && kind == other.kind && normalized == other.normalized && year == other.year

        override fun hashCode() = (kind.hashCode() * 31 + normalized.hashCode()) * 31 + (year ?: 0)

        companion object {
            fun of(file: PendingFile): LookupKey {
                val parsed = parseFileName(file.name, file.relativePath)
                return LookupKey(
                    kind = if (parsed.isEpisode) MediaKind.Tv else MediaKind.Movie,
                    title = parsed.title,
                    year = parsed.year,
                )
            }
        }
    }

    /** Tries the strict search first, then looser ones; [pickBest] guards against wrong results. */
    private suspend fun lookup(key: LookupKey): TmdbMatch? {
        if (normalizeTitle(key.title).isEmpty()) return null
        val attempts = buildList {
            add(key.title to key.year) // title + year filter ("Dune" 2021)
            if (key.year != null) {
                add("${key.title} ${key.year}" to null) // "Wonder Woman 1984 2020" style titles
                add(key.title to null)
            }
        }
        for ((query, year) in attempts) {
            val match = pickBest(tmdb.search(key.kind, query, year), key.title, key.year)
            if (match != null) return match
        }
        return null
    }

    private suspend fun persist(match: TmdbMatch?, files: List<PendingFile>) {
        val now = System.currentTimeMillis()
        try {
            database.withTransaction {
                if (match != null) {
                    mediaDao.upsertMetadata(
                        MetadataEntity(
                            mediaKey = match.mediaKey,
                            kind = match.kind.key,
                            tmdbId = match.id,
                            title = match.title,
                            year = match.year,
                            overview = match.overview,
                            posterPath = match.posterPath,
                            backdropPath = match.backdropPath,
                            rating = match.rating,
                            updatedAt = now,
                        ),
                    )
                }
                mediaDao.upsertMatches(
                    files.map {
                        FileMatchEntity(
                            documentUri = it.documentUri,
                            mediaKey = match?.mediaKey,
                            status = if (match != null) MatchStatus.Matched else MatchStatus.NotFound,
                            attemptedAt = now,
                        )
                    },
                )
            }
        } catch (e: SQLiteConstraintException) {
            // A file was removed by a rescan while we were looking it up; it will be retried.
        }
    }

    private companion object {
        const val RetryMissesAfterDays = 7L
        const val MaxConsecutiveFailures = 3
    }
}
