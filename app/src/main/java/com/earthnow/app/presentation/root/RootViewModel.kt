package com.earthnow.app.presentation.root

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.earthnow.app.data.prefs.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn

@HiltViewModel
class RootViewModel @Inject constructor(
    private val SettingsRepository: SettingsRepository
) : ViewModel() {
    val settings: StateFlow<com.earthnow.app.domain.model.UserSettings> =
        SettingsRepository.settings.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            com.earthnow.app.domain.model.UserSettings()
        )

    suspend fun markOnboardingDone() {
        SettingsRepository.setOnboardingDone()
    }
}


