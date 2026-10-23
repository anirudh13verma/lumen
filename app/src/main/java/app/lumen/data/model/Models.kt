package app.lumen.data.model

import app.lumen.data.db.LibraryFolderEntity
import app.lumen.data.db.MediaFileEntity
import app.lumen.data.scan.cleanTitle

data class LibraryFolder(
    val treeUri: String,
    val displayName: String,
    val lastScanAt: Long?,
    val lastScanFailed: Boolean,
)

data class MediaFile(
    val uri: String,
    val name: String,
    /** Cleaned-up display title. Replaced by the TMDB title once metadata matching exists. */
    val title: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val relativePath: String,
)

internal fun LibraryFolderEntity.toModel() = LibraryFolder(
    treeUri = treeUri,
    displayName = displayName,
    lastScanAt = lastScanAt,
    lastScanFailed = lastScanFailed,
)

internal fun MediaFileEntity.toModel() = MediaFile(
    uri = documentUri,
    name = name,
    title = cleanTitle(name),
    sizeBytes = sizeBytes,
    lastModified = lastModified,
    relativePath = relativePath,
)
