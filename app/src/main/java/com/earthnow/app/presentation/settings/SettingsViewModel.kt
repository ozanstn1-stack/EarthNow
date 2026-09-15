package com.earthnow.app.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthnow.app.data.repository.LocalDataRepository
import com.earthnow.app.data.prefs.SettingsRepository
import com.earthnow.app.domain.model.NotificationPrefs
import com.earthnow.app.domain.model.UserSettings
import com.earthnow.app.util.Units
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val localDataRepository: LocalDataRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = settingsRepository.settings.first()
        }
    }

    fun setTheme(mode: String) = launch { settingsRepository.setTheme(mode); reload() }

    fun setLanguage(mode: String) {
        launch {
            settingsRepository.setLanguage(mode)
            // Apply immediately; the system also reapplies it on next launch.
            val locales = when (mode) {
                "en" -> androidx.core.os.LocaleListCompat.forLanguageTags("en")
                "tr" -> androidx.core.os.LocaleListCompat.forLanguageTags("tr")
                else -> androidx.core.os.LocaleListCompat.getEmptyLocaleList()
            }
            try {
                androidx.appcompat.app.AppCompatDelegate.setApplicationLocales(locales)
            } catch (e: Exception) { /* no-op */ }
            reload()
        }
    }

    fun setMapStyle(style: String) = launch { settingsRepository.setMapStyle(style); reload() }
    fun setTempUnit(unit: Units.TempUnit) = launch { settingsRepository.setTempUnit(unit); reload() }
    fun setWindUnit(unit: Units.WindUnit) = launch { settingsRepository.setWindUnit(unit); reload() }
    fun setPressureUnit(unit: Units.PressureUnit) = launch { settingsRepository.setPressureUnit(unit); reload() }
    fun setDistUnit(unit: Units.DistUnit) = launch { settingsRepository.setDistUnit(unit); reload() }
    fun setRefresh(min: Int) = launch { settingsRepository.setRefreshMinutes(min); reload() }
    fun setBatterySaver(v: Boolean) = launch { settingsRepository.setBatterySaver(v); reload() }
    fun setAiEnabled(v: Boolean) = launch { settingsRepository.setAiEnabled(v); reload() }
    fun setAiProvider(p: String) = launch { settingsRepository.setAiProvider(p); reload() }
    fun setNotificationPrefs(p: NotificationPrefs) = launch { settingsRepository.setNotificationPrefs(p); reload() }

    fun clearCache() {
        viewModelScope.launch {
            localDataRepository.clearSearchHistory()
            reload()
        }
    }

    private fun launch(block: suspend () -> Unit) {
        viewModelScope.launch { block() }
    }

    private suspend fun reload() {
        _settings.value = settingsRepository.settings.first()
    }
}