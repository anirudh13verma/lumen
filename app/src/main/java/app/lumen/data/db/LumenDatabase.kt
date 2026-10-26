package app.lumen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LibraryFolderEntity::class,
        MediaFileEntity::class,
        MetadataEntity::class,
        FileMatchEntity::class,
    ],
    version = 2,
    exportSchema = true,
)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun mediaDao(): MediaDao
}
