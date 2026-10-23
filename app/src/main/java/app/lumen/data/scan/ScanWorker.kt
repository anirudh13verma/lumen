package app.lumen.data.scan

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import androidx.work.workDataOf
import app.lumen.LumenApplication

/** Rescans every library folder in the background, reporting progress as it goes. */
class ScanWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    override suspend fun doWork(): Result {
        val repository = (applicationContext as LumenApplication).container.libraryRepository

        var filesSoFar = 0
        for (folder in repository.getFolders()) {
            setProgress(workDataOf(KeyFolder to folder.displayName, KeyFiles to filesSoFar))

            val result = repository.syncFolder(folder) { found ->
                setProgress(workDataOf(KeyFolder to folder.displayName, KeyFiles to filesSoFar + found))
            }
            filesSoFar += result.filesFound
        }
        return Result.success()
    }

    companion object {
        const val KeyFolder = "folder"
        const val KeyFiles = "files"
    }
}
