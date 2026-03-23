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
import androidx.compose.runtime.getValue
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
        AndroidView(
            factory = { ctx -> CloudView2TextureView(ctx) },
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

private class CloudView2TextureView(context: Context) :
    TextureView(context), TextureView.SurfaceTextureListener {

    private val density = context.resources.displayMetrics.density

    @Volatile var showProgress: Float = 0f

    private var renderThread: HandlerThread? = null
    private var renderHandler: Handler? = null
    private val startTime = SystemClock.uptimeMillis()

    @Volatile private var isRunning = false

    private val paint1 = Paint().apply {
        color = Color.argb(0x18, 0xFF, 0xFF, 0xFF) // 最远层，透明度最低
        isAntiAlias = true
    }
    private val paint2 = Paint().apply {
        color = Color.argb(0x28, 0xFF, 0xFF, 0xFF) // 中间层
        isAntiAlias = true
    }
    private val paint3 = Paint().apply {
        color = Color.argb(0x40, 0xFF, 0xFF, 0xFF) // 最近层，透明度最高
        isAntiAlias = true
    }

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
        renderThread = HandlerThread("CloudView2Render").apply { start() }
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
            val h = canvas.height.toFloat()
            val sp = showProgress

            val drift1 = (elapsed % 12000L) / 12000f
            val drift2 = (elapsed % 8000L) / 8000f
            val drift3 = (elapsed % 5500L) / 5500f
            val breathe1 = triangleWave(elapsed, 4000L)
            val breathe2 = triangleWave(elapsed, 3200L)
            val breathe3 = triangleWave(elapsed, 2800L)

            // 以右上角为轴心缩放（与原 scale(sp, sp, pivot=Offset(width,0)) 一致）
            canvas.save()
            canvas.scale(sp, sp, w, 0f)

            // ===== 第一层云（最远，最慢）=====
            val layer1Y = h * 0.08f
            val layer1DriftX = drift1 * w * 1.5f - w * 0.25f
            val layer1Breathe = 1f + sin(breathe1 * PI.toFloat()) * 0.08f

            canvas.save()
            canvas.translate(layer1DriftX, layer1Y)
            canvas.scale(layer1Breathe, layer1Breathe)
            drawCloudGroup(canvas, paint1, w / 6f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer1DriftX + w * 0.7f, layer1Y + 20f * density)
            canvas.scale(layer1Breathe * 0.9f, layer1Breathe * 0.9f)
            drawCloudGroup(canvas, paint1, w / 7f, 0f, 0f)
            canvas.restore()

            // ===== 第二层云（中速）=====
            val layer2Y = h * 0.15f
            val layer2DriftX = drift2 * w * 1.3f - w * 0.15f
            val layer2Breathe = 1f + sin(breathe2 * PI.toFloat()) * 0.1f

            canvas.save()
            canvas.translate(layer2DriftX, layer2Y)
            canvas.scale(layer2Breathe, layer2Breathe)
            drawCloudGroup(canvas, paint2, w / 5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer2DriftX + w * 0.55f, layer2Y - 15f * density)
            canvas.scale(layer2Breathe * 1.1f, layer2Breathe * 1.1f)
            drawCloudGroup(canvas, paint2, w / 5.5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer2DriftX - w * 0.3f, layer2Y + 30f * density)
            canvas.scale(layer2Breathe * 0.85f, layer2Breathe * 0.85f)
            drawCloudGroup(canvas, paint2, w / 6.5f, 0f, 0f)
            canvas.restore()

            // ===== 第三层云（最近，最快）=====
            val layer3Y = h * 0.25f
            val layer3DriftX = drift3 * w * 1.2f - w * 0.1f
            val layer3Breathe = 1f + sin(breathe3 * PI.toFloat()) * 0.12f

            canvas.save()
            canvas.translate(layer3DriftX, layer3Y)
            canvas.scale(layer3Breathe, layer3Breathe)
            drawCloudGroup(canvas, paint3, w / 4.5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer3DriftX + w * 0.6f, layer3Y + 10f * density)
            canvas.scale(layer3Breathe * 0.95f, layer3Breathe * 0.95f)
            drawCloudGroup(canvas, paint3, w / 5f, 0f, 0f)
            canvas.restore()

            canvas.save()
            canvas.translate(layer3DriftX - w * 0.25f, layer3Y - 20f * density)
            canvas.scale(layer3Breathe * 1.05f, layer3Breathe * 1.05f)
            drawCloudGroup(canvas, paint3, w / 5.2f, 0f, 0f)
            canvas.restore()

            canvas.restore() // 还原 showProgress scale
        } finally {
            unlockCanvasAndPost(canvas)
        }
    }
}

private fun drawCloudGroup(
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