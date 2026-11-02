package io.github.mappix.input

import android.content.Context
import android.hardware.input.InputManager
import android.os.Handler
import android.os.Looper
import android.view.InputDevice
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.model.ControllerInfo
import io.github.mappix.domain.repository.ControllerRepository
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AndroidControllerRepository @Inject constructor(@ApplicationContext context: Context) : ControllerRepository {
    private val manager = context.getSystemService(Context.INPUT_SERVICE) as InputManager
    private val _controllers = MutableStateFlow<List<ControllerInfo>>(emptyList())
    private val _events = MutableSharedFlow<ControllerEvent>(extraBufferCapacity = 256)
    override val controllers = _controllers.asStateFlow()
    override val events = _events.asSharedFlow()

    private val listener = object : InputManager.InputDeviceListener {
        override fun onInputDeviceAdded(deviceId: Int) = refresh()
        override fun onInputDeviceRemoved(deviceId: Int) = refresh()
        override fun onInputDeviceChanged(deviceId: Int) = refresh()
    }

    init {
        manager.registerInputDeviceListener(listener, Handler(Looper.getMainLooper()))
        refresh()
    }

    override fun publish(event: ControllerEvent) { _events.tryEmit(event) }

    private fun refresh() {
        _controllers.value = manager.inputDeviceIds.mapNotNull { id ->
            val device = manager.getInputDevice(id) ?: return@mapNotNull null
            if (!device.isController()) return@mapNotNull null
            ControllerInfo(id, device.name ?: "Gamepad", device.vendorId, device.productId, device.sources)
        }.sortedBy { it.name }
    }
}

fun InputDevice.isController(): Boolean =
    (sources and InputDevice.SOURCE_GAMEPAD == InputDevice.SOURCE_GAMEPAD) ||
    (sources and InputDevice.SOURCE_JOYSTICK == InputDevice.SOURCE_JOYSTICK)
