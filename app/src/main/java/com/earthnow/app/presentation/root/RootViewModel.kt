package com.earthnow.app.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthnow.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RootViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<com.earthnow.app.domain.model.UserSettings> =
        settingsRepository.settings.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            com.earthnow.app.domain.model.UserSettings()
        )

    suspend fun markOnboardingDone() {
        settingsRepository.setOnboardingDone()
    }

    /** Suspends until the real persisted settings are emitted by DataStore
     *  (avoids reading the in-memory default before the first load). */
    suspend fun awaitSettings(): com.earthnow.app.domain.model.UserSettings =
        settingsRepository.settings.first()
}