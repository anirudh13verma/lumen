package app.lumen.ui.navigation

import androidx.annotation.StringRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Home
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.outlined.VideoLibrary
import androidx.compose.material.icons.rounded.Home
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material.icons.rounded.VideoLibrary
import androidx.compose.ui.graphics.vector.ImageVector
import app.lumen.R

/** Top-level screens shown in the bottom navigation bar. */
enum class Destination(
    val route: String,
    @StringRes val labelRes: Int,
    val icon: ImageVector,
    val selectedIcon: ImageVector,
) {
    Home("home", R.string.nav_home, Icons.Outlined.Home, Icons.Rounded.Home),
    Library("library", R.string.nav_library, Icons.Outlined.VideoLibrary, Icons.Rounded.VideoLibrary),
    Settings("settings", R.string.nav_settings, Icons.Outlined.Settings, Icons.Rounded.Settings),
}
