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
}
