package io.github.mappix.ui.editor

import android.view.KeyEvent
import android.view.MotionEvent
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mappix.R
import io.github.mappix.domain.model.InputSource
import io.github.mappix.domain.model.Mapping
import io.github.mappix.domain.model.OutputAction

private val controllerKeys = listOf(
    "A" to KeyEvent.KEYCODE_BUTTON_A, "B" to KeyEvent.KEYCODE_BUTTON_B,
    "X" to KeyEvent.KEYCODE_BUTTON_X, "Y" to KeyEvent.KEYCODE_BUTTON_Y,
    "L1" to KeyEvent.KEYCODE_BUTTON_L1, "R1" to KeyEvent.KEYCODE_BUTTON_R1,
    "L2" to KeyEvent.KEYCODE_BUTTON_L2, "R2" to KeyEvent.KEYCODE_BUTTON_R2,
    "START" to KeyEvent.KEYCODE_BUTTON_START, "SELECT" to KeyEvent.KEYCODE_BUTTON_SELECT,
    "L3" to KeyEvent.KEYCODE_BUTTON_THUMBL, "R3" to KeyEvent.KEYCODE_BUTTON_THUMBR,
    "↑" to KeyEvent.KEYCODE_DPAD_UP, "↓" to KeyEvent.KEYCODE_DPAD_DOWN,
    "←" to KeyEvent.KEYCODE_DPAD_LEFT, "→" to KeyEvent.KEYCODE_DPAD_RIGHT
)

@Composable
fun EditorScreen(id: String, vm: EditorViewModel, onBack: () -> Unit) {
    LaunchedEffect(id) { vm.open(id) }
    val profile by vm.profile.collectAsState()
    val selected by vm.selectedMapping.collectAsState()
    val awaiting by vm.awaitingSource.collectAsState()
    val error by vm.error.collectAsState()
    val current = profile
    if (current == null) {
        Text(stringResource(R.string.profile_loading), Modifier.padding(16.dp))
        return
    }
    val selectedItem = current.mappings.firstOrNull { it.id == selected }
    Column(Modifier.verticalScroll(rememberScrollState()).padding(16.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            TextButton(onClick = onBack) { Text(stringResource(R.string.back)) }
            Text(current.name, style = MaterialTheme.typography.headlineSmall)
        }
        var packageText by remember(current.id) { mutableStateOf(current.packageName.orEmpty()) }
        OutlinedTextField(
            value = packageText, onValueChange = { packageText = it },
            label = { Text(stringResource(R.string.package_name)) },
            supportingText = { Text(stringResource(R.string.package_hint)) },
            trailingIcon = { TextButton(onClick = { vm.setPackageName(packageText) }) { Text(stringResource(R.string.save)) } },
            modifier = Modifier.fillMaxWidth(), singleLine = true
        )
        Text(stringResource(R.string.select_source), style = MaterialTheme.typography.titleMedium)
        Text(stringResource(R.string.source_hint), style = MaterialTheme.typography.bodySmall)
        ControllerPicker(onChoose = { vm.add(InputSource.Button(it)) }, onStick = { vm.add(it) }, onTrigger = { vm.add(it) })
        OutlinedButton(onClick = { vm.captureNext() }) { Text(stringResource(R.string.capture_source)) }
        Text(stringResource(R.string.mapping_total, current.mappings.size), style = MaterialTheme.typography.titleMedium)
        current.mappings.forEach { mapping ->
            Card(Modifier.fillMaxWidth().clickable { vm.selectedMapping.value = mapping.id }) {
                Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(Modifier.weight(1f)) {
                            Text(sourceName(mapping.source), style = MaterialTheme.typography.titleMedium)
                            Text(actionName(mapping.output), style = MaterialTheme.typography.bodySmall)
                        }
                        Switch(checked = mapping.enabled, onCheckedChange = { vm.edit(mapping.copy(enabled = it)) })
                    }
                    if (selected == mapping.id) {
                        HorizontalDivider()
                        MappingDetails(mapping, onChange = vm::edit, onDelete = { vm.remove(mapping.id) })
                    } else TextButton(onClick = { vm.selectedMapping.value = mapping.id }) { Text(stringResource(R.string.configure)) }
                }
            }
        }
    }
    if (awaiting) AlertDialog(onDismissRequest = vm::cancelCapture,
        title = { Text(stringResource(R.string.capture_source)) },
        text = { Text(stringResource(R.string.press_controller_button)) },
        confirmButton = { TextButton(onClick = vm::cancelCapture) { Text(stringResource(R.string.cancel)) } })
    if (error != null) AlertDialog(onDismissRequest = { vm.error.value = null },
        title = { Text(stringResource(R.string.error)) }, text = { Text(error.orEmpty()) },
        confirmButton = { TextButton(onClick = { vm.error.value = null }) { Text(stringResource(R.string.ok)) } })
}

