package io.github.mappix.ui.settings

import android.provider.Settings
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Button
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mappix.R

@Composable
fun SettingsScreen(vm: SettingsViewModel, updater: UpdateViewModel, onPermissions: () -> Unit) {
    val settings by vm.settings.collectAsState()
    val context = LocalContext.current
    val updateState by updater.state.collectAsState()
    Column(Modifier.verticalScroll(rememberScrollState()).padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(stringResource(R.string.settings_title), style = MaterialTheme.typography.headlineMedium)
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Text(stringResource(R.string.dark_theme), Modifier.weight(1f))
                    Switch(checked = settings?.darkMode ?: true, onCheckedChange = vm::dark)
                }
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.overlay_switch))
                        Text(stringResource(R.string.overlay_note), style = MaterialTheme.typography.bodySmall)
                    }
                    Switch(checked = settings?.overlay ?: false,
                        onCheckedChange = { enabled ->
                            if (enabled && !Settings.canDrawOverlays(context)) onPermissions()
                            else vm.overlay(enabled)
                        })
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp)) {
                Text(stringResource(R.string.language), style = MaterialTheme.typography.titleMedium)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    FilterChip(selected = settings?.language != "en", onClick = { vm.language("fr") }, label = { Text("Français") })
                    FilterChip(selected = settings?.language == "en", onClick = { vm.language("en") }, label = { Text("English") })
                }
            }
        }
        Card(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(stringResource(R.string.update_title), style = MaterialTheme.typography.titleMedium)
                Text(stringResource(R.string.update_installed_version, updater.version))
                if (updater.configured) Text(stringResource(R.string.update_source, updater.repositoryName),
                    style = MaterialTheme.typography.bodySmall)
                else Text(stringResource(R.string.update_not_configured), style = MaterialTheme.typography.bodySmall)
                when (val state = updateState) {
                    UpdateState.Idle -> Unit
                    UpdateState.Checking -> {
                        LinearProgressIndicator(Modifier.fillMaxWidth())
                        Text(stringResource(R.string.update_checking))
                    }
                    UpdateState.UpToDate -> Text(stringResource(R.string.update_up_to_date))
                    is UpdateState.Available -> {
                        Text(stringResource(R.string.update_available, state.update.version))
                        if (state.update.notes.isNotBlank()) Text(state.update.notes.take(1500),
                            style = MaterialTheme.typography.bodySmall)
                        Button(onClick = updater::download) { Text(stringResource(R.string.update_download)) }
                    }
                    is UpdateState.Downloading -> {
                        LinearProgressIndicator(progress = { state.progress / 100f }, modifier = Modifier.fillMaxWidth())
                        Text(stringResource(R.string.update_progress, state.progress))
                    }
                    is UpdateState.Ready -> {
                        Text(stringResource(R.string.update_ready))
                        Button(onClick = updater::install) { Text(stringResource(R.string.update_install)) }
                        Text(stringResource(R.string.update_install_note), style = MaterialTheme.typography.bodySmall)
                    }
                    is UpdateState.Error -> Text(stringResource(R.string.update_error, state.message),
                        color = MaterialTheme.colorScheme.error)
                }
                OutlinedButton(onClick = updater::check, enabled = updater.configured &&
                    updateState !is UpdateState.Checking && updateState !is UpdateState.Downloading) {
                    Text(stringResource(R.string.update_check))
                }
            }
        }
        OutlinedButton(onClick = onPermissions) { Text(stringResource(R.string.configure_permissions)) }
        Text(stringResource(R.string.permissions_note), style = MaterialTheme.typography.bodySmall)
    }
}
