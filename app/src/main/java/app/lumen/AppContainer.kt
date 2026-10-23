package app.lumen

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.lumen.data.LibraryRepository
import app.lumen.data.db.LumenDatabase
import app.lumen.data.scan.FolderScanner
import app.lumen.data.scan.ScanScheduler

/** Hand-rolled dependency container. Small enough that a DI framework would be overkill for now. */
class AppContainer(context: Context) {
    private val database: LumenDatabase =
        Room.databaseBuilder(context, LumenDatabase::class.java, "lumen.db").build()

    val libraryRepository = LibraryRepository(
        context = context,
        database = database,
        scanner = FolderScanner(context.contentResolver),
    )

    val scanScheduler = ScanScheduler(context)
}

class LumenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
