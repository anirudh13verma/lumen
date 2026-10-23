package app.lumen.ui.theme

import androidx.compose.ui.graphics.Color

data class SeedPreset(val name: String, val color: Color)

/** Quick-pick accent colours shown in Settings. Any hue is also reachable via the slider. */
val SeedPresets: List<SeedPreset> = listOf(
    SeedPreset("Amber", Color(0xFFE8A33D)),
    SeedPreset("Coral", Color(0xFFFF6F61)),
    SeedPreset("Rose", Color(0xFFE91E78)),
    SeedPreset("Violet", Color(0xFF7C5CFF)),
    SeedPreset("Indigo", Color(0xFF3F51B5)),
    SeedPreset("Sky", Color(0xFF2196F3)),
    SeedPreset("Teal", Color(0xFF00A8A8)),
    SeedPreset("Emerald", Color(0xFF2E9E5B)),
    SeedPreset("Lime", Color(0xFF8BC34A)),
)
