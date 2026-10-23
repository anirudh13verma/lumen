package app.lumen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [LibraryFolderEntity::class, MediaFileEntity::class],
    version = 1,
    exportSchema = false,
)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun mediaDao(): MediaDao
}
