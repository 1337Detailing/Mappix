package io.github.mappix.ui.tester

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.FilterChip
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mappix.R

@Composable
fun TesterScreen(vm: TesterViewModel) {
    val held by vm.held.collectAsState()
    val axes by vm.axes.collectAsState()
    val devices by vm.devices.collectAsState()
    val selectedId by vm.selectedDeviceId.collectAsState()
    val history by vm.history.collectAsState()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.tester_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.tester_hint), style = MaterialTheme.typography.bodyMedium)
        if (devices.isEmpty()) Text(stringResource(R.string.no_controller))
        else devices.forEach { device ->
            FilterChip(selected = selectedId == device.deviceId,
                onClick = { vm.selectDevice(device.deviceId) },
                label = { Text(device.name) })
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(stringResource(R.string.sticks), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(28.dp)) {
                    StickCanvas(axes[MotionEvent.AXIS_X] ?: 0f, axes[MotionEvent.AXIS_Y] ?: 0f)
                    StickCanvas(axes[MotionEvent.AXIS_Z] ?: axes[MotionEvent.AXIS_RX] ?: 0f,
                        axes[MotionEvent.AXIS_RZ] ?: axes[MotionEvent.AXIS_RY] ?: 0f)
                }
                axes.toSortedMap().forEach { (axis, value) ->
                    Text("${MotionEvent.axisToString(axis)}: ${"%.3f".format(value)}", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(stringResource(R.string.pressed_buttons), style = MaterialTheme.typography.titleMedium)
                if (held.isEmpty()) Text(stringResource(R.string.no_button_pressed))
                held.sorted().forEach { Text(KeyEvent.keyCodeToString(it), color = MaterialTheme.colorScheme.primary) }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(14.dp)) {
                Row {
                    Text(stringResource(R.string.input_history),
                        style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f))
                    TextButton(onClick = vm::clearHistory) { Text(stringResource(R.string.clear_history)) }
                }
                if (history.isEmpty()) Text(stringResource(R.string.input_history_empty))
                history.take(8).forEach { Text(it, style = MaterialTheme.typography.bodySmall) }
            }
        }
        Text(stringResource(R.string.test_scope_note), style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
private fun StickCanvas(x: Float, y: Float) {
    val color = MaterialTheme.colorScheme.primary
    val background = MaterialTheme.colorScheme.outline
    Canvas(Modifier.size(130.dp)) {
        val radius = size.minDimension / 2f - 10.dp.toPx()
        val center = Offset(size.width / 2f, size.height / 2f)
        drawCircle(background, radius, center, style = Stroke(width = 3.dp.toPx()))
        drawLine(background, Offset(center.x - radius, center.y), Offset(center.x + radius, center.y), 1.dp.toPx())
        drawLine(background, Offset(center.x, center.y - radius), Offset(center.x, center.y + radius), 1.dp.toPx())
        drawCircle(color, 11.dp.toPx(), center + Offset(x.coerceIn(-1f, 1f) * radius, y.coerceIn(-1f, 1f) * radius))
    }
}
