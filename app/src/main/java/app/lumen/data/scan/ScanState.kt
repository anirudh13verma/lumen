package app.lumen.data.scan

sealed interface ScanState {
    data object Idle : ScanState

    /** [folderName] is null while the scan is queued and has not started on a folder yet. */
    data class Scanning(val folderName: String?, val filesFound: Int) : ScanState
}
