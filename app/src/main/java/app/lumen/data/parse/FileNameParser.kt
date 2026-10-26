package app.lumen.data.parse

import app.lumen.data.scan.stripVideoExtension

/**
 * What a video's file name (and folder) says about it.
 *
 * [isEpisode] is true when an episode number was found. Season and episode numbers are 1-based.
 */
data class ParsedName(
    val title: String,
    val year: Int?,
    val season: Int?,
    val episode: Int?,
    /** Last episode of a multi-episode file such as "S01E02E03" (or "S01E02-03"). */
    val episodeEnd: Int? = null,
) {
    val isEpisode: Boolean get() = episode != null
}

/** Lower-cases a title and collapses everything that is not a letter or digit, for comparing titles. */
fun normalizeTitle(title: String): String =
    title.lowercase()
        .replace("'", "") // "Marvel's" and "Marvels" must compare equal; file names rarely keep apostrophes
        .replace("’", "")
        .replace(NonAlphanumeric, " ")
        .trim()

/**
 * Splits a file name such as `Dune.Part.Two.2024.1080p.WEB-DL.mkv` or `Severance.S02E03.mkv`
 * into title, year and episode numbers.
 *
 * [relativePath] is the folder the file lives in, relative to the library folder
 * (for example `Shows/Severance/Season 2`). It is only consulted when the file name does
 * not carry the title ("S02E03.mkv", "movie.mkv") or the season.
 *
 * This is deliberately heuristic. When it cannot tell, it prefers returning a plain title
 * over guessing, because TMDB matching decides the final answer.
 */
fun parseFileName(fileName: String, relativePath: String = ""): ParsedName {
    val hadGroupTag = GroupTag.containsMatchIn(fileName)
    val stem = stripVideoExtension(fileName).replace(GroupTag, "")
    val context = contextFromPath(relativePath)

    val marker = findEpisodeMarker(stem, hadGroupTag, context.title)
    val rawTitle = if (marker != null) stem.substring(0, marker.start) else stem

    val (parsedTitle, parsedYear) = extractTitleAndYear(rawTitle)
    var title = parsedTitle
    var year = parsedYear

    if (title.isEmpty() || title.lowercase() in GenericNames) {
        // The file name carries no usable title, so lean on the folder ("Severance (2022)/Season 2/").
        context.title?.let {
            title = it
            year = year ?: context.year
        }
    }
    if (title.isEmpty()) title = cleanText(stem).ifEmpty { fileName }

    return ParsedName(
        title = title,
        year = year,
        season = marker?.let { it.season ?: context.season ?: 1 },
        episode = marker?.episode,
        episodeEnd = marker?.episodeEnd,
    )
}

// ---------------------------------------------------------------------------------------------
// Episode markers
// ---------------------------------------------------------------------------------------------

private class Marker(val start: Int, val season: Int?, val episode: Int, val episodeEnd: Int?)

/** S01E02, S01E02E03, S01E02-E03, S01E02-03 */
private val SeasonEpisode =
    Regex("""(?i)(?<![a-z0-9])s(\d{1,2})[ ._-]?e(\d{1,3})(?:[ ._-]?(?:e|-e?)(\d{1,3}))?(?![0-9])""")

/** 1x02 (but never 1920x1080) */
private val NxMM = Regex("""(?<![0-9a-zA-Z])(\d{1,2})x(\d{2,3})(?![0-9a-zA-Z])""")

/** "Season 2 Episode 3" */
private val LongForm =
    Regex("""(?i)(?<![a-z0-9])season[ ._-]*(\d{1,2})[ ._,-]*(?:episode|ep)[ ._-]*(\d{1,3})(?![0-9])""")

/** Fansub style: "Title - 12 [1080p]". Only trusted when the name started with a [Group] tag. */
private val AnimeEpisode = Regex("""\s-\s(\d{2,3})(?:v\d)?(?=\s|\[|\(|$)""")

/** "E03", "Ep 3", "Episode 3" at the very start, relying on the folder for title and season. */
private val BareEpisode = Regex("""(?i)^(?:episode|ep|e)[ ._-]*(\d{1,3})(?![0-9])""")

private fun findEpisodeMarker(stem: String, hadGroupTag: Boolean, contextTitle: String?): Marker? {
    SeasonEpisode.find(stem)?.let { m ->
        return Marker(m.range.first, m.groupValues[1].toInt(), m.groupValues[2].toInt(), m.groupValues[3].toIntOrNull())
    }
    NxMM.find(stem)?.let { m ->
        return Marker(m.range.first, m.groupValues[1].toInt(), m.groupValues[2].toInt(), null)
    }
    LongForm.find(stem)?.let { m ->
        return Marker(m.range.first, m.groupValues[1].toInt(), m.groupValues[2].toInt(), null)
    }
    AnimeEpisode.find(stem)?.let { m ->
        // "Title - 12" is ambiguous ("Apollo - 13"), so it needs backing: a [Group] tag, or a
        // folder named after the same title ("Attack on Titan/Attack on Titan - 87.mkv").
        val before = stem.substring(0, m.range.first)
        val folderAgrees = contextTitle != null && normalizeTitle(before) == normalizeTitle(contextTitle)
        if (hadGroupTag || folderAgrees) {
            return Marker(m.range.first, null, m.groupValues[1].toInt(), null)
        }
    }
    BareEpisode.find(stem)?.let { m ->
        return Marker(m.range.first, null, m.groupValues[1].toInt(), null)
    }
    return null
}

