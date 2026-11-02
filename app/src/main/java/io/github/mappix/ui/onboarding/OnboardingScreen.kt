package io.github.mappix.ui.onboarding

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.PowerManager
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.mappix.R
import io.github.mappix.service.MappixAccessibilityService

@Composable
fun OnboardingScreen(onDone: () -> Unit) {
    val context = LocalContext.current
    var refresh by remember { mutableIntStateOf(0) }
    val notifications = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { refresh++ }
    Column(Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Text(stringResource(R.string.onboarding_title), style = MaterialTheme.typography.headlineMedium)
        Text(stringResource(R.string.onboarding_intro))
        PermissionCard(stringResource(R.string.permission_accessibility), stringResource(R.string.permission_accessibility_note),
            if (isAccessibilityEnabled(context)) stringResource(R.string.granted) else stringResource(R.string.not_granted)) {
            openSettings(context, Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS))
        }
        PermissionCard(stringResource(R.string.permission_overlay), stringResource(R.string.permission_overlay_note),
            if (Settings.canDrawOverlays(context)) stringResource(R.string.granted) else stringResource(R.string.not_granted)) {
            openSettings(context, Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                Uri.parse("package:${context.packageName}")))
        }
        if (Build.VERSION.SDK_INT >= 33) {
            PermissionCard(stringResource(R.string.permission_notifications), stringResource(R.string.permission_notifications_note), "") {
                notifications.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
        PermissionCard(stringResource(R.string.permission_battery), stringResource(R.string.permission_battery_note), "") {
            openSettings(context, Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
        }
        Text(stringResource(R.string.permissions_optional), style = MaterialTheme.typography.bodySmall)
        Button(onClick = onDone, modifier = Modifier.fillMaxWidth()) { Text(stringResource(R.string.continue_label)) }
    }
}

@Composable
private fun PermissionCard(title: String, description: String, state: String, action: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(description, style = MaterialTheme.typography.bodySmall)
            if (state.isNotBlank()) Text(state, color = MaterialTheme.colorScheme.primary)
            OutlinedButton(onClick = action) { Text(stringResource(R.string.open_settings)) }
        }
    }
}

private fun openSettings(context: Context, intent: Intent) {
    runCatching { context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}

fun isAccessibilityEnabled(context: Context): Boolean {
    val flat = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES).orEmpty()
    val expected = ComponentName(context, MappixAccessibilityService::class.java)
    return flat.split(':').mapNotNull { ComponentName.unflattenFromString(it) }.any { it == expected }
}
