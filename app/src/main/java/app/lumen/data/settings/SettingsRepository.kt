package app.lumen.data.settings

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(name = "lumen_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val Seed = intPreferencesKey("theme_seed")
        val Mode = stringPreferencesKey("theme_mode")
        val Amoled = booleanPreferencesKey("theme_amoled")
        val SystemColors = booleanPreferencesKey("theme_system_colors")
    }

    val themeSettings: Flow<ThemeSettings> = context.settingsDataStore.data
        .catch { error ->
            // A corrupt/unreadable file should fall back to defaults, not crash the app.
            if (error is IOException) emit(emptyPreferences()) else throw error
        }
        .map { prefs ->
            ThemeSettings(
                seedColor = prefs[Keys.Seed] ?: ThemeSettings.DefaultSeed,
                mode = prefs[Keys.Mode]
                    ?.let { name -> ThemeMode.entries.firstOrNull { it.name == name } }
                    ?: ThemeMode.System,
                amoled = prefs[Keys.Amoled] ?: false,
                useSystemColors = prefs[Keys.SystemColors] ?: false,
            )
        }

    suspend fun setSeedColor(argb: Int) {
        context.settingsDataStore.edit { it[Keys.Seed] = argb }
    }

    suspend fun setMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[Keys.Mode] = mode.name }
    }

    suspend fun setAmoled(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.Amoled] = enabled }
    }

    suspend fun setUseSystemColors(enabled: Boolean) {
        context.settingsDataStore.edit { it[Keys.SystemColors] = enabled }
    }
}
