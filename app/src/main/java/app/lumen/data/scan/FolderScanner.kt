package app.lumen.data.scan

import android.content.ContentResolver
import android.net.Uri
import android.provider.DocumentsContract
import android.provider.DocumentsContract.Document
import java.io.IOException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.withContext

data class ScannedFile(
    val documentUri: Uri,
    val name: String,
    val relativePath: String,
    val sizeBytes: Long,
    val lastModified: Long,
    val mimeType: String?,
)

/**
 * Walks a Storage Access Framework tree and returns every video file in it.
 *
 * Uses [DocumentsContract] cursor queries directly. `DocumentFile` issues a separate
 * provider query for each property of each file, which is painfully slow on big libraries.
 */
class FolderScanner(private val resolver: ContentResolver) {

    private class Directory(val documentId: String, val path: String)

    /**
     * @param onProgress called every [PROGRESS_STEP] files with the running total.
     * @throws SecurityException / IOException if the root folder itself cannot be read
     *   (for example because the permission was revoked). Unreadable subfolders are skipped.
     */
    suspend fun scan(
        treeUri: Uri,
        onProgress: suspend (filesFound: Int) -> Unit = {},
    ): List<ScannedFile> = withContext(Dispatchers.IO) {
        val results = ArrayList<ScannedFile>()
        val pending = ArrayDeque<Directory>()
        pending.add(Directory(DocumentsContract.getTreeDocumentId(treeUri), path = ""))

        while (pending.isNotEmpty()) {
            val directory = pending.removeLast()
            val isRoot = directory.path.isEmpty()
            try {
                listDirectory(treeUri, directory, pending, results, onProgress)
            } catch (e: SecurityException) {
                if (isRoot) throw e
            } catch (e: IllegalArgumentException) {
                if (isRoot) throw e
            } catch (e: IOException) {
                // listDirectory throws this when the provider returns no cursor. Without this
                // branch one bad subfolder would fail the whole scan instead of being skipped.
                if (isRoot) throw e
            }
        }
        results
    }

    private suspend fun listDirectory(
        treeUri: Uri,
        directory: Directory,
        pending: ArrayDeque<Directory>,
        results: MutableList<ScannedFile>,
        onProgress: suspend (Int) -> Unit,
    ) {
        val childrenUri = DocumentsContract.buildChildDocumentsUriUsingTree(treeUri, directory.documentId)
        val cursor = resolver.query(childrenUri, Projection, null, null, null)
            ?: throw IOException("Provider returned no cursor for $childrenUri")

        cursor.use {
            while (it.moveToNext()) {
                currentCoroutineContext().ensureActive()

                val documentId = it.getString(0) ?: continue
                val name = it.getString(1) ?: continue
                if (name.startsWith(".")) continue // hidden files and folders such as .thumbnails

                val mimeType = it.getString(2)
                if (mimeType == Document.MIME_TYPE_DIR) {
                    val path = if (directory.path.isEmpty()) name else "${directory.path}/$name"
                    pending.add(Directory(documentId, path))
                } else if (isVideoFile(name, mimeType)) {
                    results += ScannedFile(
                        documentUri = DocumentsContract.buildDocumentUriUsingTree(treeUri, documentId),
                        name = name,
                        relativePath = directory.path,
                        sizeBytes = it.getLong(3),
                        lastModified = it.getLong(4),
                        mimeType = mimeType,
                    )
                    if (results.size % PROGRESS_STEP == 0) onProgress(results.size)
                }
            }
        }
    }

    private companion object {
        const val PROGRESS_STEP = 50

        val Projection = arrayOf(
            Document.COLUMN_DOCUMENT_ID,
            Document.COLUMN_DISPLAY_NAME,
            Document.COLUMN_MIME_TYPE,
            Document.COLUMN_SIZE,
            Document.COLUMN_LAST_MODIFIED,
        )
    }
}
