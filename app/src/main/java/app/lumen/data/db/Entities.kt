package app.lumen.data.db

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

/** A folder the user picked. [treeUri] is the Storage Access Framework tree URI. */
@Entity(tableName = "library_folders")
data class LibraryFolderEntity(
    @PrimaryKey val treeUri: String,
    val displayName: String,
    val addedAt: Long,
    val lastScanAt: Long? = null,
    val lastScanFailed: Boolean = false,
)

/** A video file found inside a library folder. Deleting the folder deletes its files. */
@Entity(
    tableName = "media_files",
    foreignKeys = [
        ForeignKey(
            entity = LibraryFolderEntity::class,
            parentColumns = ["treeUri"],
            childColumns = ["folderUri"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("folderUri")],
)
data class MediaFileEntity(
    @PrimaryKey val documentUri: String,
    val folderUri: String,
    val name: String,
    /** Directory path inside the library folder, e.g. "Shows/Severance/Season 2". Empty at the root. */
    val relativePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
    val addedAt: Long,
)

/** Lightweight projection used to detect new, changed and removed files during a rescan. */
data class FileStamp(
    val documentUri: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val addedAt: Long,
)

/** TMDB details for one film or show, shared by every file that matched it. */
@Entity(tableName = "media_metadata")
data class MetadataEntity(
    /** "movie:603" or "tv:1396". */
    @PrimaryKey val mediaKey: String,
    /** "movie" or "tv". */
    val kind: String,
    val tmdbId: Int,
    val title: String,
    val year: Int?,
    val overview: String?,
    val posterPath: String?,
    val backdropPath: String?,
    val rating: Double?,
    val updatedAt: Long,
)

/**
 * The outcome of trying to match one file. Kept apart from [MediaFileEntity] so that rescans,
 * which upsert files, can never wipe a match.
 *
 * A row with [status] [MatchStatus.NotFound] remembers a failed lookup so it is not repeated on
 * every launch; it is retried after a week.
 */
@Entity(
    tableName = "file_matches",
    foreignKeys = [
        ForeignKey(
            entity = MediaFileEntity::class,
            parentColumns = ["documentUri"],
            childColumns = ["documentUri"],
            onDelete = ForeignKey.CASCADE,
        ),
        ForeignKey(
            entity = MetadataEntity::class,
            parentColumns = ["mediaKey"],
            childColumns = ["mediaKey"],
            onDelete = ForeignKey.SET_NULL,
        ),
    ],
    indices = [Index("mediaKey")],
)
data class FileMatchEntity(
    @PrimaryKey val documentUri: String,
    val mediaKey: String?,
    val status: Int,
    val attemptedAt: Long,
)

object MatchStatus {
    const val Matched = 1
    const val NotFound = 2
}
