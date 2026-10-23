package app.lumen.ui.library

import android.app.Application
import android.net.Uri
import androidx.annotation.StringRes
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lumen.LumenApplication
import app.lumen.R
import app.lumen.data.AddFolderResult
import app.lumen.data.model.LibraryFolder
import app.lumen.data.model.MediaFile
import app.lumen.data.scan.ScanState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

data class LibraryUiState(
    /** False until the first read from the database finishes, so empty states never flash. */
    val loaded: Boolean = false,
    val folders: List<LibraryFolder> = emptyList(),
    val files: List<MediaFile> = emptyList(),
    val scan: ScanState = ScanState.Idle,
)

class LibraryViewModel(application: Application) : AndroidViewModel(application) {

    private val container = (application as LumenApplication).container
    private val repository = container.libraryRepository
    private val scheduler = container.scanScheduler

    val uiState: StateFlow<LibraryUiState> = combine(
        repository.folders,
        repository.files,
        scheduler.state,
    ) { folders, files, scan ->
        LibraryUiState(loaded = true, folders = folders, files = files, scan = scan)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), LibraryUiState())

    private val _message = MutableStateFlow<Int?>(null)

    /** A one-off string resource to show in a snackbar, or null. Call [messageShown] after showing. */
    val message: StateFlow<Int?> = _message

    fun scanOnLaunch() = scheduler.scan()

    fun rescan() = scheduler.scan()

    fun addFolder(treeUri: Uri) {
        viewModelScope.launch {
            try {
                when (repository.addFolder(treeUri)) {
                    AddFolderResult.Added -> scheduler.scan()
                    AddFolderResult.AlreadyCovered -> post(R.string.library_folder_already_added)
                }
            } catch (e: SecurityException) {
                // The picker did not hand over a grant that can be kept across restarts.
                post(R.string.library_folder_add_failed)
            } catch (e: IllegalArgumentException) {
                // Not a folder (tree) URI.
                post(R.string.library_folder_add_failed)
            }
        }
    }

    fun removeFolder(folder: LibraryFolder) {
        viewModelScope.launch { repository.removeFolder(folder.treeUri) }
    }

    fun messageShown() {
        _message.value = null
    }

    private fun post(@StringRes message: Int) {
        _message.value = message
    }
}
