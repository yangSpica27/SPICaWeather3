package me.spica.spicaweather3.ui.widget.particle

import android.graphics.Bitmap
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 灭霸消散效果的单个粒子
 * 
 * @param x 当前X坐标
 * @param y 当前Y坐标
 * @param originX 原始X坐标（在卡片中的位置）
 * @param originY 原始Y坐标
 * @param color 粒子颜色（从截图采样）
 * @param size 粒子大小
 * @param velocityX X方向速度
 * @param velocityY Y方向速度
 * @param alpha 透明度 (0-255)
 * @param delay 延迟时间（用于实现从边缘开始的波浪消散）
 * @param isActive 是否已激活（开始消散）
 */
data class DisintegrateParticle(
    var x: Float,
    var y: Float,
    val originX: Float,
    val originY: Float,
    val color: Int,
    var size: Float,
    var velocityX: Float,
    var velocityY: Float,
    var alpha: Int = 255,
    val delay: Float, // 0-1 的延迟因子
    var isActive: Boolean = false,
    var lifeTime: Float = 0f // 粒子存活时间
)

/**
 * 灭霸消散效果管理器
 * 将位图分解为粒子，实现从一侧开始逐渐消散的效果
 */
class ThanosDisintegrateManager {

    private val particles = mutableListOf<DisintegrateParticle>()
    private var isInitialized = false
    
    // 配置参数
    private val particleSize = 4 // 每个粒子代表的像素块大小
    private val maxLifeTime = 0.8f // 粒子最大存活时间（秒）
    
    /**
     * 从位图初始化粒子系统
     * @param bitmap 要消散的卡片截图
     * @param width 显示宽度
     * @param height 显示高度
     */
    fun initFromBitmap(bitmap: Bitmap, width: Int, height: Int) {
        particles.clear()
        
        val scaleX = bitmap.width.toFloat() / width
        val scaleY = bitmap.height.toFloat() / height
        
        // 将位图分解成粒子网格
        var y = 0
        while (y < height) {
            var x = 0
            while (x < width) {
                // 从位图采样颜色
                val sampleX = (x * scaleX).toInt().coerceIn(0, bitmap.width - 1)
                val sampleY = (y * scaleY).toInt().coerceIn(0, bitmap.height - 1)
                val pixelColor = bitmap.getPixel(sampleX, sampleY)
                
                // 跳过完全透明的像素
                if (android.graphics.Color.alpha(pixelColor) < 10) {
                    x += particleSize
                    continue
                }
                
                // 计算延迟因子：从右边开始消散（x越大，delay越小）
                // 添加一些随机性和Y方向的变化，让效果更自然
                val normalizedX = x.toFloat() / width
                val normalizedY = y.toFloat() / height
                val randomOffset = Random.nextFloat() * 0.15f
                val delay = (1f - normalizedX) + (sin(normalizedY * 3.14f) * 0.1f) + randomOffset
                
                // 创建粒子
                val particle = DisintegrateParticle(
                    x = x.toFloat(),
                    y = y.toFloat(),
                    originX = x.toFloat(),
                    originY = y.toFloat(),
                    color = pixelColor,
                    size = particleSize.toFloat() + Random.nextFloat() * 2f,
                    velocityX = 0f,
                    velocityY = 0f,
                    delay = delay.coerceIn(0f, 1f)
                )
                particles.add(particle)
                
                x += particleSize
            }
            y += particleSize
        }
        
        isInitialized = true
    }
    
    /**
     * 更新粒子状态
     * @param progress 动画进度 (0-1)
     * @param deltaTime 距离上一帧的时间（秒）
     */
    fun update(progress: Float, deltaTime: Float) {
        if (!isInitialized) return
        
        for (particle in particles) {
            // 根据进度和延迟决定粒子是否应该开始消散
            if (!particle.isActive && progress >= particle.delay * 0.7f) {
                // 激活粒子，设置随机速度
                particle.isActive = true
                
                // 向右上方飘散，带有随机性
                val angle = Random.nextFloat() * 0.8f - 0.2f // -0.2 到 0.6 弧度，主要向右
                val speed = 50f + Random.nextFloat() * 100f
                
                particle.velocityX = cos(angle) * speed
                particle.velocityY = sin(angle) * speed - 30f // 略微向上
            }
            
            // 更新已激活的粒子
            if (particle.isActive) {
                particle.lifeTime += deltaTime
                
                // 应用速度
                particle.x += particle.velocityX * deltaTime
                particle.y += particle.velocityY * deltaTime
                
                // 应用重力（轻微向下）
                particle.velocityY += 100f * deltaTime
                
                // 添加一些随机扰动（模拟灰尘飘散）
                particle.velocityX += (Random.nextFloat() - 0.5f) * 20f * deltaTime
                particle.velocityY += (Random.nextFloat() - 0.5f) * 10f * deltaTime
                
                // 阻尼
                particle.velocityX *= 0.99f
                particle.velocityY *= 0.99f
                
                // 根据存活时间减小透明度和大小
                val lifeProgress = (particle.lifeTime / maxLifeTime).coerceIn(0f, 1f)
                particle.alpha = ((1f - lifeProgress) * 255).toInt()
                particle.size = particle.size * (1f - lifeProgress * 0.3f)
            }
        }
    }
    
    /**
     * 获取所有粒子用于绘制
     */
    fun getParticles(): List<DisintegrateParticle> = particles
    
    /**
     * 获取未消散的粒子（用于绘制剩余的卡片部分）
     */
    fun getInactiveParticles(): List<DisintegrateParticle> = particles.filter { !it.isActive }
    
    /**
     * 获取已消散的粒子（用于绘制飘散的粒子）
     */
    fun getActiveParticles(): List<DisintegrateParticle> = particles.filter { it.isActive && it.alpha > 0 }
    
    /**
     * 检查消散是否完成
     */
    fun isComplete(): Boolean {
        if (!isInitialized) return false
        return particles.all { it.isActive && it.alpha <= 0 }
    }
    
    /**
     * 重置管理器
     */
    fun reset() {
        particles.clear()
        isInitialized = false
    }
}
