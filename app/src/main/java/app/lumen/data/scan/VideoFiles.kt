package app.lumen.data.scan

/**
 * Extensions treated as video when the provider reports a generic MIME type
 * (MKV in particular often arrives as application/octet-stream).
 * "ts" is deliberately absent: it is far more often TypeScript than MPEG-TS, and real
 * MPEG-TS files are caught by their video/mp2t MIME type.
 */
private val VideoExtensions = setOf(
    "mkv", "mp4", "m4v", "avi", "mov", "wmv", "flv", "webm",
    "mpg", "mpeg", "m2ts", "mts", "3gp", "ogv", "divx", "rmvb",
    "vob", "asf", "f4v", "ogm",
)

private fun extensionOf(name: String): String = name.substringAfterLast('.', "").lowercase()

/** Removes a *known* video extension and leaves other dots alone ("Movie.Name.2024" is unchanged). */
fun stripVideoExtension(fileName: String): String =
    if (extensionOf(fileName) in VideoExtensions) fileName.substringBeforeLast('.') else fileName

fun isVideoFile(name: String, mimeType: String?): Boolean {
    if (mimeType != null && mimeType.startsWith("video/")) return true
    return extensionOf(name) in VideoExtensions
}

private val Whitespace = Regex("\\s+")

/**
 * Placeholder display title: strips the extension and turns dots/underscores into spaces.
 * The real filename parser (title, year, season, episode) arrives with TMDB matching.
 */
fun cleanTitle(fileName: String): String {
    return stripVideoExtension(fileName)
        .replace('.', ' ')
        .replace('_', ' ')
        .replace(Whitespace, " ")
        .trim()
        .ifEmpty { fileName }
}

/**
 * Whether [childDocId] is [parentDocId] or lives inside it, for SAF document IDs such as
 * "primary:Movies" and "primary:Movies/Action". Used to avoid scanning the same files twice
 * when a user adds a folder and one of its parents.
 */
fun isSameOrInside(childDocId: String, parentDocId: String): Boolean {
    if (childDocId == parentDocId) return true
    val prefix = if (parentDocId.endsWith(":") || parentDocId.endsWith("/")) parentDocId else "$parentDocId/"
    return childDocId.startsWith(prefix)
}
