package io.github.mappix.ui.tester

import android.view.KeyEvent
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class TesterViewModel @Inject constructor(repository: ControllerRepository) : ViewModel() {
    val held = MutableStateFlow<Set<Int>>(emptySet())
    val axes = MutableStateFlow<Map<Int, Float>>(emptyMap())
    val lastDeviceId = MutableStateFlow<Int?>(null)
    val devices = repository.controllers
    val selectedDeviceId = MutableStateFlow<Int?>(null)
    val history = MutableStateFlow<List<String>>(emptyList())

    fun selectDevice(id: Int) {
        selectedDeviceId.value = id
        held.value = emptySet()
        axes.value = emptyMap()
        history.value = emptyList()
    }

    fun clearHistory() { history.value = emptyList() }

    init {
        viewModelScope.launch {
            repository.events.collect { event ->
                if (selectedDeviceId.value == null) selectedDeviceId.value = event.deviceId
                if (selectedDeviceId.value != event.deviceId) return@collect
                lastDeviceId.value = event.deviceId
                when (event) {
                    is ControllerEvent.Button -> {
                        held.value = held.value.toMutableSet().apply {
                            if (event.pressed) add(event.keyCode) else remove(event.keyCode)
                        }
                        history.value = (listOf("${KeyEvent.keyCodeToString(event.keyCode)} : " +
                            if (event.pressed) "DOWN" else "UP") + history.value).take(20)
                    }
                    is ControllerEvent.Axes -> axes.value = event.values
                }
            }
        }
        viewModelScope.launch {
            repository.controllers.collect { list ->
                if (list.none { it.deviceId == selectedDeviceId.value }) {
                    selectedDeviceId.value = list.firstOrNull()?.deviceId
                    held.value = emptySet(); axes.value = emptyMap(); lastDeviceId.value = null
                    history.value = emptyList()
                }
            }
        }
    }
}
