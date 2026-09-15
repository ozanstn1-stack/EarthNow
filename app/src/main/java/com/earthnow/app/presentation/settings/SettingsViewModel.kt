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
    private val SettingsRepository: SettingsRepository,
    private val localDataRepository: LocalDataRepository
) : ViewModel() {

    private val _settings = MutableStateFlow<UserSettings?>(null)
    val settings: StateFlow<UserSettings?> = _settings.asStateFlow()

    init {
        viewModelScope.launch {
            _settings.value = SettingsRepository.settings.first()
        }
    }

    fun setTheme(mode: String) = launch { SettingsRepository.setTheme(mode); reload() }
    fun setMapStyle(style: String) = launch { SettingsRepository.setMapStyle(style); reload() }
    fun setTempUnit(unit: Units.TempUnit) = launch { SettingsRepository.setTempUnit(unit); reload() }
    fun setWindUnit(unit: Units.WindUnit) = launch { SettingsRepository.setWindUnit(unit); reload() }
    fun setPressureUnit(unit: Units.PressureUnit) = launch { SettingsRepository.setPressureUnit(unit); reload() }
    fun setDistUnit(unit: Units.DistUnit) = launch { SettingsRepository.setDistUnit(unit); reload() }
    fun setRefresh(min: Int) = launch { SettingsRepository.setRefreshMinutes(min); reload() }
    fun setBatterySaver(v: Boolean) = launch { SettingsRepository.setBatterySaver(v); reload() }
    fun setAiEnabled(v: Boolean) = launch { SettingsRepository.setAiEnabled(v); reload() }
    fun setAiProvider(p: String) = launch { SettingsRepository.setAiProvider(p); reload() }
    fun setNotificationPrefs(p: NotificationPrefs) = launch { SettingsRepository.setNotificationPrefs(p); reload() }

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
        _settings.value = SettingsRepository.settings.first()
    }
}