// ---------------------------------------------------------------------------------------------
// Title and year
// ---------------------------------------------------------------------------------------------

private const val MinYear = 1900

/** Far enough ahead for announced films; keeps "Blade Runner 2049" from being read as a year. */
private const val MaxYear = 2035

/** Leading "[SubsPlease] " style release-group tag. Square brackets only, so "(500) Days" survives. */
private val GroupTag = Regex("""^\s*\[[^\]]{1,40}]\s*""")

/** Everything from the first of these onwards is release noise, not title. */
private val Junk = Regex(
    """(?i)(?<![a-z0-9])(?:480p|576p|720p|1080[pi]|2160p|4k|uhd|hdr10\+?|hdr|dolby[ ._-]?vision|""" +
        """web[ ._-]?dl|webrip|blu[ ._-]?ray|bdrip|brrip|bdremux|remux|hdtv|dvdrip|dvdscr|hdrip|""" +
        """x264|x265|h[ ._]?264|h[ ._]?265|hevc|avc|xvid|divx|aac|e?ac3|dd[p+]?[ ._]?[0-9]|""" +
        """dts(?:[ ._-]?hd)?|truehd|atmos|flac|10bit|proper|repack|extended|unrated|""" +
        """directors?[ ._]cut|imax|amzn|dsnp|hmax)(?![a-z0-9])""",
)

/** A year, optionally wrapped in (…) or […]. */
private val YearToken = Regex("""(?<![0-9a-zA-Z])[(\[]?((?:19|20)\d{2})[)\]]?(?![0-9a-zA-Z])""")

private val SeparatorRuns = Regex("""[._]+""")
private val Spaces = Regex("""\s+""")
private val NonAlphanumeric = Regex("""[^\p{L}\p{N}]+""")

private val GenericNames = setOf("movie", "film", "video", "sample", "untitled")

private fun cleanText(text: String): String =
    text.replace(SeparatorRuns, " ")
        .replace("()", " ")
        .replace("[]", " ")
        .replace(Spaces, " ")
        .trimStart(' ', '-', '–')
        .trimEnd(' ', '-', '–', ',', '(', '[')

/**
 * Cuts [raw] at the first piece of release noise, then picks the release year: the **last**
 * plausible year that still has some title in front of it. That is what makes "2012.2009" give the
 * film "2012" from 2009 and "Blade Runner 2049 2017" give "Blade Runner 2049" from 2017.
 */
private fun extractTitleAndYear(raw: String): Pair<String, Int?> {
    val cutAt = Junk.find(raw)?.range?.first ?: raw.length
    val region = raw.substring(0, cutAt)

    var year: Int? = null
    var titleEnd = region.length
    for (match in YearToken.findAll(region)) {
        val candidate = match.groupValues[1].toInt()
        if (candidate !in MinYear..MaxYear) continue
        if (cleanText(region.substring(0, match.range.first)).isEmpty()) continue
        year = candidate
        titleEnd = match.range.first
    }
    return cleanText(region.substring(0, titleEnd)) to year
}

// ---------------------------------------------------------------------------------------------
// Folder context
// ---------------------------------------------------------------------------------------------

private class PathContext(val title: String?, val year: Int?, val season: Int?)

private val SeasonFolder = Regex("""(?i)^(?:season|series|staffel|saison|s)[ ._-]*(\d{1,2})\b.*$""")

private val GenericFolders = setOf(
    "tv", "tv shows", "tvshows", "shows", "series", "tv series", "movies", "movie", "films", "film",
    "video", "videos", "downloads", "media", "anime", "specials", "extras", "sample", "samples",
)

private fun contextFromPath(relativePath: String): PathContext {
    val segments = relativePath.split('/').filter { it.isNotBlank() }

    // Nearest season folder, working from the file outwards.
    val season = segments.asReversed()
        .firstNotNullOfOrNull { SeasonFolder.matchEntire(it.trim())?.groupValues?.get(1)?.toIntOrNull() }

    // Nearest folder that is neither "Season 2" nor a generic bucket such as "Movies".
    val titleSegment = segments.asReversed().firstOrNull {
        SeasonFolder.matchEntire(it.trim()) == null && it.trim().lowercase() !in GenericFolders
    }
    val (title, year) = titleSegment?.let { extractTitleAndYear(it) } ?: ("" to null)
    return PathContext(title = title.ifEmpty { null }, year = year, season = season)
}
