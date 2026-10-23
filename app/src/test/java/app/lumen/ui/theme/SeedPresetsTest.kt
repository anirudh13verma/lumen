package app.lumen.ui.theme

import androidx.compose.ui.graphics.toArgb
import app.lumen.data.settings.ThemeSettings
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SeedPresetsTest {

    @Test
    fun defaultSeedMatchesAFirstClassPreset() {
        // Keeps the Settings swatch row highlighting the default colour on a fresh install.
        assertTrue(SeedPresets.any { it.color.toArgb() == ThemeSettings.DefaultSeed })
    }

    @Test
    fun presetNamesAreUnique() {
        assertEquals(SeedPresets.size, SeedPresets.map { it.name }.toSet().size)
    }
}
