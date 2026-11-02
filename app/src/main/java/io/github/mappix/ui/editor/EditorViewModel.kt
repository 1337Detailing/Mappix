package io.github.mappix.ui.editor

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.model.InputSource
import io.github.mappix.domain.model.Mapping
import io.github.mappix.domain.model.OutputAction
import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.repository.ControllerRepository
import io.github.mappix.domain.repository.ProfileRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class EditorViewModel @Inject constructor(
    private val profiles: ProfileRepository,
    private val controllers: ControllerRepository
) : ViewModel() {
    val profile = MutableStateFlow<Profile?>(null)
    val selectedMapping = MutableStateFlow<String?>(null)
    val awaitingSource = MutableStateFlow(false)
    val error = MutableStateFlow<String?>(null)
    private var observer: Job? = null
    private var saveJob: Job? = null

    init {
        viewModelScope.launch {
            controllers.events.collect { event ->
                if (!awaitingSource.value) return@collect
                when (event) {
                    is ControllerEvent.Button -> if (event.pressed) capture(InputSource.Button(event.keyCode))
                    is ControllerEvent.Axes -> {
                        val candidate = event.values.entries.firstOrNull { (axis, value) ->
                            axis !in listOf(MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
                                MotionEvent.AXIS_RX, MotionEvent.AXIS_RY, MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y) && value > 0.75f
                        }
                        if (candidate != null) capture(InputSource.Axis(candidate.key))
                    }
                }
            }
        }
    }

    fun open(id: String) {
        if (profile.value?.id == id && observer?.isActive == true) return
        observer?.cancel()
        selectedMapping.value = null
        observer = viewModelScope.launch { profiles.observe(id).collectLatest { profile.value = it } }
    }

    fun captureNext() { awaitingSource.value = true }
    fun cancelCapture() { awaitingSource.value = false }
    private fun capture(source: InputSource) {
        awaitingSource.value = false
        add(source)
    }

    fun add(source: InputSource) {
        val default = if (source is InputSource.Stick) OutputAction.DirectionalKeys()
            else OutputAction.Tap()
        val mapping = Mapping(source = source, output = default)
        updateProfile { copy(mappings = mappings + mapping) }
        selectedMapping.value = mapping.id
    }

    fun edit(mapping: Mapping) = updateProfile { copy(mappings = mappings.map { if (it.id == mapping.id) mapping else it }) }
    fun remove(id: String) {
        updateProfile { copy(mappings = mappings.filterNot { it.id == id }) }
        if (selectedMapping.value == id) selectedMapping.value = null
    }
    fun setPackageName(value: String) = updateProfile { copy(packageName = value.trim().ifBlank { null }) }

    private fun updateProfile(block: Profile.() -> Profile) {
        val current = profile.value ?: return
        val updated = current.block()
        // Optimisme UI pour éviter de perdre des changements rapprochés.
        profile.value = updated
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(180) // Regroupe les modifications des curseurs.
            runCatching { profiles.save(updated) }.onFailure { error.value = it.localizedMessage }
        }
    }
}
