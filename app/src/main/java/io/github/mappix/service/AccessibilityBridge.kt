package io.github.mappix.service

import android.graphics.Path
import android.accessibilityservice.GestureDescription
import android.os.Handler
import android.os.Looper
import android.util.DisplayMetrics
import android.view.WindowManager
import android.content.Context
import dagger.hilt.android.qualifiers.ApplicationContext
import java.lang.ref.WeakReference
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AccessibilityBridge @Inject constructor(@ApplicationContext private val context: Context) {
    private var instance = WeakReference<MappixAccessibilityService>(null)
    private val mainHandler = Handler(Looper.getMainLooper())

    fun attach(service: MappixAccessibilityService) { instance = WeakReference(service) }
    fun detach(service: MappixAccessibilityService) {
        if (instance.get() === service) instance.clear()
    }
    fun available(): Boolean = instance.get() != null

    fun tap(nx: Float, ny: Float, durationMs: Long) {
        mainHandler.post {
            val service = instance.get() ?: return@post
            val metrics = DisplayMetrics()
            @Suppress("DEPRECATION")
            (context.getSystemService(Context.WINDOW_SERVICE) as WindowManager).defaultDisplay.getRealMetrics(metrics)
            val x = nx.coerceIn(0f, 1f) * metrics.widthPixels
            val y = ny.coerceIn(0f, 1f) * metrics.heightPixels
            val path = Path().apply { moveTo(x, y) }
            val stroke = GestureDescription.StrokeDescription(path, 0L, durationMs.coerceIn(1L, 1000L))
            service.dispatchGesture(GestureDescription.Builder().addStroke(stroke).build(), null, null)
        }
    }
}
