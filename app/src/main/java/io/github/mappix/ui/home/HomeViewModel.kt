package io.github.mappix.ui.home

import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mappix.data.preferences.SettingsRepository
import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.repository.ControllerRepository
import io.github.mappix.domain.repository.ProfileRepository
import io.github.mappix.service.AccessibilityBridge
import io.github.mappix.service.MappingForegroundService
import io.github.mappix.service.MappingRuntime
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    controllerRepository: ControllerRepository,
    profileRepository: ProfileRepository,
    private val settingsRepository: SettingsRepository,
    val runtime: MappingRuntime,
    val bridge: AccessibilityBridge
) : ViewModel() {
    val failure = MutableStateFlow<String?>(null)
    val controllers = controllerRepository.controllers
    val profiles = profileRepository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val running = runtime.running
    val active = runtime.activeProfile

    fun start() {
        runCatching {
            ContextCompat.startForegroundService(context, Intent(context, MappingForegroundService::class.java))
        }.onFailure { failure.value = it.localizedMessage ?: it.javaClass.simpleName }
    }
    fun stop() { context.stopService(Intent(context, MappingForegroundService::class.java)) }
}
