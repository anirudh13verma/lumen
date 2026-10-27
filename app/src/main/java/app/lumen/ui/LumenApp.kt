package app.lumen.ui

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import app.lumen.data.settings.ThemeSettings
import app.lumen.data.model.LibraryItem
import app.lumen.ui.detail.DetailScreen
import app.lumen.ui.home.HomeScreen
import app.lumen.ui.library.LibraryScreen
import app.lumen.ui.library.LibraryViewModel
import app.lumen.ui.navigation.Destination
import app.lumen.ui.navigation.DetailRoute
import app.lumen.ui.settings.SettingsScreen
import app.lumen.ui.settings.SettingsViewModel

@Composable
fun LumenApp(
    settings: ThemeSettings,
    settingsViewModel: SettingsViewModel,
    libraryViewModel: LibraryViewModel,
) {
    val navController = rememberNavController()
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = backStackEntry?.destination

    val library by libraryViewModel.uiState.collectAsStateWithLifecycle()
    val message by libraryViewModel.message.collectAsStateWithLifecycle()

    // The system folder picker. The result is a persistable tree URI the repository stores.
    val folderPicker = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) libraryViewModel.addFolder(uri)
    }
    val onAddFolder: () -> Unit = { folderPicker.launch(null) }

    val snackbarHostState = remember { SnackbarHostState() }
    val context = LocalContext.current
    LaunchedEffect(message) {
        val pending = message
        if (pending != null) {
            snackbarHostState.showSnackbar(context.getString(pending))
            libraryViewModel.messageShown()
        }
    }

    // The detail screen is full-bleed, so the bottom bar hides there.
    val onDetail = currentDestination?.route == DetailRoute.Pattern
    val openItem: (LibraryItem) -> Unit = { navController.navigate(DetailRoute.create(it.id)) }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        bottomBar = {
            if (!onDetail) NavigationBar {
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
            composable(Destination.Home.route) {
                HomeScreen(state = library, onAddFolder = onAddFolder, onItemClick = openItem)
            }
            composable(Destination.Library.route) {
                LibraryScreen(state = library, onAddFolder = onAddFolder, onItemClick = openItem)
            }
            composable(
                route = DetailRoute.Pattern,
                arguments = listOf(navArgument(DetailRoute.ArgId) { type = NavType.StringType }),
            ) { entry ->
                val itemId = DetailRoute.decode(entry.arguments?.getString(DetailRoute.ArgId))
                DetailScreen(
                    item = library.items.firstOrNull { it.id == itemId },
                    onBack = { navController.popBackStack() },
                )
            }
            composable(Destination.Settings.route) {
                SettingsScreen(
                    settings = settings,
                    onModeChange = settingsViewModel::setMode,
                    onPresetSelected = settingsViewModel::setSeedColor,
                    onHuePreview = settingsViewModel::previewSeedColor,
                    onHueCommit = settingsViewModel::commitPreviewSeedColor,
                    onAmoledChange = settingsViewModel::setAmoled,
                    onSystemColorsChange = settingsViewModel::setUseSystemColors,
                    folders = library.folders,
                    scan = library.scan,
                    onAddFolder = onAddFolder,
                    onRemoveFolder = libraryViewModel::removeFolder,
                    onRescan = libraryViewModel::rescan,
                    tmdbConfigured = library.tmdbConfigured,
                    onRefetchMetadata = libraryViewModel::refetchMetadata,
                )
            }
        }
    }
}
