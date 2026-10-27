package app.lumen.data.model

import app.lumen.data.db.MediaRow
import app.lumen.data.parse.ParsedName
import app.lumen.data.parse.normalizeTitle
import app.lumen.data.parse.parseFileName

enum class ItemKind { Movie, Show }

/**
 * One card in the library: a movie, or a whole show (all its episodes together).
 * [posterPath] is a TMDB path, or null while the item is unmatched.
 */
data class LibraryItem(
    val id: String,
    val kind: ItemKind,
    val title: String,
    val year: Int?,
    val posterPath: String?,
    /** Files behind this card. For a show, the number of episode files. */
    val fileCount: Int,
    val totalSizeBytes: Long,
    val addedAt: Long,
    val matched: Boolean,
    val backdropPath: String? = null,
    val overview: String? = null,
    /** TMDB average vote, 0..10. */
    val rating: Double? = null,
    /** The video files behind this card, episodes in season/episode order. */
    val files: List<ItemFile> = emptyList(),
)

/** One video file of a [LibraryItem]. Season and episode are null for movies. */
data class ItemFile(
    val uri: String,
    val name: String,
    val sizeBytes: Long,
    val season: Int?,
    val episode: Int?,
    val episodeEnd: Int?,
)

private class ParsedRow(val row: MediaRow, val parsed: ParsedName)

/**
 * Turns scanned files into library cards.
 *
 * - Files matched to the same TMDB entry share one card (all episodes of a show; two
 *   versions of one movie).
 * - Episodes not yet matched are grouped by their parsed show title, so twenty unmatched episodes
 *   show up as one card instead of twenty. If another episode of that show *is* matched, they join
 *   its card straight away, without waiting for their own lookup.
 * - Any other unmatched file is a card of its own.
 */
fun buildLibraryItems(rows: List<MediaRow>): List<LibraryItem> {
    val parsedRows = rows.map { ParsedRow(it, parseFileName(it.name, it.relativePath)) }

    val matchedShowKeyByTitle = HashMap<String, String>()
    for (item in parsedRows) {
        val key = item.row.mediaKey
        if (item.row.kind == "tv" && key != null) {
            matchedShowKeyByTitle.putIfAbsent(normalizeTitle(item.parsed.title), key)
        }
    }

    val groups = LinkedHashMap<String, MutableList<ParsedRow>>()
    for (item in parsedRows) {
        val matchedKey = item.row.mediaKey
        val showTitle = normalizeTitle(item.parsed.title)
        val groupKey = when {
            matchedKey != null -> matchedKey
            item.parsed.isEpisode -> matchedShowKeyByTitle[showTitle] ?: "show:$showTitle"
            else -> "file:${item.row.documentUri}"
        }
        groups.getOrPut(groupKey) { ArrayList() }.add(item)
    }

    return groups.map { (key, members) -> members.toItem(key) }
}

private fun List<ParsedRow>.toItem(key: String): LibraryItem {
    val matched = firstOrNull { it.row.mediaKey != null }?.row
    val first = first().parsed

    val kind = when (matched?.kind) {
        "tv" -> ItemKind.Show
        "movie" -> ItemKind.Movie
        else -> if (any { it.parsed.isEpisode }) ItemKind.Show else ItemKind.Movie
    }

    return LibraryItem(
        id = key,
        kind = kind,
        title = matched?.metaTitle ?: first.title,
        year = matched?.metaYear ?: firstNotNullOfOrNull { it.parsed.year },
        posterPath = matched?.posterPath,
        fileCount = size,
        totalSizeBytes = sumOf { it.row.sizeBytes },
        addedAt = maxOf { it.row.addedAt },
        matched = matched != null,
        backdropPath = matched?.backdropPath,
        overview = matched?.overview,
        rating = matched?.rating,
        files = sortedWith(compareBy({ it.parsed.season ?: 0 }, { it.parsed.episode ?: 0 }, { it.row.name }))
            .map {
                ItemFile(
                    uri = it.row.documentUri,
                    name = it.row.name,
                    sizeBytes = it.row.sizeBytes,
                    season = it.parsed.season,
                    episode = it.parsed.episode,
                    episodeEnd = it.parsed.episodeEnd,
                )
            },
    )
}

enum class LibraryFilter { All, Movies, Shows }

/** Applies the Library screen's type chip and search text. Search ignores case and punctuation. */
fun filterItems(items: List<LibraryItem>, filter: LibraryFilter, query: String): List<LibraryItem> {
    val needle = normalizeTitle(query)
    return items.filter { item ->
        val kindOk = when (filter) {
            LibraryFilter.All -> true
            LibraryFilter.Movies -> item.kind == ItemKind.Movie
            LibraryFilter.Shows -> item.kind == ItemKind.Show
        }
        kindOk && (needle.isEmpty() || normalizeTitle(item.title).contains(needle))
    }
}

/** A season's episodes. [season] 0 collects files whose season is unknown. */
data class SeasonGroup(val season: Int, val episodes: List<ItemFile>)

fun groupBySeason(files: List<ItemFile>): List<SeasonGroup> =
    files.groupBy { it.season ?: 0 }
        .toSortedMap()
        .map { (season, list) -> SeasonGroup(season, list.sortedWith(compareBy({ it.episode ?: Int.MAX_VALUE }, { it.name }))) }
