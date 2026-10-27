package app.lumen.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import app.lumen.R
import app.lumen.data.model.ItemKind
import app.lumen.data.model.LibraryItem

/** "3 episodes" for a show, "2021" for a matched movie, the file size otherwise. */
@Composable
fun itemSubtitle(item: LibraryItem): String = when {
    item.kind == ItemKind.Show ->
        pluralStringResource(R.plurals.episode_count, item.fileCount, item.fileCount)
    item.year != null -> stringResource(R.string.item_year, item.year)
    else -> rememberFileSize(item.totalSizeBytes)
}

/** Detail-screen meta line, e.g. "2022 · ★ 8.7 · 9 episodes". */
@Composable
fun itemMeta(item: LibraryItem): String {
    val parts = buildList {
        item.year?.let { add(it.toString()) }
        item.rating?.let { add(stringResource(R.string.item_rating, it)) }
        add(
            if (item.kind == ItemKind.Show) {
                pluralStringResource(R.plurals.episode_count, item.fileCount, item.fileCount)
            } else {
                rememberFileSize(item.totalSizeBytes)
            },
        )
    }
    return parts.joinToString(" · ")
}
