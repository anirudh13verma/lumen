package app.lumen.data.scan

import android.content.Context
import androidx.work.ExistingWorkPolicy
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class ScanScheduler(context: Context) {

    private val workManager = WorkManager.getInstance(context)

    /**
     * Queues a library scan. If one is already running the new request is chained after it, so a
     * folder added mid-scan still gets scanned. Rescans are incremental, so the extra run is cheap.
     */
    fun scan() {
        workManager.enqueueUniqueWork(
            UniqueName,
            ExistingWorkPolicy.APPEND_OR_REPLACE,
            OneTimeWorkRequestBuilder<ScanWorker>().build(),
        )
    }

    val state: Flow<ScanState> = workManager.getWorkInfosForUniqueWorkFlow(UniqueName).map { infos ->
        val running = infos.firstOrNull { it.state == WorkInfo.State.RUNNING }
        when {
            running?.progress?.getString(ScanWorker.KeyPhase) == ScanWorker.PhaseMetadata -> ScanState.FetchingMetadata(
                done = running.progress.getInt(ScanWorker.KeyDone, 0),
                total = running.progress.getInt(ScanWorker.KeyTotal, 0),
            )
            running != null -> ScanState.Scanning(
                folderName = running.progress.getString(ScanWorker.KeyFolder),
                filesFound = running.progress.getInt(ScanWorker.KeyFiles, 0),
            )
            infos.any { it.state == WorkInfo.State.ENQUEUED } -> ScanState.Scanning(folderName = null, filesFound = 0)
            else -> ScanState.Idle
        }
    }

    private companion object {
        const val UniqueName = "library-scan"
    }
}
