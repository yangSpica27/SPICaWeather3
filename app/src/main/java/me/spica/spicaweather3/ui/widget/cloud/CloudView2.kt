package me.spica.spicaweather3.ui.widget.cloud

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.SurfaceTexture
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import android.view.TextureView
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
import kotlin.math.PI
import kotlin.math.sin

/**
 * 华丽的多云背景天气动画组件
 *
 * 特性：
 * - 多层云朵，制造景深效果
 * - 视差滚动动画，不同层以不同速度漂移
 * - 云朵呼吸动画（大小变化）
 * - 流畅的显示/隐藏过渡动画
 *
 * @param modifier 修饰符
 * @param show 是否显示云朵动画
 * @param collapsedFraction 折叠比例 (0-1)，用于页面滚动时的联动效果
 */
@Composable
fun CloudView2(
    modifier: Modifier = Modifier,
    show: Boolean = true,
    collapsedFraction: Float = 0f
) {
    ShowOnIdleContent(
        visible = show,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = modifier.fillMaxSize()
    ) {
        val showProgress by animateFloatAsState(
            targetValue = if (show) 1f else 0f,
            animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
            label = "show_progress"
        )
        
        // 使用 remember 保持 View 实例，避免重复创建
        val textureView = remember { CloudView2TextureView(null) }
        
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
                    // 页面折叠时产生视差效果
                    translationY = -size.height / 15f * collapsedFraction
                }
        )
    }
}

private class CloudView2TextureView(initialContext: Context?) :
    TextureView(initialContext ?: getDummyContext()), 
    TextureView.SurfaceTextureListener {

    companion object {
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

    private val paint1 = Paint().apply {
        color = Color.argb(0x18, 0xFF, 0xFF, 0xFF)
        isAntiAlias = true
    }
    private val paint2 = Paint().apply {
        color = Color.argb(0x28, 0xFF, 0xFF, 0xFF)
        isAntiAlias = true
    }
    private val paint3 = Paint().apply {
        color = Color.argb(0x40, 0xFF, 0xFF, 0xFF)
        isAntiAlias = true
    }

    private val frameRunnable = object : Runnable {
        override fun run() {
            if (!isRunning || !isSurfaceAvailable) return
            val frameStart = SystemClock.uptimeMillis()
            try {
                renderFrame()
            } catch (e: Exception) {
                // 捕获渲染异常，防止崩溃
                android.util.Log.w("CloudView2", "Render frame error", e)
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
            renderThread = HandlerThread("CloudView2Render").apply { start() }
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
            val h = canvas.height.toFloat()
            val sp = showProgress

            val drift1 = (elapsed % 12000L) / 12000f
            val drift2 = (elapsed % 8000L) / 8000f
            val drift3 = (elapsed % 5500L) / 5500f
            val breathe1 = cloudTriangleWave(elapsed, 4000L)
            val breathe2 = cloudTriangleWave(elapsed, 3200L)
            val breathe3 = cloudTriangleWave(elapsed, 2800L)

            canvas.save()
            canvas.scale(sp, sp, w, 0f)

            // ===== 第一层云（最远，最慢）=====
            val layer1Y = h * 0.08f
            val layer1DriftX = drift1 * w * 1.5f - w * 0.25f
            val layer1Breathe = 1f + sin(breathe1 * PI).toFloat() * 0.08f

            canvas.save()
            canvas.translate(layer1DriftX, layer1Y)
            canvas.scale(layer1Breathe, layer1Breathe)
            drawCloudGroup2(canvas, paint1, w / 6f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer1DriftX + w * 0.7f, layer1Y + 20f * density)
            canvas.scale(layer1Breathe * 0.9f, layer1Breathe * 0.9f)
            drawCloudGroup2(canvas, paint1, w / 7f, 0f, 0f)
            canvas.restore()

            // ===== 第二层云（中速）=====
            val layer2Y = h * 0.15f
            val layer2DriftX = drift2 * w * 1.3f - w * 0.15f
            val layer2Breathe = 1f + sin(breathe2 * PI).toFloat() * 0.1f

            canvas.save()
            canvas.translate(layer2DriftX, layer2Y)
            canvas.scale(layer2Breathe, layer2Breathe)
            drawCloudGroup2(canvas, paint2, w / 5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer2DriftX + w * 0.55f, layer2Y - 15f * density)
            canvas.scale(layer2Breathe * 1.1f, layer2Breathe * 1.1f)
            drawCloudGroup2(canvas, paint2, w / 5.5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer2DriftX - w * 0.3f, layer2Y + 30f * density)
            canvas.scale(layer2Breathe * 0.85f, layer2Breathe * 0.85f)
            drawCloudGroup2(canvas, paint2, w / 6.5f, 0f, 0f)
            canvas.restore()

            // ===== 第三层云（最近，最快）=====
            val layer3Y = h * 0.25f
            val layer3DriftX = drift3 * w * 1.2f - w * 0.1f
            val layer3Breathe = 1f + sin(breathe3 * PI).toFloat() * 0.12f

            canvas.save()
            canvas.translate(layer3DriftX, layer3Y)
            canvas.scale(layer3Breathe, layer3Breathe)
            drawCloudGroup2(canvas, paint3, w / 4.5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer3DriftX + w * 0.6f, layer3Y + 10f * density)
            canvas.scale(layer3Breathe * 0.95f, layer3Breathe * 0.95f)
            drawCloudGroup2(canvas, paint3, w / 5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer3DriftX - w * 0.25f, layer3Y - 20f * density)
            canvas.scale(layer3Breathe * 1.05f, layer3Breathe * 1.05f)
            drawCloudGroup2(canvas, paint3, w / 5.2f, 0f, 0f)
            canvas.restore()

            canvas.restore()
        } finally {
            try {
                unlockCanvasAndPost(canvas)
            } catch (e: Exception) {
                // 捕获 unlock 异常
                android.util.Log.w("CloudView2", "Unlock canvas error", e)
            }
        }
    }
}

/**
 * 三角波函数 - 用于呼吸动画的周期性变化
 */
private fun cloudTriangleWave(elapsed: Long, period: Long): Float {
    val phase = (elapsed % period).toFloat() / period.toFloat()
    return if (phase < 0.5f) phase * 2f else 2f - phase * 2f
}

/**
 * 绘制云朵组 - 由多个圆形组合而成
 */
private fun drawCloudGroup2(
    canvas: Canvas,
    paint: Paint,
    baseRadius: Float,
    offsetX: Float,
    offsetY: Float
) {
    canvas.drawCircle(offsetX, offsetY, baseRadius, paint)
    canvas.drawCircle(offsetX - baseRadius * 0.65f, offsetY + baseRadius * 0.15f, baseRadius * 0.7f, paint)
    canvas.drawCircle(offsetX + baseRadius * 0.7f, offsetY + baseRadius * 0.1f, baseRadius * 0.75f, paint)
    canvas.drawCircle(offsetX - baseRadius * 0.15f, offsetY - baseRadius * 0.5f, baseRadius * 0.6f, paint)
    canvas.drawCircle(offsetX + baseRadius * 0.35f, offsetY - baseRadius * 0.35f, baseRadius * 0.5f, paint)
    canvas.drawCircle(offsetX + baseRadius * 0.2f, offsetY + baseRadius * 0.4f, baseRadius * 0.55f, paint)
}