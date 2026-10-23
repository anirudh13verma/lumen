package app.lumen.data.settings

enum class ThemeMode { System, Light, Dark }

/**
 * Everything the user can change about how Lumen looks.
 *
 * The whole palette is generated from [seedColor], so users pick one colour and
 * every surface, container and accent stays harmonious.
 */
data class ThemeSettings(
    /** ARGB seed that the Material 3 tonal palette is generated from. */
    val seedColor: Int = DefaultSeed,
    val mode: ThemeMode = ThemeMode.System,
    /** Pure-black backgrounds in dark mode (saves power on OLED screens). */
    val amoled: Boolean = false,
    /** Use Material You wallpaper colours instead of [seedColor] (Android 12+). */
    val useSystemColors: Boolean = false,
) {
    companion object {
        val DefaultSeed: Int = 0xFFE8A33D.toInt()
    }
}
