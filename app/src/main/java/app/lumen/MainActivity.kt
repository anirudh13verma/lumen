package app.lumen

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import app.lumen.ui.LumenApp
import app.lumen.ui.library.LibraryViewModel
import app.lumen.ui.settings.SettingsViewModel
import app.lumen.ui.theme.LumenTheme

class MainActivity : ComponentActivity() {

    private val settingsViewModel: SettingsViewModel by viewModels()
    private val libraryViewModel: LibraryViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        // Pick up files added or removed while the app was closed. Skipped on rotation and
        // other recreations (savedInstanceState != null) so those don't trigger a rescan.
        if (savedInstanceState == null) libraryViewModel.scanOnLaunch()

        setContent {
            val settings by settingsViewModel.theme.collectAsStateWithLifecycle()
            // Wait for the saved theme so the first frame never flashes the default colours.
            settings?.let { theme ->
                LumenTheme(theme) {
                    LumenApp(
                        settings = theme,
                        settingsViewModel = settingsViewModel,
                        libraryViewModel = libraryViewModel,
                    )
                }
            }
        }
    }
}
