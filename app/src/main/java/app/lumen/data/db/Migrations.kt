package app.lumen.data.db

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

/** v1 -> v2: TMDB metadata and per-file match results. Existing library data is untouched. */
val Migration1To2 = object : Migration(1, 2) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `media_metadata` (" +
                "`mediaKey` TEXT NOT NULL, " +
                "`kind` TEXT NOT NULL, " +
                "`tmdbId` INTEGER NOT NULL, " +
                "`title` TEXT NOT NULL, " +
                "`year` INTEGER, " +
                "`overview` TEXT, " +
                "`posterPath` TEXT, " +
                "`backdropPath` TEXT, " +
                "`rating` REAL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`mediaKey`))",
        )
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `file_matches` (" +
                "`documentUri` TEXT NOT NULL, " +
                "`mediaKey` TEXT, " +
                "`status` INTEGER NOT NULL, " +
                "`attemptedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`documentUri`), " +
                "FOREIGN KEY(`documentUri`) REFERENCES `media_files`(`documentUri`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE, " +
                "FOREIGN KEY(`mediaKey`) REFERENCES `media_metadata`(`mediaKey`) " +
                "ON UPDATE NO ACTION ON DELETE SET NULL)",
        )
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_file_matches_mediaKey` ON `file_matches` (`mediaKey`)")
    }
}

/** v2 -> v3: watch progress for resume and "Continue watching". */
val Migration2To3 = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL(
            "CREATE TABLE IF NOT EXISTS `watch_progress` (" +
                "`documentUri` TEXT NOT NULL, " +
                "`positionMs` INTEGER NOT NULL, " +
                "`durationMs` INTEGER NOT NULL, " +
                "`completed` INTEGER NOT NULL, " +
                "`updatedAt` INTEGER NOT NULL, " +
                "PRIMARY KEY(`documentUri`), " +
                "FOREIGN KEY(`documentUri`) REFERENCES `media_files`(`documentUri`) " +
                "ON UPDATE NO ACTION ON DELETE CASCADE)",
        )
    }
}
