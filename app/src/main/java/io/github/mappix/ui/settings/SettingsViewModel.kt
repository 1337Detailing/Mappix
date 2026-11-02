package io.github.mappix.ui.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mappix.data.preferences.SettingsRepository
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(private val repository: SettingsRepository) : ViewModel() {
    val settings = repository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    fun dark(enabled: Boolean) = viewModelScope.launch { repository.setDarkMode(enabled) }
    fun overlay(enabled: Boolean) = viewModelScope.launch { repository.setOverlay(enabled) }
    fun language(code: String) = viewModelScope.launch { repository.setLanguage(code) }
    fun onboardingDone() = viewModelScope.launch { repository.setOnboardingSeen() }
}
