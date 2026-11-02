package io.github.mappix.service

import android.accessibilityservice.AccessibilityService
import android.view.KeyEvent
import android.view.accessibility.AccessibilityEvent
import dagger.hilt.android.AndroidEntryPoint
import io.github.mappix.input.ControllerEventDispatcher
import javax.inject.Inject

@AndroidEntryPoint
class MappixAccessibilityService : AccessibilityService() {
    @Inject lateinit var bridge: AccessibilityBridge
    @Inject lateinit var dispatcher: ControllerEventDispatcher
    @Inject lateinit var runtime: MappingRuntime

    override fun onServiceConnected() {
        super.onServiceConnected()
        bridge.attach(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED && event.packageName != null) {
            runtime.foreground(event.packageName.toString())
        }
    }

    override fun onKeyEvent(event: KeyEvent): Boolean {
        // On observe sans consommer : le jeu garde ses entrées natives.
        dispatcher.onKey(event)
        return false
    }

    override fun onInterrupt() = Unit
    override fun onDestroy() {
        bridge.detach(this)
        super.onDestroy()
    }
}
