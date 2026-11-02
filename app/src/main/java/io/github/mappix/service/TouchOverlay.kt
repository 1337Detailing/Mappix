package io.github.mappix.service

import android.content.Context
import android.graphics.Color
import android.graphics.PixelFormat
import android.graphics.drawable.GradientDrawable
import android.provider.Settings
import android.view.Gravity
import android.view.MotionEvent
import android.view.WindowManager
import android.widget.TextView
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.mappix.domain.model.Mapping
import io.github.mappix.domain.model.OutputAction
import io.github.mappix.domain.model.Profile
import io.github.mappix.domain.repository.ProfileRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/** Marqueurs tactiles déplaçables, visibles uniquement si l'utilisateur l'autorise. */
@Singleton
class TouchOverlay @Inject constructor(
    @ApplicationContext private val context: Context,
    private val repository: ProfileRepository
) {
    private val manager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private val markers = mutableListOf<TextView>()
    private var lastProfile: Profile? = null

    fun show(profile: Profile?, scope: CoroutineScope) {
        hide()
        if (profile == null || !Settings.canDrawOverlays(context)) return
        lastProfile = profile
        val size = (52 * context.resources.displayMetrics.density).toInt()
        val screen = context.resources.displayMetrics
        profile.mappings.filter { it.enabled && it.output is OutputAction.Tap }.forEachIndexed { index, mapping ->
            val tap = mapping.output as OutputAction.Tap
            val marker = TextView(context).apply {
                text = "${index + 1}"
                textSize = 17f
                gravity = Gravity.CENTER
                setTextColor(Color.WHITE)
                background = GradientDrawable().apply {
                    shape = GradientDrawable.OVAL
                    setColor(0xB3714DDE.toInt())
                    setStroke(2, Color.WHITE)
                }
                elevation = 8f
            }
            val params = WindowManager.LayoutParams(
                size, size, WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY,
                WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
                PixelFormat.TRANSLUCENT
            ).apply {
                gravity = Gravity.TOP or Gravity.START
                x = (tap.x.coerceIn(0f, 1f) * screen.widthPixels).toInt() - size / 2
                y = (tap.y.coerceIn(0f, 1f) * screen.heightPixels).toInt() - size / 2
            }
            var downX = 0f
            var downY = 0f
            var initialX = 0
            var initialY = 0
            marker.setOnTouchListener { _, event ->
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        downX = event.rawX; downY = event.rawY
                        initialX = params.x; initialY = params.y
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        params.x = (initialX + event.rawX - downX).toInt()
                        params.y = (initialY + event.rawY - downY).toInt()
                        runCatching { manager.updateViewLayout(marker, params) }
                        true
                    }
                    MotionEvent.ACTION_UP -> {
                        val nx = ((params.x + size / 2f) / screen.widthPixels).coerceIn(0f, 1f)
                        val ny = ((params.y + size / 2f) / screen.heightPixels).coerceIn(0f, 1f)
                        scope.launch {
                            val latest = repository.get(profile.id) ?: return@launch
                            repository.save(latest.copy(mappings = latest.mappings.map { m ->
                                if (m.id == mapping.id) m.copy(output = (m.output as? OutputAction.Tap)?.copy(x = nx, y = ny) ?: m.output)
                                else m
                            }))
                        }
                        true
                    }
                    else -> false
                }
            }
            if (runCatching { manager.addView(marker, params) }.isSuccess) markers.add(marker)
        }
    }

    fun hide() {
        markers.forEach { runCatching { manager.removeView(it) } }
        markers.clear()
        lastProfile = null
    }
}
