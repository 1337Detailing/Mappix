package io.github.mappix.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mappix.data.preferences.SettingsRepository
import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.repository.ProfileRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val repository: ProfileRepository,
    private val settingsRepository: SettingsRepository
) : ViewModel() {
    val profiles = repository.observeAll().stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val settings = settingsRepository.settings.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)
    val message = MutableStateFlow<String?>(null)

    fun create(name: String, onCreated: (String) -> Unit) = viewModelScope.launch {
        runCatching { Profile(name = name.trim()).also { repository.save(it) } }
            .onSuccess { onCreated(it.id) }
            .onFailure { message.value = it.localizedMessage }
    }
    fun rename(profile: Profile, name: String) = viewModelScope.launch { runCatching { repository.save(profile.copy(name = name)) }.onFailure { message.value = it.message } }
    fun duplicate(id: String) = viewModelScope.launch { runCatching { repository.duplicate(id) }.onFailure { message.value = it.message } }
    fun delete(id: String) = viewModelScope.launch {
        repository.delete(id)
        if (settings.value?.selectedProfileId == id) settingsRepository.selectProfile(null)
    }
    fun select(id: String) = viewModelScope.launch { settingsRepository.selectProfile(id) }
    fun export(profile: Profile) = repository.exportJson(profile)
    fun import(text: String) = viewModelScope.launch {
        runCatching { repository.importJson(text) }.onFailure { message.value = it.localizedMessage }
    }
    fun clearMessage() { message.value = null }
}
