package app.lumen.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.scan.ScanState

/**
 * Shown while a scan runs. Scanning has no known total, so its spinner is indeterminate;
 * fetching posters knows how many files it has to do, so it shows real progress.
 */
@Composable
fun ScanStatusCard(
    scan: ScanState,
    modifier: Modifier = Modifier,
) {
    if (scan is ScanState.Idle) return

    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp),
        ) {
            if (scan is ScanState.FetchingMetadata && scan.total > 0) {
                CircularProgressIndicator(
                    progress = { scan.done.toFloat() / scan.total },
                    modifier = Modifier.size(24.dp),
                    strokeWidth = 3.dp,
                )
            } else {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 3.dp)
            }
            Column {
                Text(
                    text = stringResource(
                        if (scan is ScanState.FetchingMetadata) R.string.scan_fetching_posters else R.string.scan_scanning,
                    ),
                    style = MaterialTheme.typography.titleSmall,
                    color = MaterialTheme.colorScheme.onSecondaryContainer,
                )
                val detail = when (scan) {
                    is ScanState.Scanning ->
                        scan.folderName?.takeIf { scan.filesFound > 0 }
                            ?.let { stringResource(R.string.scan_in_folder, scan.filesFound, it) }
                    is ScanState.FetchingMetadata ->
                        stringResource(R.string.scan_fetching_progress, scan.done, scan.total)
                    ScanState.Idle -> null
                }
                if (detail != null) {
                    Text(
                        text = detail,
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                }
            }
        }
    }
}

/** First-run call to action, shown when no folders have been added yet. */
@Composable
fun EmptyLibraryCard(
    onAddFolder: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Text(
                text = stringResource(R.string.library_empty_title),
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Text(
                text = stringResource(R.string.library_empty_body),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSecondaryContainer,
            )
            Button(onClick = onAddFolder) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(
                    text = stringResource(R.string.add_folder),
                    modifier = Modifier.padding(start = 8.dp),
                )
            }
        }
    }
}
