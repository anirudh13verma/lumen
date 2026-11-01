package app.lumen.data.meta

import app.lumen.data.parse.normalizeTitle
import kotlin.math.abs

enum class MediaKind(val key: String) {
    Movie("movie"),
    Tv("tv"),
}

/** One TMDB search hit, reduced to what Lumen stores. */
data class TmdbMatch(
    val kind: MediaKind,
    val id: Int,
    val title: String,
    val originalTitle: String?,
    val year: Int?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double?,
) {
    /** Stable primary key for the metadata table, e.g. "movie:603" or "tv:1396". */
    val mediaKey: String get() = "${kind.key}:$id"
}

/** Builds image URLs for the paths TMDB returns (always start with "/"). */
object TmdbImages {
    private const val Base = "https://image.tmdb.org/t/p/"

    fun poster(path: String?): String? = path?.let { "${Base}w342$it" }

    fun backdrop(path: String?): String? = path?.let { "${Base}w780$it" }

    /** Large backdrop for the full-width hero. */
    fun backdropLarge(path: String?): String? = path?.let { "${Base}w1280$it" }

    fun logo(path: String?): String? = path?.takeIf { it.isNotEmpty() }?.let { "${Base}w500$it" }
}

/**
 * Chooses which TMDB search result, if any, is really the file's film or show.
 *
 * Wrong posters are worse than missing ones, so this is conservative:
 * 1. A result whose title (or original title) equals the query wins; if the query has a year,
 *    a result within one year of it is preferred.
 * 2. Otherwise only TMDB's top result is considered, and only if its title contains the query (or
 *    the other way round) and the years agree. With no year to check, the two titles must also be
 *    close in length, so "Dune" cannot quietly become "Dune: Part Two".
 * 3. Anything else is "no match".
 */
fun pickBest(candidates: List<TmdbMatch>, queryTitle: String, queryYear: Int?): TmdbMatch? {
    val query = normalizeTitle(queryTitle)
    if (query.isEmpty() || candidates.isEmpty()) return null

    fun yearAgrees(candidate: TmdbMatch): Boolean {
        val candidateYear = candidate.year ?: return false
        return queryYear != null && abs(candidateYear - queryYear) <= 1
    }

    val exact = candidates.filter {
        normalizeTitle(it.title) == query || (it.originalTitle != null && normalizeTitle(it.originalTitle) == query)
    }
    if (exact.isNotEmpty()) {
        return exact.firstOrNull { yearAgrees(it) } ?: exact.first()
    }

    val top = candidates.first()
    val topTitle = normalizeTitle(top.title)
    val related = topTitle.isNotEmpty() && (topTitle.contains(query) || query.contains(topTitle))
    if (!related) return null

    if (queryYear != null) return top.takeIf { yearAgrees(it) }

    val ratio = minOf(topTitle.length, query.length).toDouble() / maxOf(topTitle.length, query.length)
    return top.takeIf { ratio >= 0.6 }
}

/** One title logo offered by TMDB. [language] is an ISO 639-1 code, or null for language-neutral art. */
data class LogoCandidate(val path: String, val language: String?, val voteAverage: Double)

/**
 * Picks the logo to show: English first, then language-neutral, then anything else; the best-rated
 * within each group. SVG logos are skipped because the image loader here only decodes bitmaps.
 */
fun pickLogo(candidates: List<LogoCandidate>): String? {
    val usable = candidates.filter { !it.path.endsWith(".svg", ignoreCase = true) }
    fun rank(c: LogoCandidate) = when (c.language) {
        "en" -> 0
        null -> 1
        else -> 2
    }
    return usable.sortedWith(compareBy<LogoCandidate> { rank(it) }.thenByDescending { it.voteAverage })
        .firstOrNull()?.path
}
