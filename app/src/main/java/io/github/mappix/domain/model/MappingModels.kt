package io.github.mappix.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
sealed class InputSource {
    @Serializable @SerialName("button")
    data class Button(val keyCode: Int) : InputSource()

    @Serializable @SerialName("axis")
    data class Axis(val axis: Int, val positive: Boolean = true) : InputSource()

    @Serializable @SerialName("stick")
    data class Stick(val axisX: Int, val axisY: Int) : InputSource()
}

@Serializable
sealed class OutputAction {
    @Serializable @SerialName("button")
    data class Button(val keyCode: Int) : OutputAction()

    // Coordonnées relatives à l'écran : 0.0 à 1.0.
    @Serializable @SerialName("tap")
    data class Tap(val x: Float = 0.5f, val y: Float = 0.5f, val durationMs: Long = 60) : OutputAction()

    @Serializable @SerialName("directional")
    data class DirectionalKeys(
        val up: Int = 19, val down: Int = 20,
        val left: Int = 21, val right: Int = 22
    ) : OutputAction()
}

@Serializable
data class Mapping(
    val id: String = UUID.randomUUID().toString(),
    val source: InputSource,
    val output: OutputAction,
    val enabled: Boolean = true,
    val deadzone: Float = 0.18f,
    val sensitivity: Float = 1f
)

@Serializable
data class Profile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val packageName: String? = null,
    val mappings: List<Mapping> = emptyList(),
    val updatedAt: Long = System.currentTimeMillis()
)

@Serializable
data class ProfileExport(val schemaVersion: Int = 1, val profile: Profile)

data class ControllerInfo(
    val deviceId: Int,
    val name: String,
    val vendorId: Int,
    val productId: Int,
    val sources: Int
)

sealed interface ControllerEvent {
    val deviceId: Int
    data class Button(
        override val deviceId: Int,
        val keyCode: Int,
        val pressed: Boolean
    ) : ControllerEvent
    data class Axes(
        override val deviceId: Int,
        val values: Map<Int, Float>
    ) : ControllerEvent
}

sealed interface ResolvedAction {
    data class Tap(val x: Float, val y: Float, val durationMs: Long) : ResolvedAction
    // Actions logiques : l'injection de touches système n'est pas accessible via les API publiques.
    data class VirtualButton(val keyCode: Int, val pressed: Boolean) : ResolvedAction
}
