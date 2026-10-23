package app.lumen.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
import androidx.room.withTransaction
import app.lumen.data.db.LibraryFolderEntity
import app.lumen.data.db.LumenDatabase
import app.lumen.data.db.MediaFileEntity
import app.lumen.data.model.LibraryFolder
import app.lumen.data.model.MediaFile
import app.lumen.data.model.toModel
import app.lumen.data.scan.FolderScanner
import app.lumen.data.scan.ScannedFile
import app.lumen.data.scan.isSameOrInside
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

enum class AddFolderResult {
    Added,

    /** The folder, or a parent of it, is already in the library. */
    AlreadyCovered,
}

data class SyncResult(
    /** False if the folder could not be read (revoked permission, deleted folder, ...). */
    val success: Boolean,
    val filesFound: Int,
)

class LibraryRepository(
    private val context: Context,
    private val database: LumenDatabase,
    private val scanner: FolderScanner,
) {
    private val folderDao get() = database.folderDao()
    private val mediaDao get() = database.mediaDao()

    val folders: Flow<List<LibraryFolder>> =
        folderDao.observeAll().map { list -> list.map { it.toModel() } }

    val files: Flow<List<MediaFile>> =
        mediaDao.observeAll().map { list -> list.map { it.toModel() } }

    suspend fun getFolders(): List<LibraryFolderEntity> = folderDao.getAll()

    suspend fun addFolder(treeUri: Uri): AddFolderResult = withContext(Dispatchers.IO) {
        val authority = treeUri.authority
        val newDocId = DocumentsContract.getTreeDocumentId(treeUri)
        val existing = folderDao.getAll()

        fun docIdOf(folder: LibraryFolderEntity) = DocumentsContract.getTreeDocumentId(Uri.parse(folder.treeUri))
        fun sameProvider(folder: LibraryFolderEntity) = Uri.parse(folder.treeUri).authority == authority

        if (existing.any { sameProvider(it) && isSameOrInside(newDocId, docIdOf(it)) }) {
            return@withContext AddFolderResult.AlreadyCovered
        }

        // Keep access across app restarts and reboots.
        context.contentResolver.takePersistableUriPermission(treeUri, Intent.FLAG_GRANT_READ_URI_PERMISSION)

        folderDao.insert(
            LibraryFolderEntity(
                treeUri = treeUri.toString(),
                displayName = displayNameOf(treeUri),
                addedAt = System.currentTimeMillis(),
            ),
        )

        // A new parent folder swallows any existing subfolders so files are never listed twice.
        existing
            .filter { sameProvider(it) && isSameOrInside(docIdOf(it), newDocId) }
            .forEach { removeFolder(it.treeUri) }

        AddFolderResult.Added
    }

    suspend fun removeFolder(treeUri: String) = withContext(Dispatchers.IO) {
        try {
            context.contentResolver.releasePersistableUriPermission(
                Uri.parse(treeUri),
                Intent.FLAG_GRANT_READ_URI_PERMISSION,
            )
        } catch (e: SecurityException) {
            // Permission was already gone; nothing to release.
        }
        folderDao.delete(treeUri) // cascades to the folder's files
    }

    /**
     * Scans [folder] and updates the database to match what is on disk: new files are added,
     * changed files updated and deleted files removed. Unchanged files are left untouched.
     *
     * @return how many videos were found, and whether the folder could be read at all.
     */
    suspend fun syncFolder(
        folder: LibraryFolderEntity,
        onProgress: suspend (filesFound: Int) -> Unit = {},
    ): SyncResult {
        val scanStartedAt = System.currentTimeMillis()
        return try {
            val scanned = scanner.scan(Uri.parse(folder.treeUri), onProgress)
            val existing = mediaDao.stamps(folder.treeUri).associateBy { it.documentUri }

            val seen = HashSet<String>(scanned.size)
            val toUpsert = ArrayList<MediaFileEntity>()
            for (file in scanned) {
                val key = file.documentUri.toString()
                seen += key
                val old = existing[key]
                val unchanged = old != null &&
                    old.sizeBytes == file.sizeBytes &&
                    old.lastModified == file.lastModified
                if (!unchanged) {
                    toUpsert += file.toEntity(folder.treeUri, addedAt = old?.addedAt ?: scanStartedAt)
                }
            }
            val removed = existing.keys.filterNot { it in seen }

            database.withTransaction {
                toUpsert.chunked(SqlBatchSize).forEach { mediaDao.upsertAll(it) }
                removed.chunked(SqlBatchSize).forEach { mediaDao.deleteByUris(it) }
                folderDao.updateScanResult(folder.treeUri, scanStartedAt, failed = false)
            }
            SyncResult(success = true, filesFound = scanned.size)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            // Typically a revoked permission or a folder that no longer exists.
            folderDao.updateScanResult(folder.treeUri, scanStartedAt, failed = true)
            SyncResult(success = false, filesFound = 0)
        }
    }

    private fun displayNameOf(treeUri: Uri): String {
        val rootDocument = DocumentsContract.buildDocumentUriUsingTree(
            treeUri,
            DocumentsContract.getTreeDocumentId(treeUri),
        )
        val name = runCatching {
            context.contentResolver.query(
                rootDocument,
                arrayOf(DocumentsContract.Document.COLUMN_DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor -> if (cursor.moveToFirst()) cursor.getString(0) else null }
        }.getOrNull()
        return name ?: treeUri.lastPathSegment ?: "Folder"
    }

    private fun ScannedFile.toEntity(folderUri: String, addedAt: Long) = MediaFileEntity(
        documentUri = documentUri.toString(),
        folderUri = folderUri,
        name = name,
        relativePath = relativePath,
        sizeBytes = sizeBytes,
        lastModified = lastModified,
        mimeType = mimeType,
        addedAt = addedAt,
    )

    private companion object {
        /** SQLite caps the number of bound variables per statement, so large lists are chunked. */
        const val SqlBatchSize = 500
    }
}
