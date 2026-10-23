package app.lumen.ui

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import app.lumen.data.settings.ThemeSettings
import app.lumen.ui.home.HomeScreen
import app.lumen.ui.library.LibraryScreen
import app.lumen.ui.navigation.Destination
import app.lumen.ui.settings.SettingsScreen
import app.lumen.ui.settings.SettingsViewModel

@Composable
fun LumenApp(
    settings: ThemeSettings,
    settingsViewModel: SettingsViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    Scaffold(
        bottomBar = {
            NavigationBar {
                Destination.entries.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) { saveState = true }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = {
                            Icon(
                                imageVector = if (selected) destination.selectedIcon else destination.icon,
                                contentDescription = null,
                            )
                        },
                        label = { Text(stringResource(destination.labelRes)) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = Destination.Home.route,
            modifier = Modifier.padding(innerPadding),
        ) {
            composable(Destination.Home.route) { HomeScreen() }
            composable(Destination.Library.route) { LibraryScreen() }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onModeChange = settingsViewModel::setMode,
                    onPresetSelected = settingsViewModel::setSeedColor,
                    onHuePreview = settingsViewModel::previewSeedColor,
                    onHueCommit = settingsViewModel::commitPreviewSeedColor,
                    onAmoledChange = settingsViewModel::setAmoled,
                    onSystemColorsChange = settingsViewModel::setUseSystemColors,
                )
            }
        }
    }
}
