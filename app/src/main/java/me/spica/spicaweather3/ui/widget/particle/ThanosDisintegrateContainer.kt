package me.spica.spicaweather3.ui.widget.particle


import android.graphics.Bitmap
import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.isActive
import android.graphics.Paint as AndroidPaint

/**
 * 灭霸消散效果容器
 * 包裹内容组件，当触发消散时会产生类似灭霸打响指的粒子消散效果
 *
 * @param isDisintegrating 是否正在消散
 * @param onDisintegrationComplete 消散完成的回调
 * @param content 要显示的内容
 */
@Composable
fun ThanosDisintegrateContainer(
    isDisintegrating: Boolean,
    onDisintegrationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    val graphicsLayer = rememberGraphicsLayer()
    var componentSize by remember { mutableStateOf(IntSize.Zero) }
    
    // 粒子管理器
    val particleManager = remember { ThanosDisintegrateManager() }
    
    // 动画进度
    val animationProgress = remember { Animatable(0f) }
    
    // 帧计数器，用于触发重绘
    var frameTick by remember { mutableIntStateOf(0) }
    
    // 上一帧时间戳
    var lastFrameTime by remember { mutableLongStateOf(0L) }
    
    // 是否已初始化粒子
    var isParticleInitialized by remember { mutableStateOf(false) }
    
    // 用于绘制粒子的Paint
    val particlePaint = remember {
        AndroidPaint().apply {
            isAntiAlias = true
            style = AndroidPaint.Style.FILL
        }
    }
    
    // 当开始消散时，捕获截图并初始化粒子
    LaunchedEffect(isDisintegrating) {
        if (isDisintegrating && componentSize.width > 0 && componentSize.height > 0) {
            // 捕获当前内容的截图
            val imageBitmap = graphicsLayer.toImageBitmap()
            val hardwareBitmap = imageBitmap.asAndroidBitmap()
            
            // 将硬件位图转换为软件位图（ARGB_8888），以便访问像素
            val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
            
            // 初始化粒子系统
            particleManager.initFromBitmap(
                softwareBitmap,
                componentSize.width,
                componentSize.height
            )
            
            // 释放复制的位图（粒子已初始化，不再需要）
            softwareBitmap.recycle()
            
            isParticleInitialized = true
            
            // 启动动画
            animationProgress.snapTo(0f)
            lastFrameTime = System.nanoTime()
            
            // 主动画循环
            while (isActive && animationProgress.value < 1f) {
                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f // 转换为秒
                lastFrameTime = currentTime
                
                // 更新进度（速度：0.7f 约1.4秒完成主动画）
                val newProgress = (animationProgress.value + deltaTime * 0.7f).coerceAtMost(1f)
                animationProgress.snapTo(newProgress)
                
                // 更新粒子物理
                particleManager.update(animationProgress.value, deltaTime)
                
                // 等待下一帧
                awaitFrame()
                frameTick++
            }
            
            // 动画完成后继续更新一小段时间让粒子飘散
            val fadeOutStart = System.nanoTime()
            while (isActive && (System.nanoTime() - fadeOutStart) < 400_000_000L) { // 0.4秒
                val currentTime = System.nanoTime()
                val deltaTime = (currentTime - lastFrameTime) / 1_000_000_000f
                lastFrameTime = currentTime
                
                particleManager.update(1f, deltaTime)
                awaitFrame()
                frameTick++
            }
            
            // 通知消散完成
            onDisintegrationComplete()
            
            // 重置状态
            isParticleInitialized = false
            particleManager.reset()
            animationProgress.snapTo(0f)
        }
    }
    
    Box(
        modifier = modifier
            .onSizeChanged { componentSize = it }
            .drawWithContent {
                if (!isDisintegrating || !isParticleInitialized) {
                    // 正常绘制内容
                    graphicsLayer.record {
                        this@drawWithContent.drawContent()
                    }
                    drawLayer(graphicsLayer)
                } else {
                    // 消散状态：绘制粒子效果
                    // 读取frameTick确保重绘
                    frameTick
                    
                    drawIntoCanvas { canvas ->
                        val nativeCanvas = canvas.nativeCanvas
                        
                        // 1. 先绘制未消散的部分（仍在原位的粒子）
                        val inactiveParticles = particleManager.getInactiveParticles()
                        for (particle in inactiveParticles) {
                            particlePaint.color = particle.color
                            particlePaint.alpha = 255
                            nativeCanvas.drawRect(
                                particle.originX,
                                particle.originY,
                                particle.originX + particle.size,
                                particle.originY + particle.size,
                                particlePaint
                            )
                        }
                        
                        // 2. 绘制正在飘散的粒子
                        val activeParticles = particleManager.getActiveParticles()
                        for (particle in activeParticles) {
                            if (particle.alpha > 0) {
                                particlePaint.color = particle.color
                                particlePaint.alpha = particle.alpha
                                
                                // 绘制圆形粒子，看起来更像灰尘
                                nativeCanvas.drawCircle(
                                    particle.x + particle.size / 2,
                                    particle.y + particle.size / 2,
                                    particle.size / 2,
                                    particlePaint
                                )
                            }
                        }
                    }
                }
            }
    ) {
        content()
    }
}
