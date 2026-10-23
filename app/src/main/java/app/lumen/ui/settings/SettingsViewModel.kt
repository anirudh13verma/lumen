package app.lumen.ui.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import app.lumen.data.settings.SettingsRepository
import app.lumen.data.settings.ThemeMode
import app.lumen.data.settings.ThemeSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = SettingsRepository(application)

    /** Seed colour being dragged on the hue slider. Applied live, saved on release. */
    private val previewSeed = MutableStateFlow<Int?>(null)

    private val merged: Flow<ThemeSettings?> =
        combine(repository.themeSettings, previewSeed) { saved, preview ->
            if (preview != null) saved.copy(seedColor = preview) else saved
        }

    /** `null` until the first read from disk finishes, so the UI never flashes default colours. */
    val theme: StateFlow<ThemeSettings?> =
        merged.stateIn(viewModelScope, SharingStarted.Eagerly, null)

    fun setSeedColor(argb: Int) {
        viewModelScope.launch { repository.setSeedColor(argb) }
    }

    fun previewSeedColor(argb: Int) {
        previewSeed.value = argb
    }

    fun commitPreviewSeedColor() {
        val pending = previewSeed.value ?: return
        viewModelScope.launch {
            repository.setSeedColor(pending)
            previewSeed.value = null
        }
    }

    fun setMode(mode: ThemeMode) {
        viewModelScope.launch { repository.setMode(mode) }
    }

    fun setAmoled(enabled: Boolean) {
        viewModelScope.launch { repository.setAmoled(enabled) }
    }

    fun setUseSystemColors(enabled: Boolean) {
        viewModelScope.launch { repository.setUseSystemColors(enabled) }
    }
}
