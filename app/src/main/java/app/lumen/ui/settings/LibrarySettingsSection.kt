package app.lumen.ui.settings

import android.text.format.DateUtils
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.CreateNewFolder
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Folder
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.model.LibraryFolder
import app.lumen.data.scan.ScanState
import app.lumen.data.scan.isBusy

/** Manage which folders are part of the library. Lives on the Settings screen. */
@Composable
fun LibrarySettingsSection(
    folders: List<LibraryFolder>,
    scan: ScanState,
    onAddFolder: () -> Unit,
    onRemoveFolder: (LibraryFolder) -> Unit,
    onRescan: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.settings_library),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
        )

        if (folders.isEmpty()) {
            Text(
                text = stringResource(R.string.settings_no_folders),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        } else {
            folders.forEach { folder ->
                FolderRow(folder = folder, onRemove = { onRemoveFolder(folder) })
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            FilledTonalButton(onClick = onAddFolder) {
                Icon(Icons.Rounded.CreateNewFolder, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.add_folder), modifier = Modifier.padding(start = 8.dp))
            }
            OutlinedButton(
                onClick = onRescan,
                enabled = folders.isNotEmpty() && !scan.isBusy,
            ) {
                Icon(Icons.Rounded.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Text(stringResource(R.string.settings_rescan), modifier = Modifier.padding(start = 8.dp))
            }
        }

        Text(
            text = stringResource(R.string.settings_library_hint),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FolderRow(
    folder: LibraryFolder,
    onRemove: () -> Unit,
) {
    val scannedAt = folder.lastScanAt
    val status = when {
        folder.lastScanFailed -> stringResource(R.string.settings_folder_failed)
        scannedAt == null -> stringResource(R.string.settings_folder_never_scanned)
        else -> stringResource(
            R.string.settings_folder_last_scan,
            DateUtils.getRelativeTimeSpanString(
                scannedAt,
                System.currentTimeMillis(),
                DateUtils.MINUTE_IN_MILLIS,
            ).toString(),
        )
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(
            imageVector = Icons.Rounded.Folder,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Column(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 16.dp),
        ) {
            Text(
                text = folder.displayName,
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            Text(
                text = status,
                style = MaterialTheme.typography.bodyMedium,
                color = if (folder.lastScanFailed) {
                    MaterialTheme.colorScheme.error
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
            )
        }
        IconButton(onClick = onRemove) {
            Icon(
                imageVector = Icons.Rounded.Delete,
                contentDescription = stringResource(R.string.settings_remove_folder, folder.displayName),
            )
        }
    }
}
