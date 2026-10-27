package app.lumen.data.db

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(
    entities = [
        LibraryFolderEntity::class,
        MediaFileEntity::class,
        MetadataEntity::class,
        FileMatchEntity::class,
        ProgressEntity::class,
    ],
    version = 3,
    exportSchema = true,
)
abstract class LumenDatabase : RoomDatabase() {
    abstract fun folderDao(): FolderDao
    abstract fun mediaDao(): MediaDao
    abstract fun progressDao(): ProgressDao
}
