package app.lumen.ui.player

import androidx.media3.common.C
import androidx.media3.common.Player
import androidx.media3.common.TrackSelectionOverride
import androidx.media3.common.Tracks
import java.util.Locale

/**
 * Builds the choices for the audio or subtitle panel from what the file actually contains.
 * [offLabel] adds a "turn off" entry (subtitles); audio always has one track playing.
 */
internal fun buildTrackOptions(
    player: Player,
    tracks: Tracks,
    type: Int,
    offLabel: String?,
    fallbackLabel: (number: Int) -> String,
): List<TrackOption> {
    val options = ArrayList<TrackOption>()

    if (offLabel != null) {
        options += TrackOption(
            label = offLabel,
            selected = !tracks.isTypeSelected(type),
            onSelect = {
                player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                    .setTrackTypeDisabled(type, true)
                    .build()
            },
        )
    }

    var number = 0
    for (group in tracks.groups) {
        if (group.type != type) continue
        for (index in 0 until group.length) {
            if (!group.isTrackSupported(index)) continue
            number++
            val format = group.getTrackFormat(index)
            val label = format.label?.takeIf { it.isNotBlank() }
                ?: languageName(format.language)
                ?: fallbackLabel(number)
            options += TrackOption(
                label = label,
                selected = group.isTrackSelected(index),
                onSelect = {
                    player.trackSelectionParameters = player.trackSelectionParameters.buildUpon()
                        .setTrackTypeDisabled(type, false)
                        .setOverrideForType(TrackSelectionOverride(group.mediaTrackGroup, index))
                        .build()
                },
            )
        }
    }
    return options
}

internal const val TextTrackType = C.TRACK_TYPE_TEXT
internal const val AudioTrackType = C.TRACK_TYPE_AUDIO

private fun languageName(code: String?): String? {
    if (code.isNullOrBlank() || code == "und") return null
    return Locale.forLanguageTag(code).displayLanguage
        .takeIf { it.isNotBlank() }
        ?.replaceFirstChar { it.uppercase() }
}
