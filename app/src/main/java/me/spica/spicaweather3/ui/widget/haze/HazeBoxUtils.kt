package me.spica.spicaweather3.ui.widget.haze

import android.graphics.Canvas
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.toArgb
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_10
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_30
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import org.jbox2d.particle.ParticleGroup
import org.jbox2d.particle.ParticleGroupDef
import org.jbox2d.particle.ParticleType
import kotlin.math.sin
import kotlin.random.Random

/**
 * 烟雾粒子效果工具类
 * 使用 JBox2D 粒子系统实现烟雾向上飘散效果
 * 
 * @param width 绘制的UI宽度
 * @param height 绘制UI的高度
 * @param proportion 模拟世界的缩放比例
 */
class HazeBoxUtils(
  private val width: Int,
  private val height: Int,
  private val proportion: Float = 100f
) {

  var initOK = false
    private set

  private lateinit var world: World

  // 烟雾粒子组列表
  private val groups = ArrayList<ParticleGroup>()
  private val MAX_SMOKE_GROUPS = 30 // 同时存在的烟雾团数量（增加以保证连续性）

  // 粒子组生命周期（毫秒）
  private val TOTAL_LIFETIME_MS = 3000L // 3秒后重生（缩短周期保证连续）

  // 复用的绘制数组
  private var cachedFloatArray: FloatArray? = null
  
  // 风力模拟：左右重力变化
  private var windForceX = 0f
  private var windChangeCounter = 0
  private val WIND_CHANGE_INTERVAL = 60 // 每60帧改变一次风向
  
  // 用于均匀分布的网格索引
  private var nextGridIndex = 0

  fun initHazeBox() {
    synchronized(this) {
      // 重力设置为向上（负Y方向），模拟烟雾上升
      world = World(Vec2(0f, -2f)) // 向上的重力
      world.particleRadius = 15 / proportion // 较大的粒子半径，模拟烟雾团
      world.particleMaxCount = 1500 // 烟雾粒子数量（增加以支持更多烟雾团）

      // 初始化烟雾粒子组，错开生成时间
      for (i in 0 until MAX_SMOKE_GROUPS) {
        val group = createNewSmokeGroup(i)
        groups.add(group)
      }

      initOK = true
    }
  }

  /**
   * 创建新的烟雾粒子组
   * @param groupIndex 组索引，用于计算初始延迟时间
   */
  private fun createNewSmokeGroup(groupIndex: Int = -1): ParticleGroup {
    val particleGroupDef = ParticleGroupDef()
    
    // 烟雾团形状 - 使用圆形区域
    val shape = org.jbox2d.collision.shapes.CircleShape()
    val radius = width / (150f * proportion)
    shape.radius = radius
    particleGroupDef.shape = shape
    particleGroupDef.flags = ParticleType.b2_waterParticle
    
    // 设置烟雾的初始上升速度（较慢）
    val baseVelocityY = -3f // 向上漂浮
    val velocityVariation = 0.5f
    particleGroupDef.linearVelocity.set(
      (Random.nextFloat() - 0.5f) * 2f, // 轻微的水平初始速度
      baseVelocityY + (Random.nextFloat() - 0.5f) * velocityVariation
    )
    
    // 使用网格系统确保烟雾在水平方向均匀分布
    val gridColumns = 5
    val columnIndex = nextGridIndex % gridColumns
    nextGridIndex = (nextGridIndex + 1) % gridColumns
    
    // 在当前列内随机选择位置
    val columnWidth = width.toFloat() / gridColumns
    val columnOffset = columnIndex * columnWidth
    val positionInColumn = Random.nextFloat() * columnWidth
    val xPosition = (columnOffset + positionInColumn) / proportion
    
    // Y 轴位置：从屏幕底部开始
    val yPosition = if (groupIndex >= 0) {
      // 初始化时，均匀分布在不同高度
      height / proportion + (groupIndex * 0.5f)
    } else {
      // 重新激活时，从底部开始
      height / proportion + Random.nextFloat() * 0.3f
    }
    
    particleGroupDef.position.set(xPosition, yPosition)
    
    val group = world.createParticleGroup(particleGroupDef)
    
    // 设置生命周期
    val delayOffset = if (groupIndex >= 0) {
      (groupIndex * 100L) // 每组间隔 100ms（缩短间隔让烟雾快速铺满）
    } else {
      0L
    }
    group.userData = System.currentTimeMillis() + delayOffset
    
    return group
  }

  fun next() {
    if (!initOK) return
    synchronized(this) {
      val currentTime = System.currentTimeMillis()
      val groupsToReactivate = mutableListOf<ParticleGroup>()

      // 模拟风力变化
      windChangeCounter++
      if (windChangeCounter >= WIND_CHANGE_INTERVAL) {
        windChangeCounter = 0
        // 使用正弦波模拟风力的平滑变化
        val time = currentTime / 1000f
        windForceX = (sin(time * 0.5) * 3f + sin(time * 0.3) * 1.5f).toFloat()
      }
      
      // 应用风力到世界重力
      world.gravity.set(windForceX, -2f)

      // 查找需要重新激活的粒子组
      for (group in groups) {
        if (group.userData != null && currentTime - (group.userData as Long) > TOTAL_LIFETIME_MS) {
          groupsToReactivate.add(group)
        }
      }

      // 重新激活过期的粒子组
      for (group in groupsToReactivate) {
        world.destroyParticlesInGroup(group)
        group.userData = currentTime
        val newGroup = createNewSmokeGroup()
        groups[groups.indexOf(group)] = newGroup
      }

      // 执行物理世界步进
      world.step(1 / 120f, 8, 3)
    }
  }

  private val paint = Paint().apply {
    // 烟雾使用渐变效果，中心较亮，边缘透明
    isAntiAlias = true
  }.asFrameworkPaint()
  
  // 预创建不同尺寸的 shader 缓存池，避免每帧重复创建
  private val shaderCache = Array(5) { index ->
    val radius = 25f + index * 5f // 25, 30, 35, 40, 45
    RadialGradient(
      0f, 0f, radius,
      intArrayOf(
        COLOR_WHITE_30.toArgb(),
        COLOR_WHITE_10.toArgb()
      ),
      floatArrayOf(0f, 1f),
      Shader.TileMode.CLAMP
    )
  }
  
  // 缓存 Matrix 对象，用于变换 shader 位置
  private val shaderMatrix = android.graphics.Matrix()

  fun drawPoints(canvas: Canvas) {
    if (!initOK) return
    synchronized(this) {
      val positionBuffer = world.particlePositionBuffer
      val particleCount = positionBuffer.size
      
      // 为每个粒子绘制渐变圆形，模拟烟雾效果
      for (i in 0 until particleCount) {
        val vec2 = positionBuffer[i]
        val x = vec2.x * proportion
        val y = vec2.y * proportion
        if (x>1 && y >1){
          // 只绘制在屏幕范围内的粒子
          if (y > -50 && y < height + 50) {
            // 根据粒子索引选择缓存的 shader（循环使用不同大小）
            val shaderIndex = i % shaderCache.size
            val shader = shaderCache[shaderIndex]
            val radius = 25f + shaderIndex * 5f

            // 使用 Matrix 将 shader 移动到粒子位置
            shaderMatrix.reset()
            shaderMatrix.setTranslate(x, y)
            shader.setLocalMatrix(shaderMatrix)

            paint.shader = shader
            canvas.drawCircle(x, y, radius, paint)
          }
        }

      }
    }
  }
}
