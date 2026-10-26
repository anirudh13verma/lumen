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
    )
}
