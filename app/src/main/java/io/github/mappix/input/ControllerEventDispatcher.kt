package io.github.mappix.input

import android.view.InputDevice
import android.view.KeyEvent
import android.view.MotionEvent
import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.repository.ControllerRepository
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ControllerEventDispatcher @Inject constructor(private val repository: ControllerRepository) {
    private val trackedAxes = intArrayOf(
        MotionEvent.AXIS_X, MotionEvent.AXIS_Y, MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ,
        MotionEvent.AXIS_RX, MotionEvent.AXIS_RY, MotionEvent.AXIS_LTRIGGER,
        MotionEvent.AXIS_RTRIGGER, MotionEvent.AXIS_HAT_X, MotionEvent.AXIS_HAT_Y,
        MotionEvent.AXIS_BRAKE, MotionEvent.AXIS_GAS
    )

    private val keyStates = mutableMapOf<Pair<Int, Int>, Boolean>()
    private val axisStates = mutableMapOf<Int, Map<Int, Float>>()

    @Synchronized
    fun resetDevice(deviceId: Int) {
        keyStates.keys.removeAll { it.first == deviceId }
        axisStates.remove(deviceId)
    }

    @Synchronized
    fun onKey(event: KeyEvent): Boolean {
        val device = event.device ?: return false
        if (!device.isController()) return false
        if (event.action != KeyEvent.ACTION_DOWN && event.action != KeyEvent.ACTION_UP) return false
        if (event.repeatCount > 0) return true
        val pressed = event.action == KeyEvent.ACTION_DOWN
        val key = device.id to event.keyCode
        if (keyStates[key] == pressed) return true
        keyStates[key] = pressed
        repository.publish(ControllerEvent.Button(device.id, event.keyCode, pressed))
        return true
    }

    @Synchronized
    fun onMotion(event: MotionEvent): Boolean {
        val device = event.device ?: return false
        if (!device.isController() || event.actionMasked != MotionEvent.ACTION_MOVE) return false
        val axes = buildMap {
            for (axis in trackedAxes) {
                if (device.getMotionRange(axis, event.source) != null) put(axis, event.getAxisValue(axis))
            }
        }
        if (axes.isEmpty()) return false
        val previous = axisStates[device.id]
        if (previous != null && axes.all { (axis, value) ->
                kotlin.math.abs(value - (previous[axis] ?: 0f)) < 0.012f
            }) return true
        axisStates[device.id] = axes
        repository.publish(ControllerEvent.Axes(device.id, axes))
        return true
    }
}
