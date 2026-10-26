package app.lumen.data.scan

sealed interface ScanState {
    data object Idle : ScanState

    /** [folderName] is null while the scan is queued and has not started on a folder yet. */
    data class Scanning(val folderName: String?, val filesFound: Int) : ScanState

    /** Folders are scanned; posters and details are being looked up on TMDB. */
    data class FetchingMetadata(val done: Int, val total: Int) : ScanState
}

/** True while anything (folder scan or poster lookup) is running or queued. */
val ScanState.isBusy: Boolean get() = this !is ScanState.Idle
