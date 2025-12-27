package me.spica.spicaweather3.ui.widget.rain

import android.graphics.Canvas
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import org.jbox2d.collision.shapes.PolygonShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.Body
import org.jbox2d.dynamics.BodyDef
import org.jbox2d.dynamics.BodyType
import org.jbox2d.dynamics.FixtureDef
import org.jbox2d.dynamics.World
import org.jbox2d.particle.ParticleGroup
import org.jbox2d.particle.ParticleGroupDef
import org.jbox2d.particle.ParticleType
import kotlin.random.Random

/**
 * 绘制雨点碰撞到地面的
 * Jbox2D坐标工具类
 * 雨点从-height出不断落下
 * 有个宽度为width的刚体在底部
 * 雨点和刚体不断碰撞
 * 每个雨点的尺寸为世界宽度的1/30 高度的1/20
 * @author SPICa27
 * @param width 绘制的UI宽度
 * @param height 绘制UI的高度
 * @param proportion 模拟世界的缩放比例
 */
class RainBoxUtils(
  private val width: Int,
  private val height: Int,
  private val proportion: Float = 300f
) {

  var initOK = false
    private set

  private lateinit var world: World

  private lateinit var groundBody: Body

  // 我们自己维护的粒子组列表，用于循环利用
  private val groups = ArrayList<ParticleGroup>()
  private val MAX_ACTIVE_RAIN_GROUPS = 35 // 同时活跃的最大雨点组数量

  // 粒子组从创建/激活到最终销毁的总生命周期（毫秒）
  private val TOTAL_LIFETIME_MS = 4000

  // 复用的 FloatArray，避免每帧分配新数组
  private var cachedFloatArray: FloatArray? = null
  
  // 用于均匀分布的网格索引
  private var nextGridIndex = 0


  fun initRainBox() {
    synchronized(this) {
      world = World(Vec2(0f, 15f)) // 增加重力加速度，让雨点下落更快
      world.particleRadius = 6 / proportion // 全局粒子交互半径
      world.particleMaxCount = 1000 // 最大粒子数量

      // 创建地板
      val groundBodyDef = BodyDef()
      groundBodyDef.type = BodyType.STATIC
      groundBodyDef.position.set(0f, height / proportion * 2) // 将地板放置在屏幕下方
      groundBody = world.createBody(groundBodyDef)
      val fixtureDef = FixtureDef()
      val box = PolygonShape()
      box.setAsBox(width * 1f / proportion, height / proportion) // 地板宽度，高度
      fixtureDef.shape = box
      fixtureDef.friction = 10f // 摩擦系数
      fixtureDef.restitution = 0.3f // 补偿系数
      fixtureDef.filter.maskBits = 0b01
      fixtureDef.filter.groupIndex = 0b01
      groundBody.createFixture(fixtureDef)

      // 初始化固定数量的雨点组，错开生成时间实现更自然的节奏
      for (i in 0 until MAX_ACTIVE_RAIN_GROUPS) {
        val group = createNewRainGroup(i)
        groups.add(group)
      }

      initOK = true
    }
  }

  /**
   * 创建新的雨点组，使用网格分布算法确保雨点均匀分布
   * @param groupIndex 组索引，用于计算初始延迟时间
   */
  private fun createNewRainGroup(groupIndex: Int = -1): ParticleGroup {
    val particleGroupDef = ParticleGroupDef()
    val shape = PolygonShape()
    // 设置雨点组的初始生成形状和大小
    shape.setAsBox(width / (300f * proportion), width / (25f * proportion))
    particleGroupDef.shape = shape
    particleGroupDef.flags = ParticleType.b2_waterParticle
    
    // 速度更加统一，只在一个较小范围内随机，让下落节奏更一致
    val baseVelocity = 18f // 提升基础速度，加快下落
    val velocityVariation = baseVelocity * 0.15f // 只允许 ±15% 的速度变化
    particleGroupDef.linearVelocity.set(
      0f, 
      baseVelocity + (Random.nextFloat() - 0.5f) * 2 * velocityVariation
    )
    
    // 使用网格系统确保雨点在水平方向均匀分布
    val gridColumns = 5 // 将屏幕水平分为5列
    val columnIndex = nextGridIndex % gridColumns
    nextGridIndex = (nextGridIndex + 1) % gridColumns
    
    // 在当前列内随机选择位置，保证整体分布均匀
    val columnWidth = width.toFloat() / gridColumns
    val columnOffset = columnIndex * columnWidth
    val positionInColumn = Random.nextFloat() * columnWidth
    val xPosition = (columnOffset + positionInColumn) / proportion
    
    // Y 轴位置：根据组索引错开初始高度，让雨点逐步进入而不是同时出现
    val yPosition = if (groupIndex >= 0) {
      // 初始化时，将雨点均匀分布在上方不同高度
      -height / proportion * (0.5f + groupIndex * 0.15f)
    } else {
      // 重新激活时，从屏幕上方随机高度开始
      -height / proportion * (0.3f + Random.nextFloat() * 0.4f)
    }
    
    particleGroupDef.position.set(xPosition, yPosition)
    
    val group = world.createParticleGroup(particleGroupDef)
    
    // 设置生命周期：初始化时错开创建时间，实现渐进式出现
    val delayOffset = if (groupIndex >= 0) {
      (groupIndex * 120L) // 每组间隔 120ms
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

      // 遍历我们自己维护的粒子组列表，找出需要重新激活的组
      for (group in groups) {
        // 判断组是否过期 (userData 存储了创建或上次激活时间)
        if (group.userData != null && currentTime - (group.userData as Long) > TOTAL_LIFETIME_MS) {
          groupsToReactivate.add(group)
        }
      }

      // 实际重新激活这些粒子组
      for (group in groupsToReactivate) {
        // 销毁组内的所有粒子 (组对象本身还在)
        world.destroyParticlesInGroup(group)
        // 重新设置 userData，表示该组现在是新的/活跃的
        group.userData = currentTime
        val newGroup = createNewRainGroup()
        groups[groups.indexOf(group)] = newGroup // 替换旧组为新组
      }

      // 执行 JBox2D 物理世界的步进，更新所有粒子位置
      world.step(1 / 120f, 10, 3) // 时间步长, 速度迭代, 位置迭代
    }
  }

  private val paint = Paint()
    .apply {
      color = COLOR_WHITE_100
      strokeWidth = 12f // 雨点绘制宽度
      strokeCap = StrokeCap.Round
    }
    .asFrameworkPaint()


  fun drawPoints(canvas: Canvas) {
    if (!initOK) return
    synchronized(this) {
      val positionBuffer = world.particlePositionBuffer
      val particleCount = positionBuffer.size
      
      // 复用或创建 FloatArray，避免每帧分配新内存
      val arraySize = particleCount * 2
      val floatArray = if (cachedFloatArray?.size == arraySize) {
        cachedFloatArray!!
      } else {
        FloatArray(arraySize).also { cachedFloatArray = it }
      }
      
      // 将 JBox2D 世界坐标转换为 UI 屏幕坐标
      for (i in 0 until particleCount) {
        val vec2 = positionBuffer[i]
        floatArray[i * 2] = vec2.x * proportion
        floatArray[i * 2 + 1] = vec2.y * proportion
      }
      
      // 使用 drawPoints 绘制所有粒子
      canvas.drawPoints(floatArray, paint)
    }
  }
}