@Composable
private fun ControllerPicker(onChoose: (Int) -> Unit, onStick: (InputSource.Stick) -> Unit, onTrigger: (InputSource.Axis) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            controllerKeys.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    row.forEach { (label, code) ->
                        Box(Modifier.weight(1f).background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                            .clickable { onChoose(code) }.padding(vertical = 13.dp), contentAlignment = Alignment.Center) {
                            Text(label, style = MaterialTheme.typography.labelMedium)
                        }
                    }
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TextButton(onClick = { onStick(InputSource.Stick(MotionEvent.AXIS_X, MotionEvent.AXIS_Y)) }) { Text("LS") }
                TextButton(onClick = { onStick(InputSource.Stick(MotionEvent.AXIS_Z, MotionEvent.AXIS_RZ)) }) { Text("RS") }
                TextButton(onClick = { onTrigger(InputSource.Axis(MotionEvent.AXIS_LTRIGGER)) }) { Text("LT") }
                TextButton(onClick = { onTrigger(InputSource.Axis(MotionEvent.AXIS_RTRIGGER)) }) { Text("RT") }
            }
        }
    }
}

@Composable
private fun MappingDetails(mapping: Mapping, onChange: (Mapping) -> Unit, onDelete: () -> Unit) {
    Text(stringResource(R.string.output_action), style = MaterialTheme.typography.titleSmall)
    val choices: List<Pair<Int, OutputAction>> = if (mapping.source is InputSource.Stick)
        listOf(R.string.output_directions to OutputAction.DirectionalKeys())
    else listOf(R.string.output_tap to OutputAction.Tap(), R.string.output_button to OutputAction.Button(KeyEvent.KEYCODE_BUTTON_A))
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        choices.forEach { (label, action) ->
            val checked = mapping.output::class == action::class
            FilterChip(selected = checked, onClick = { onChange(mapping.copy(output = action)) }, label = { Text(stringResource(label)) })
        }
    }
    when (val action = mapping.output) {
        is OutputAction.Tap -> {
            Text(stringResource(R.string.coordinate_x, action.x))
            Slider(value = action.x.coerceIn(0f, 1f), onValueChange = { onChange(mapping.copy(output = action.copy(x = it))) })
            Text(stringResource(R.string.coordinate_y, action.y))
            Slider(value = action.y.coerceIn(0f, 1f), onValueChange = { onChange(mapping.copy(output = action.copy(y = it))) })
            Text(stringResource(R.string.tap_duration, action.durationMs))
            Slider(value = action.durationMs.toFloat().coerceIn(20f, 500f), valueRange = 20f..500f,
                onValueChange = { onChange(mapping.copy(output = action.copy(durationMs = it.toLong()))) })
        }
        is OutputAction.Button -> {
            var expanded by remember { mutableStateOf(false) }
            OutlinedButton(onClick = { expanded = true }) { Text(KeyEvent.keyCodeToString(action.keyCode)) }
            if (expanded) AlertDialog(onDismissRequest = { expanded = false },
                title = { Text(stringResource(R.string.choose_target)) },
                text = {
                    Column(Modifier.verticalScroll(rememberScrollState())) {
                        controllerKeys.forEach { (label, key) ->
                            TextButton(onClick = {
                                onChange(mapping.copy(output = action.copy(keyCode = key))); expanded = false
                            }) { Text("$label — ${KeyEvent.keyCodeToString(key)}") }
                        }
                    }
                }, confirmButton = { TextButton(onClick = { expanded = false }) { Text(stringResource(R.string.cancel)) } })
            Text(stringResource(R.string.button_output_warning), style = MaterialTheme.typography.bodySmall)
        }
        is OutputAction.DirectionalKeys -> Text(stringResource(R.string.stick_output_note), style = MaterialTheme.typography.bodySmall)
    }
    Text(stringResource(R.string.deadzone_percent, (mapping.deadzone * 100).toInt()))
    Slider(value = mapping.deadzone.coerceIn(0f, 0.95f), valueRange = 0f..0.75f,
        onValueChange = { onChange(mapping.copy(deadzone = it)) })
    Text(stringResource(R.string.sensitivity_value, mapping.sensitivity))
    Slider(value = mapping.sensitivity.coerceIn(0.1f, 3f), valueRange = 0.1f..3f,
        onValueChange = { onChange(mapping.copy(sensitivity = it)) })
    TextButton(onClick = onDelete) { Text(stringResource(R.string.delete_mapping)) }
}

private fun sourceName(source: InputSource): String = when (source) {
    is InputSource.Button -> KeyEvent.keyCodeToString(source.keyCode)
    is InputSource.Axis -> "Axis ${MotionEvent.axisToString(source.axis)} ${if (source.positive) "+" else "−"}"
    is InputSource.Stick -> "Stick ${MotionEvent.axisToString(source.axisX)} / ${MotionEvent.axisToString(source.axisY)}"
}

private fun actionName(output: OutputAction): String = when (output) {
    is OutputAction.Button -> "→ ${KeyEvent.keyCodeToString(output.keyCode)}"
    is OutputAction.Tap -> "→ Tap (%.2f, %.2f)".format(output.x, output.y)
    is OutputAction.DirectionalKeys -> "→ D-pad"
}
