package app.lumen.ui.settings

import android.graphics.Color as AndroidColor
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import app.lumen.R
import app.lumen.data.settings.ThemeMode
import app.lumen.data.settings.ThemeSettings
import app.lumen.ui.theme.SeedPresets

@Composable
fun SettingsScreen(
    settings: ThemeSettings,
    onModeChange: (ThemeMode) -> Unit,
    onPresetSelected: (Int) -> Unit,
    onHuePreview: (Int) -> Unit,
    onHueCommit: () -> Unit,
    onAmoledChange: (Boolean) -> Unit,
    onSystemColorsChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
) {
    val supportsSystemColors = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
    val accentEnabled = !(settings.useSystemColors && supportsSystemColors)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(20.dp),
    ) {
        Text(
            text = stringResource(R.string.settings_title),
            style = MaterialTheme.typography.headlineMedium,
        )

        SectionLabel(stringResource(R.string.settings_appearance))

        // Theme mode
        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(stringResource(R.string.settings_theme_mode), style = MaterialTheme.typography.titleSmall)
            val modes = listOf(
                ThemeMode.System to R.string.settings_mode_system,
                ThemeMode.Light to R.string.settings_mode_light,
                ThemeMode.Dark to R.string.settings_mode_dark,
            )
            SingleChoiceSegmentedButtonRow(Modifier.fillMaxWidth()) {
                modes.forEachIndexed { index, (mode, labelRes) ->
                    SegmentedButton(
                        selected = settings.mode == mode,
                        onClick = { onModeChange(mode) },
                        shape = SegmentedButtonDefaults.itemShape(index = index, count = modes.size),
                    ) {
                        Text(stringResource(labelRes))
                    }
                }
            }
        }

        // Accent colour (greyed out while system colours are in use)
        Column(
            verticalArrangement = Arrangement.spacedBy(12.dp),
            modifier = Modifier.then(if (accentEnabled) Modifier else Modifier.alpha(0.4f)),
        ) {
            Text(stringResource(R.string.settings_accent_color), style = MaterialTheme.typography.titleSmall)
            SwatchRow(
                selectedSeed = settings.seedColor,
                enabled = accentEnabled,
                onSelected = onPresetSelected,
            )
            Text(
                stringResource(R.string.settings_custom_hue),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HueSlider(
                seedColor = settings.seedColor,
                enabled = accentEnabled,
                onPreview = onHuePreview,
                onCommit = onHueCommit,
            )
        }

        ToggleRow(
            title = stringResource(R.string.settings_amoled),
            summary = stringResource(R.string.settings_amoled_summary),
            checked = settings.amoled,
            onCheckedChange = onAmoledChange,
        )

        if (supportsSystemColors) {
            ToggleRow(
                title = stringResource(R.string.settings_system_colors),
                summary = stringResource(R.string.settings_system_colors_summary),
                checked = settings.useSystemColors,
                onCheckedChange = onSystemColorsChange,
            )
        }
    }
}

@Composable
private fun SectionLabel(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.primary,
    )
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SwatchRow(
    selectedSeed: Int,
    enabled: Boolean,
    onSelected: (Int) -> Unit,
) {
    FlowRow(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        SeedPresets.forEach { preset ->
            val selected = preset.color.toArgb() == selectedSeed
            val checkTint = if (preset.color.luminance() > 0.5f) Color.Black else Color.White
            Surface(
                shape = CircleShape,
                color = preset.color,
                border = if (selected) BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface) else null,
                modifier = Modifier
                    .size(44.dp)
                    .clip(CircleShape)
                    .clickable(enabled = enabled, role = Role.RadioButton) { onSelected(preset.color.toArgb()) },
            ) {
                if (selected) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Rounded.Check,
                            contentDescription = stringResource(R.string.settings_selected),
                            tint = checkTint,
                        )
                    }
                }
            }
        }
    }
}

private val HueStops: List<Color> = (0..360 step 60).map { Color.hsv(it.toFloat(), 0.7f, 0.9f) }

@Composable
private fun HueSlider(
    seedColor: Int,
    enabled: Boolean,
    onPreview: (Int) -> Unit,
    onCommit: () -> Unit,
) {
    // The seed colour is the single source of truth, so the thumb always matches what is applied.
    val hue = FloatArray(3).also { AndroidColor.colorToHSV(seedColor, it) }[0]

    Box(contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 10.dp) // inset by half the thumb width so the bar lines up with the track
                .height(12.dp)
                .clip(RoundedCornerShape(6.dp))
                .background(Brush.horizontalGradient(HueStops)),
        )
        Slider(
            value = hue,
            onValueChange = { onPreview(Color.hsv(it, 0.7f, 0.9f).toArgb()) },
            onValueChangeFinished = onCommit,
            valueRange = 0f..360f,
            enabled = enabled,
            colors = SliderDefaults.colors(
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                disabledActiveTrackColor = Color.Transparent,
                disabledInactiveTrackColor = Color.Transparent,
            ),
        )
    }
}

@Composable
private fun ToggleRow(
    title: String,
    summary: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(16.dp),
    ) {
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.bodyLarge)
            Text(
                summary,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}
