package app.lumen.data.db

/**
 * One scanned file joined with whatever TMDB metadata it has been matched to.
 * The metadata columns are null until the file has been matched.
 */
data class MediaRow(
    val documentUri: String,
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val addedAt: Long,
    val mediaKey: String?,
    /** "movie" or "tv". */
    val kind: String?,
    val metaTitle: String?,
    val metaYear: Int?,
    val posterPath: String?,
)

/** A file that still needs a TMDB lookup. */
data class PendingFile(
    val documentUri: String,
    val name: String,
    val relativePath: String,
)
