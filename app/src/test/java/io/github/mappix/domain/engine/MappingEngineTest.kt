package io.github.mappix.domain.engine

import io.github.mappix.domain.model.ControllerEvent
import io.github.mappix.domain.model.InputSource
import io.github.mappix.domain.model.Mapping
import io.github.mappix.domain.model.OutputAction
import io.github.mappix.domain.model.ResolvedAction
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MappingEngineTest {
    @Test fun `tap only once until key released`() {
        val engine = MappingEngine()
        val mapping = Mapping(source = InputSource.Button(96), output = OutputAction.Tap(0.25f, 0.75f, 80))
        val press = ControllerEvent.Button(1, 96, true)
        assertEquals(listOf(ResolvedAction.Tap(0.25f, 0.75f, 80)), engine.resolve(press, listOf(mapping)))
        assertTrue(engine.resolve(press, listOf(mapping)).isEmpty())
        engine.resolve(ControllerEvent.Button(1, 96, false), listOf(mapping))
        assertEquals(1, engine.resolve(press, listOf(mapping)).size)
    }

    @Test fun `axis to logical button fires on transitions only`() {
        val engine = MappingEngine()
        val mapping = Mapping(source = InputSource.Axis(17), output = OutputAction.Button(97), deadzone = 0.2f)
        assertTrue(engine.resolve(ControllerEvent.Axes(1, mapOf(17 to 0.1f)), listOf(mapping)).isEmpty())
        assertEquals(listOf(ResolvedAction.VirtualButton(97, true)), engine.resolve(ControllerEvent.Axes(1, mapOf(17 to 0.8f)), listOf(mapping)))
        assertTrue(engine.resolve(ControllerEvent.Axes(1, mapOf(17 to 0.9f)), listOf(mapping)).isEmpty())
        assertEquals(listOf(ResolvedAction.VirtualButton(97, false)), engine.resolve(ControllerEvent.Axes(1, mapOf(17 to 0f)), listOf(mapping)))
    }

    @Test fun `stick deadzone and direction mapping`() {
        val engine = MappingEngine()
        val mapping = Mapping(source = InputSource.Stick(0, 1), output = OutputAction.DirectionalKeys(), deadzone = 0.15f)
        assertTrue(engine.resolve(ControllerEvent.Axes(1, mapOf(0 to 0.1f, 1 to 0f)), listOf(mapping)).isEmpty())
        assertEquals(listOf(ResolvedAction.VirtualButton(22, true)), engine.resolve(ControllerEvent.Axes(1, mapOf(0 to 1f, 1 to 0f)), listOf(mapping)))
        assertEquals(listOf(ResolvedAction.VirtualButton(22, false)), engine.resolve(ControllerEvent.Axes(1, mapOf(0 to 0f, 1 to 0f)), listOf(mapping)))
    }
}
