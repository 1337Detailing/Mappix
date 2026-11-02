package io.github.mappix.ui.profiles

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mappix.R
import io.github.mappix.domain.model.Profile

@Composable
fun ProfilesScreen(vm: ProfilesViewModel, onEdit: (String) -> Unit) {
    val profiles by vm.profiles.collectAsState()
    val settings by vm.settings.collectAsState()
    val error by vm.message.collectAsState()
    val context = LocalContext.current
    var renameTarget by remember { mutableStateOf<Profile?>(null) }
    var createDialog by remember { mutableStateOf(false) }
    var name by remember { mutableStateOf("") }
    var exportProfile by remember { mutableStateOf<Profile?>(null) }
    var deleteTarget by remember { mutableStateOf<Profile?>(null) }
    var search by remember { mutableStateOf("") }
    val visibleProfiles = remember(profiles, search) {
        profiles.filter { search.isBlank() || it.name.contains(search, ignoreCase = true) ||
            it.packageName.orEmpty().contains(search, ignoreCase = true) }
    }

    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
        if (uri != null) runCatching {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { it.readText() }
        }.onSuccess { if (it != null) vm.import(it) }.onFailure { vm.message.value = it.message }
    }
    val exportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        val profile = exportProfile
        if (uri != null && profile != null) runCatching {
            context.contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { it.write(vm.export(profile)) }
        }.onFailure { vm.message.value = it.message }
    }

    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(stringResource(R.string.profiles_title), style = MaterialTheme.typography.headlineMedium)
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            Button(onClick = { name = ""; createDialog = true }) {
                Icon(Icons.Rounded.Add, null)
                Text(stringResource(R.string.create_profile))
            }
            OutlinedButton(onClick = { importLauncher.launch(arrayOf("application/json", "text/plain", "*/*")) }) {
                Text(stringResource(R.string.import_profile))
            }
        }
        OutlinedTextField(value = search, onValueChange = { search = it.take(100) },
            label = { Text(stringResource(R.string.profile_search)) },
            singleLine = true, modifier = Modifier.fillMaxWidth())
        if (profiles.isEmpty()) Text(stringResource(R.string.empty_profiles))
        else if (visibleProfiles.isEmpty()) Text(stringResource(R.string.profile_no_matches))
        LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            items(visibleProfiles, key = { it.id }) { profile ->
                Card(Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row {
                            RadioButton(selected = settings?.selectedProfileId == profile.id, onClick = { vm.select(profile.id) })
                            Column {
                                Text(profile.name, style = MaterialTheme.typography.titleMedium)
                                Text(profile.packageName ?: stringResource(R.string.any_app), style = MaterialTheme.typography.bodySmall)
                                Text(stringResource(R.string.mappings_count, profile.mappings.size), style = MaterialTheme.typography.bodySmall)
                            }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { onEdit(profile.id) }) { Text(stringResource(R.string.edit)) }
                            TextButton(onClick = { name = profile.name; renameTarget = profile }) { Text(stringResource(R.string.rename)) }
                            TextButton(onClick = { vm.duplicate(profile.id) }) { Text(stringResource(R.string.duplicate)) }
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TextButton(onClick = { exportProfile = profile; exportLauncher.launch("mappix-${profile.name}.json") }) { Text(stringResource(R.string.export_profile)) }
                            TextButton(onClick = { deleteTarget = profile }) { Text(stringResource(R.string.delete)) }
                        }
                    }
                }
            }
        }
    }
    if (createDialog || renameTarget != null) {
        val old = renameTarget
        AlertDialog(onDismissRequest = { createDialog = false; renameTarget = null },
            title = { Text(stringResource(if (old == null) R.string.create_profile else R.string.rename)) },
            text = { OutlinedTextField(value = name, onValueChange = { name = it.take(80) }, label = { Text(stringResource(R.string.profile_name)) }) },
            confirmButton = {
                TextButton(enabled = name.isNotBlank(), onClick = {
                    if (old == null) vm.create(name, onEdit) else vm.rename(old, name)
                    createDialog = false; renameTarget = null
                }) { Text(stringResource(R.string.save)) }
            }, dismissButton = { TextButton(onClick = { createDialog = false; renameTarget = null }) { Text(stringResource(R.string.cancel)) } }
        )
    }
    deleteTarget?.let { target ->
        AlertDialog(onDismissRequest = { deleteTarget = null },
            title = { Text(stringResource(R.string.profile_delete_title)) },
            text = { Text(stringResource(R.string.profile_delete_confirmation, target.name)) },
            confirmButton = { TextButton(onClick = { vm.delete(target.id); deleteTarget = null }) {
                Text(stringResource(R.string.delete))
            } },
            dismissButton = { TextButton(onClick = { deleteTarget = null }) { Text(stringResource(R.string.cancel)) } })
    }
    if (error != null) AlertDialog(onDismissRequest = vm::clearMessage, title = { Text(stringResource(R.string.error)) },
        text = { Text(error.orEmpty()) }, confirmButton = { TextButton(onClick = vm::clearMessage) { Text(stringResource(R.string.ok)) } })
}
