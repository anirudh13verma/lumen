package app.lumen

import android.app.Application
import android.content.Context
import androidx.room.Room
import app.lumen.data.LibraryRepository
import app.lumen.data.db.LumenDatabase
import app.lumen.data.db.Migration1To2
import app.lumen.data.db.Migration2To3
import app.lumen.data.playback.PlaybackRepository
import app.lumen.data.meta.MetadataRepository
import app.lumen.data.meta.TmdbClient
import app.lumen.data.scan.FolderScanner
import app.lumen.data.scan.ScanScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

/** Hand-rolled dependency container. Small enough that a DI framework would be overkill for now. */
class AppContainer(context: Context) {
    private val database: LumenDatabase =
        Room.databaseBuilder(context, LumenDatabase::class.java, "lumen.db")
            .addMigrations(Migration1To2, Migration2To3)
            .build()

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .build()

    val metadataRepository = MetadataRepository(
        database = database,
        tmdb = TmdbClient(httpClient, BuildConfig.TMDB_API_KEY),
    )

    val libraryRepository = LibraryRepository(
        context = context,
        database = database,
        scanner = FolderScanner(context.contentResolver),
    )

    val scanScheduler = ScanScheduler(context)

    val playbackRepository = PlaybackRepository(database)

    /** For work that must outlive a screen, such as saving the playback position as the player closes. */
    val applicationScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
}

class LumenApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
