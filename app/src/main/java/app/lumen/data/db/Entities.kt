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
