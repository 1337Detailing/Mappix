package io.github.mappix.domain.engine

import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.model.InputSource
import io.github.mappix.domain.model.Mapping
import io.github.mappix.domain.model.OutputAction
import io.github.mappix.domain.model.ResolvedAction
import javax.inject.Inject
import kotlin.math.abs

/** Moteur pur et testable : aucun accès au système Android. */
class MappingEngine @Inject constructor() {
    private val states = mutableMapOf<String, Boolean>()

    fun reset(): List<ResolvedAction.VirtualButton> {
        val releases = states.filterValues { it }.keys.mapNotNull { key ->
            key.substringAfterLast(':').toIntOrNull()?.let { ResolvedAction.VirtualButton(it, false) }
        }
        states.clear()
        return releases
    }

    fun resolve(event: ControllerEvent, mappings: List<Mapping>): List<ResolvedAction> = buildList {
        for (mapping in mappings) {
            if (!mapping.enabled) continue
            val source = mapping.source
            when {
                event is ControllerEvent.Button && source is InputSource.Button && source.keyCode == event.keyCode -> {
                    dispatch(mapping, event.pressed, this)
                }
                event is ControllerEvent.Axes && source is InputSource.Axis -> {
                    val raw = event.values[source.axis] ?: continue
                    val adjusted = (if (source.positive) raw else -raw).coerceAtLeast(0f)
                    dispatch(mapping, (adjusted * mapping.sensitivity.coerceIn(0.1f, 3f)) > mapping.deadzone.coerceIn(0f, 0.95f), this)
                }
                event is ControllerEvent.Axes && source is InputSource.Stick -> {
                    val output = mapping.output as? OutputAction.DirectionalKeys ?: continue
                    val x = normalized(event.values[source.axisX] ?: 0f, mapping.deadzone, mapping.sensitivity)
                    val y = normalized(event.values[source.axisY] ?: 0f, mapping.deadzone, mapping.sensitivity)
                    virtual(mapping.id, output.left, x < -0.5f, this)
                    virtual(mapping.id, output.right, x > 0.5f, this)
                    virtual(mapping.id, output.up, y < -0.5f, this)
                    virtual(mapping.id, output.down, y > 0.5f, this)
                }
            }
        }
    }

    private fun dispatch(mapping: Mapping, pressed: Boolean, actions: MutableList<ResolvedAction>) {
        when (val output = mapping.output) {
            is OutputAction.Button -> virtual(mapping.id, output.keyCode, pressed, actions)
            is OutputAction.Tap -> {
                val key = "${mapping.id}:tap"
                val wasPressed = states.put(key, pressed) ?: false
                if (pressed && !wasPressed) {
                    actions.add(ResolvedAction.Tap(output.x.coerceIn(0f, 1f), output.y.coerceIn(0f, 1f), output.durationMs.coerceIn(1L, 1000L)))
                }
            }
            is OutputAction.DirectionalKeys -> Unit
        }
    }

    private fun virtual(id: String, code: Int, pressed: Boolean, actions: MutableList<ResolvedAction>) {
        val key = "$id:$code"
        val previous = states.put(key, pressed) ?: false
        if (pressed != previous) actions.add(ResolvedAction.VirtualButton(code, pressed))
    }

    private fun normalized(value: Float, deadzone: Float, sensitivity: Float): Float {
        val dz = deadzone.coerceIn(0f, 0.95f)
        if (abs(value) <= dz) return 0f
        val scaled = ((abs(value) - dz) / (1f - dz) * sensitivity.coerceIn(0.1f, 3f)).coerceAtMost(1f)
        return if (value < 0) -scaled else scaled
    }
}
