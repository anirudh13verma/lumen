package app.lumen.ui.theme

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import app.lumen.data.settings.ThemeMode
import app.lumen.data.settings.ThemeSettings
import com.materialkolor.rememberDynamicColorScheme

@Composable
fun LumenTheme(
    settings: ThemeSettings,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (settings.mode) {
        ThemeMode.System -> isSystemInDarkTheme()
        ThemeMode.Light -> false
        ThemeMode.Dark -> true
    }
    val amoled = darkTheme && settings.amoled
    val context = LocalContext.current

    val colorScheme: ColorScheme =
        if (settings.useSystemColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val system = if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
            if (amoled) system.copy(background = Color.Black, surface = Color.Black) else system
        } else {
            rememberDynamicColorScheme(
                seedColor = Color(settings.seedColor),
                isDark = darkTheme,
                isAmoled = amoled,
            )
        }

    // The in-app Light/Dark override can differ from the system setting, so the
    // status/navigation bar icons have to follow the app theme, not the OS.
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = context.findActivity()?.window ?: return@SideEffect
            val controller = WindowCompat.getInsetsController(window, view)
            controller.isAppearanceLightStatusBars = !darkTheme
            controller.isAppearanceLightNavigationBars = !darkTheme
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        content = content,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
