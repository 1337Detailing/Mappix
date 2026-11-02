package io.github.mappix.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import dagger.hilt.android.AndroidEntryPoint
import io.github.mappix.MainActivity
import io.github.mappix.input.ControllerEventDispatcher
import io.github.mappix.R
import io.github.mappix.data.preferences.SettingsRepository
import io.github.mappix.domain.engine.MappingEngine
import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.model.ResolvedAction
import io.github.mappix.domain.repository.ControllerRepository
import io.github.mappix.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class MappingForegroundService : Service() {
    @Inject lateinit var profiles: ProfileRepository
    @Inject lateinit var controllers: ControllerRepository
    @Inject lateinit var settings: SettingsRepository
    @Inject lateinit var engine: MappingEngine
    @Inject lateinit var runtime: MappingRuntime
    @Inject lateinit var dispatcher: ControllerEventDispatcher
    @Inject lateinit var bridge: AccessibilityBridge
    @Inject lateinit var overlay: TouchOverlay

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var observeJob: Job? = null
    private var inputJob: Job? = null
    private var deviceJob: Job? = null

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        val channel = NotificationChannel(CHANNEL_ID, getString(R.string.mapping_channel), NotificationManager.IMPORTANCE_LOW)
        getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
            return START_NOT_STICKY
        }
        // Évite un crash si Android refuse le démarrage au premier plan.
        if (runCatching { startForeground(NOTIFICATION_ID, notification()) }.isFailure) {
            stopSelf(startId)
            return START_NOT_STICKY
        }
        if (observeJob == null) startCollectors()
        runtime.setRunning(true)
        return START_STICKY
    }

    private fun notification(): Notification {
        val open = PendingIntent.getActivity(this, 0, Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val stop = PendingIntent.getService(this, 1,
            Intent(this, MappingForegroundService::class.java).setAction(ACTION_STOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mappix)
            .setContentTitle(getString(R.string.mapping_active))
            .setContentText(getString(R.string.mapping_notification_body))
            .setContentIntent(open)
            .addAction(0, getString(R.string.stop), stop)
            .setOngoing(true)
            .build()
    }

    private fun startCollectors() {
        deviceJob = scope.launch {
            var previous = emptySet<Int>()
            controllers.controllers.collect { connected ->
                val current = connected.map { it.deviceId }.toSet()
                if ((previous - current).isNotEmpty()) {
                    (previous - current).forEach(dispatcher::resetDevice)
                    engine.reset()
                }
                previous = current
            }
        }
        observeJob = scope.launch {
            combine(profiles.observeAll(), settings.settings, runtime.foregroundPackage) { list, config, foreground ->
                val association = list.firstOrNull { !it.packageName.isNullOrBlank() && it.packageName == foreground }
                val fallback = list.firstOrNull { it.id == config.selectedProfileId }
                val chosen = association ?: fallback
                Pair(if (foreground == packageName) null else chosen, config.overlay)
            }.collectLatest { (profile, overlayEnabled) ->
                engine.reset()
                runtime.active(profile)
                if (overlayEnabled) overlay.show(profile, scope) else overlay.hide()
            }
        }
        inputJob = scope.launch {
            controllers.events.collect { event ->
                val profile: Profile = runtime.activeProfile.value ?: return@collect
                // Seules les entrées du périphérique encore connecté sont traitées.
                if (controllers.controllers.value.none { it.deviceId == event.deviceId }) return@collect
                engine.resolve(event, profile.mappings).forEach { action ->
                    runtime.emitted(action)
                    if (action is ResolvedAction.Tap && runtime.foregroundPackage.value != packageName) {
                        bridge.tap(action.x, action.y, action.durationMs)
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        overlay.hide()
        observeJob?.cancel(); inputJob?.cancel(); deviceJob?.cancel()
        engine.reset()
        runtime.active(null)
        runtime.setRunning(false)
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        const val ACTION_STOP = "io.github.mappix.action.STOP"
        private const val CHANNEL_ID = "mapping_status"
        private const val NOTIFICATION_ID = 1001
    }
}
