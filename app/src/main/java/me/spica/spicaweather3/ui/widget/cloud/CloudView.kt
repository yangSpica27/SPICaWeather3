package me.spica.spicaweather3.ui.widget.cloud

import android.content.Context
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.TextureView
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.viewinterop.AndroidView
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent

@Composable
fun CloudView(
    collapsedFraction: Float,
    show: Boolean,
) {
    ShowOnIdleContent(
        show,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = Modifier.fillMaxSize()
    ) {
        val showProgress by animateFloatAsState(
            targetValue = if (show) 1f else 0f,
            animationSpec = spring(dampingRatio = .45f, stiffness = 500f),
            label = "show_progress"
        )
        AndroidView(
            factory = { ctx -> CloudTextureView(ctx) },
            update = { view -> view.showProgress = showProgress },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -size.height / 20f * EaseInOutBounce.transform(collapsedFraction)
                }
        )
    }
}

private class CloudTextureView(context: Context) :
    TextureView(context), TextureView.SurfaceTextureListener {

    private val density = context.resources.displayMetrics.density

    @Volatile var showProgress: Float = 0f

    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private val startTime = SystemClock.uptimeMillis()

    @Volatile private var isRunning = false

    private val paint = Paint().apply { isAntiAlias = true }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return
            val frameStart = SystemClock.uptimeMillis()
            renderFrame()
            val delay = maxOf(1L, 16L - (SystemClock.uptimeMillis() - frameStart))
            renderHandler?.postDelayed(this, delay)
        }
    }

    init {
        isOpaque = false
        surfaceTextureListener = this
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        isRunning = true
        renderThread = HandlerThread("CloudRender").apply { start() }
        renderHandler = Handler(renderThread!!.looper).also { it.post(frameRunnable) }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        isRunning = false
        renderHandler?.removeCallbacksAndMessages(null)
        renderThread?.quitSafely()
        renderThread = null
        renderHandler = null
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun renderFrame() {
        val canvas = lockCanvas() ?: return
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val elapsed = SystemClock.uptimeMillis() - startTime
            val w = canvas.width.toFloat()
            val sp = showProgress
            val step = 16f * density

            val dist1 = smoothWave(elapsed, 5400L)
            val dist2 = smoothWave(elapsed, 3500L)
            val dist3 = smoothWave(elapsed, 2750L)

            // 与原实现一致：以右上角为轴心缩放（canvas.scale pivot = (w, 0)）
            canvas.save()
            canvas.scale(sp, sp, w, 0f)

            paint.color = Color.argb(0x26, 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(0f, 0f, w / 5f * sp + dist2 * step, paint)

            paint.color = Color.argb(0x80, 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(40f * density, 0f, w / 3f * sp + dist1 * step, paint)

            paint.color = Color.argb(0x26, 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(w / 2f, 0f, w / 5f * sp + dist1 * step, paint)

            paint.color = Color.argb(0x80, 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(w / 2f + 6f * density, 18f * density, w / 3f * sp + dist3 * step, paint)

            paint.color = Color.argb(0x26, 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(w, -4f * density, w / 5f * sp + dist1 * step, paint)

            paint.color = Color.argb(0x80, 0xFF, 0xFF, 0xFF)
            canvas.drawCircle(w + 6f * density, 8f * density, w / 3f * sp + dist2 * step, paint)

            canvas.restore()
        } finally {
            unlockCanvasAndPost(canvas)
        }
    }
}