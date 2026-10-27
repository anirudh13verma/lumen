package app.lumen.data.db

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Upsert
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM library_folders ORDER BY addedAt")
    fun observeAll(): Flow<List<LibraryFolderEntity>>

    @Query("SELECT * FROM library_folders ORDER BY addedAt")
    suspend fun getAll(): List<LibraryFolderEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(folder: LibraryFolderEntity): Long

    @Query("DELETE FROM library_folders WHERE treeUri = :treeUri")
    suspend fun delete(treeUri: String)

    @Query("UPDATE library_folders SET lastScanAt = :at, lastScanFailed = :failed WHERE treeUri = :treeUri")
    suspend fun updateScanResult(treeUri: String, at: Long, failed: Boolean)
}

@Dao
interface MediaDao {
    /** Newest additions first. Files found in the same scan fall back to file modification time. */
    @Query("SELECT * FROM media_files ORDER BY addedAt DESC, lastModified DESC, name")
    fun observeAll(): Flow<List<MediaFileEntity>>

    @Query("SELECT documentUri, sizeBytes, lastModified, addedAt FROM media_files WHERE folderUri = :folderUri")
    suspend fun stamps(folderUri: String): List<FileStamp>

    @Upsert
    suspend fun upsertAll(files: List<MediaFileEntity>)

    @Query("DELETE FROM media_files WHERE documentUri IN (:uris)")
    suspend fun deleteByUris(uris: List<String>)

    /** Every file with its match (if any), newest additions first. */
    @Query(
        """
        SELECT f.documentUri AS documentUri, f.name AS name, f.relativePath AS relativePath,
               f.sizeBytes AS sizeBytes, f.lastModified AS lastModified, f.addedAt AS addedAt,
               m.mediaKey AS mediaKey, m.kind AS kind, m.title AS metaTitle,
               m.year AS metaYear, m.posterPath AS posterPath,
               m.backdropPath AS backdropPath, m.overview AS overview, m.rating AS rating
        FROM media_files f
        LEFT JOIN file_matches fm ON fm.documentUri = f.documentUri
        LEFT JOIN media_metadata m ON m.mediaKey = fm.mediaKey
        ORDER BY f.addedAt DESC, f.lastModified DESC, f.name
        """,
    )
    fun observeRows(): Flow<List<MediaRow>>

    /** Files never looked up, plus files whose last lookup failed before [retryBefore]. */
    @Query(
        """
        SELECT f.documentUri AS documentUri, f.name AS name, f.relativePath AS relativePath
        FROM media_files f
        LEFT JOIN file_matches fm ON fm.documentUri = f.documentUri
        WHERE fm.documentUri IS NULL OR (fm.status = ${MatchStatus.NotFound} AND fm.attemptedAt < :retryBefore)
        """,
    )
    suspend fun filesNeedingMatch(retryBefore: Long): List<PendingFile>

    @Upsert
    suspend fun upsertMetadata(metadata: MetadataEntity)

    @Upsert
    suspend fun upsertMatches(matches: List<FileMatchEntity>)

    @Query("DELETE FROM file_matches")
    suspend fun clearMatches()

    /** Drops metadata no file points at any more. */
    @Query("DELETE FROM media_metadata WHERE mediaKey NOT IN (SELECT mediaKey FROM file_matches WHERE mediaKey IS NOT NULL)")
    suspend fun pruneMetadata()
}
