package io.github.mappix.ui.home

import android.view.KeyEvent
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mappix.R

@Composable
fun HomeScreen(vm: HomeViewModel, onProfiles: () -> Unit, onTester: () -> Unit, onOnboarding: () -> Unit) {
    val controllers by vm.controllers.collectAsState()
    val running by vm.running.collectAsState()
    val active by vm.active.collectAsState()
    val profiles by vm.profiles.collectAsState()
    val failure by vm.failure.collectAsState()
    Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.home_title), style = MaterialTheme.typography.headlineMedium)
        Card(colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Icon(Icons.Rounded.SportsEsports, contentDescription = null)
                    Text(stringResource(R.string.connected_controllers, controllers.size), style = MaterialTheme.typography.titleLarge)
                }
                if (controllers.isEmpty()) Text(stringResource(R.string.no_controller))
                controllers.forEach { item ->
                    Text(item.name, style = MaterialTheme.typography.titleMedium)
                    Text("VID ${item.vendorId} · PID ${item.productId} · ID ${item.deviceId}", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = onTester) { Text(stringResource(R.string.open_tester)) }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.current_profile), style = MaterialTheme.typography.titleMedium)
                Text(active?.name ?: stringResource(R.string.none_selected))
                Text(stringResource(R.string.profiles_available, profiles.size), style = MaterialTheme.typography.bodySmall)
                OutlinedButton(onClick = onProfiles) { Text(stringResource(R.string.manage_profiles)) }
                HorizontalDivider()
                Text(if (running) stringResource(R.string.service_on) else stringResource(R.string.service_off))
                Button(onClick = { if (running) vm.stop() else vm.start() }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (running) stringResource(R.string.stop_mapping) else stringResource(R.string.start_mapping))
                }
                if (!vm.bridge.available()) {
                    Text(stringResource(R.string.accessibility_required), style = MaterialTheme.typography.bodySmall)
                    OutlinedButton(onClick = onOnboarding) { Text(stringResource(R.string.configure_permissions)) }
                }
            }
        }
        Text(stringResource(R.string.compatibility_note), style = MaterialTheme.typography.bodySmall)
    }
    if (failure != null) AlertDialog(onDismissRequest = { vm.failure.value = null },
        title = { Text(stringResource(R.string.error)) }, text = { Text(failure.orEmpty()) },
        confirmButton = { TextButton(onClick = { vm.failure.value = null }) { Text(stringResource(R.string.ok)) } })
}
