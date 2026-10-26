package app.lumen.data.scan

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.lumen.LumenApplication

/** Rescans every library folder, then looks up posters for new files, reporting progress as it goes. */
class ScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val container = (applicationContext as LumenApplication).container
        val repository = container.libraryRepository

        var filesSoFar = 0
        for (folder in repository.getFolders()) {
            setProgress(workDataOf(KeyFolder to folder.displayName, KeyFiles to filesSoFar))

            val result = repository.syncFolder(folder) { found ->
                setProgress(workDataOf(KeyFolder to folder.displayName, KeyFiles to filesSoFar + found))
            }
            filesSoFar += result.filesFound
        }

        container.metadataRepository.matchPending { done, total ->
            setProgress(workDataOf(KeyPhase to PhaseMetadata, KeyDone to done, KeyTotal to total))
        }
        return Result.success()
    }

    companion object {
        const val KeyFolder = "folder"
        const val KeyFiles = "files"
        const val KeyPhase = "phase"
        const val KeyDone = "done"
        const val KeyTotal = "total"
        const val PhaseMetadata = "metadata"
    }
}
