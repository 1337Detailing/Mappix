package io.github.mappix.service

import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.model.ResolvedAction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MappingRuntime @Inject constructor() {
    private val _running = MutableStateFlow(false)
    private val _foregroundPackage = MutableStateFlow<String?>(null)
    private val _activeProfile = MutableStateFlow<Profile?>(null)
    private val _lastAction = MutableSharedFlow<ResolvedAction>(extraBufferCapacity = 64)

    val running = _running.asStateFlow()
    val foregroundPackage = _foregroundPackage.asStateFlow()
    val activeProfile = _activeProfile.asStateFlow()
    val lastAction = _lastAction.asSharedFlow()

    fun setRunning(value: Boolean) { _running.value = value }
    fun foreground(packageName: String?) { _foregroundPackage.value = packageName }
    fun active(profile: Profile?) { _activeProfile.value = profile }
    fun emitted(action: ResolvedAction) { _lastAction.tryEmit(action) }
}
