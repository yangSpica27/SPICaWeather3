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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
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
        
        // 使用 remember 保持 View 实例，避免重复创建
        val textureView = remember { CloudTextureView(null) }
        
        // 确保组件离开时清理资源
        DisposableEffect(Unit) {
            onDispose {
                textureView.cleanup()
            }
        }
        
        AndroidView(
            factory = { ctx -> 
                textureView.also { it.initialize(ctx) }
            },
            update = { view -> view.showProgress = showProgress },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -size.height / 20f * EaseInOutBounce.transform(collapsedFraction)
                }
        )
    }
}

private class CloudTextureView(initialContext: Context?) :
    TextureView(initialContext ?: getDummyContext()), 
    TextureView.SurfaceTextureListener {

    companion object {
        // 提供一个临时 context 用于初始化
        private fun getDummyContext(): Context {
            throw IllegalStateException("Context must be provided via initialize()")
        }
    }

    private var _density: Float = 2.0f
    private val density: Float get() = _density

    @Volatile var showProgress: Float = 0f

    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private var startTime = SystemClock.uptimeMillis()

    @Volatile private var isRunning = false
    @Volatile private var isSurfaceAvailable = false

    private val paint = Paint().apply { isAntiAlias = true }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRunning || !isSurfaceAvailable) return
            val frameStart = SystemClock.uptimeMillis()
            try {
                renderFrame()
            } catch (e: Exception) {
                // 捕获渲染异常，防止崩溃
                android.util.Log.w("CloudView", "Render frame error", e)
            }
            // 双重检查，确保线程安全
            if (isRunning && isSurfaceAvailable) {
                val delay = maxOf(1L, 16L - (SystemClock.uptimeMillis() - frameStart))
                renderHandler?.postDelayed(this, delay)
            }
        }
    }

    fun initialize(ctx: Context) {
        _density = ctx.resources.displayMetrics.density
        isOpaque = false
        surfaceTextureListener = this
    }

    fun cleanup() {
        isRunning = false
        isSurfaceAvailable = false
        renderHandler?.removeCallbacksAndMessages(null)
        renderThread?.let { thread ->
            thread.quitSafely()
            try { 
                thread.join(100) // 最多等待100ms避免阻塞
            } catch (_: InterruptedException) { 
                Thread.currentThread().interrupt() 
            }
        }
        renderThread = null
        renderHandler = null
    }

    override fun onSurfaceTextureAvailable(surface: SurfaceTexture, width: Int, height: Int) {
        isSurfaceAvailable = true
        startTime = SystemClock.uptimeMillis()
        
        // 防止重复创建线程
        if (renderThread == null || !renderThread!!.isAlive) {
            isRunning = true
            renderThread = HandlerThread("CloudRender").apply { start() }
            renderHandler = Handler(renderThread!!.looper)
            renderHandler?.post(frameRunnable)
        }
    }

    override fun onSurfaceTextureDestroyed(surface: SurfaceTexture): Boolean {
        isSurfaceAvailable = false
        cleanup()
        return true
    }

    override fun onSurfaceTextureSizeChanged(surface: SurfaceTexture, width: Int, height: Int) {}
    override fun onSurfaceTextureUpdated(surface: SurfaceTexture) {}

    private fun renderFrame() {
        // 三重检查确保安全
        if (!isRunning || !isSurfaceAvailable) return
        
        val canvas = try {
            lockCanvas()
        } catch (e: Exception) {
            // Surface 可能已被销毁，静默失败
            return
        } ?: return
        
        try {
            canvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            val elapsed = SystemClock.uptimeMillis() - startTime
            val w = canvas.width.toFloat()
            val sp = showProgress
            val step = 16f * density

            val dist1 = smoothCloudWave(elapsed, 5400L)
            val dist2 = smoothCloudWave(elapsed, 3500L)
            val dist3 = smoothCloudWave(elapsed, 2750L)

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
            try {
                unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                // 捕获 unlock 异常
                android.util.Log.w("CloudView", "Unlock canvas error", e)
            }
        }
    }
}

/**
 * 平滑波动函数 - 用于云朵大小的周期性变化
 */
private fun smoothCloudWave(elapsed: Long, period: Long): Float {
    val phase = (elapsed % period).toFloat() / period.toFloat()
    return kotlin.math.sin(phase * 2f * kotlin.math.PI.toFloat()).toFloat()
}