package me.spica.spicaweather3.ui.widget.snow

import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import org.jbox2d.collision.shapes.CircleShape
import org.jbox2d.common.Vec2
import org.jbox2d.dynamics.World
import org.jbox2d.particle.ParticleGroup
import org.jbox2d.particle.ParticleGroupDef
import org.jbox2d.particle.ParticleType
import java.util.concurrent.Executors
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 华丽的下雪效果 View - 基于 JBox2D 物理引擎
 * 
 * 特性：
 * - 多层次雪花（近景、中景、远景）
 * - 雪花旋转动画
 * - 真实的风力模拟（正弦波叠加）
 * - 粒子大小和透明度变化
 * - 发光模糊效果
 * - 雪花飘落轨迹随机化
 */
@Composable
fun SnowView(show: Boolean = true) {
  ShowOnIdleContent(visible = show, modifier = Modifier.fillMaxSize()) {
    BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
      val snowSystem = remember {
        SnowParticleSystem(
          width = constraints.maxWidth,
          height = constraints.maxHeight
        )
      }

      var frameTime by remember { mutableLongStateOf(0L) }

      val computeContext = remember { 
        Executors.newFixedThreadPool(1).asCoroutineDispatcher() 
      }

      DisposableEffect(Unit) {
        onDispose { computeContext.close() }
      }

      LaunchedEffect(Unit) {
        snowSystem.initialize()
        launch(computeContext) {
          while (isActive) {
            snowSystem.update()
            awaitFrame()
            frameTime = System.currentTimeMillis()
          }
        }
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .drawBehind {
            frameTime // 触发重绘
            drawIntoCanvas { canvas ->
              snowSystem.render(canvas.nativeCanvas)
            }
          }
      )
    }
  }
}

/**
 * 雪花粒子数据类
 */
private data class Snowflake(
  var x: Float,
  var y: Float,
  val size: Float,
  val speed: Float,
  var rotation: Float,
  val rotationSpeed: Float,
  val alpha: Float,
  val layer: Int // 0=远景, 1=中景, 2=近景
)

/**
 * 雪花粒子系统
 */
private class SnowParticleSystem(
  private val width: Int,
  private val height: Int,
  private val proportion: Float = 80f
) {
  private lateinit var world: World
  private val particleGroups = ArrayList<ParticleGroup>()
  private val snowflakes = ArrayList<Snowflake>()
  
  // 配置参数
  private val maxParticleGroups = 40
  private val maxSnowflakes = 120
  private val particleLifetime = 6000L
  
  // 风力模拟
  private var windForceX = 0f
  private var windTick = 0
  private val windChangeInterval = 40
  
  // 三层雪花的绘制画笔
  private val paintLayers = Array(3) { layer ->
    Paint().apply { 
      isAntiAlias = true
      when (layer) {
        0 -> alpha = 0.3f // 远景 - 半透明
        1 -> alpha = 0.6f // 中景
        2 -> alpha = 0.9f // 近景 - 最清晰
      }
    }.asFrameworkPaint().apply {
      // 添加轻微模糊效果
      when (layer) {
        0 -> maskFilter = BlurMaskFilter(3f, BlurMaskFilter.Blur.NORMAL)
        1 -> maskFilter = BlurMaskFilter(2f, BlurMaskFilter.Blur.NORMAL)
        2 -> maskFilter = BlurMaskFilter(1.5f, BlurMaskFilter.Blur.NORMAL)
      }
    }
  }
  
  fun initialize() {
    synchronized(this) {
      // 创建物理世界，重力向下
      world = World(Vec2(0f, 5.5f))
      world.particleRadius = 5f / proportion
      world.particleMaxCount = 1500
      world.particleDamping = 0.5f // 增加阻尼，减少反弹
      world.particleDensity = 1.2f // 增加密度，更容易下落
      
      // 初始化 JBox2D 粒子组（用于物理模拟）
      repeat(maxParticleGroups) { index ->
        particleGroups.add(createParticleGroup(index))
      }
      
      // 初始化额外的装饰性雪花（不参与物理模拟，纯视觉效果）
      repeat(maxSnowflakes) { index ->
        snowflakes.add(createDecorativeSnowflake(index))
      }
    }
  }
  
  /**
   * 创建 JBox2D 粒子组
   */
  private fun createParticleGroup(index: Int = -1): ParticleGroup {
    val def = ParticleGroupDef()
    val shape = CircleShape()
    
    // 随机大小的雪花
    val baseRadius = 4f + Random.nextFloat() * 8f
    shape.radius = baseRadius / proportion
    def.shape = shape
    // 使用粉末粒子类型，不会相互碰撞反弹
    def.flags = ParticleType.b2_powderParticle
    
    // 初始位置：顶部随机分布
    val x = Random.nextFloat() * width / proportion
    val y = if (index >= 0) {
      // 初始化时错开高度，从顶部到屏幕中间区域
      -index * 0.3f * (height / proportion)
    } else {
      // 重生时从顶部开始
      -(Random.nextFloat() * 2f)  * (height / proportion)
    }
    def.position.set(x, y)
    
    // 初始速度：确保向下落 + 随机水平漂移
    val horizontalDrift = (Random.nextFloat() - 0.5f) * 1.5f
    val fallSpeed = 5f + Random.nextFloat() * 3f // 增加下落速度
    def.linearVelocity.set(horizontalDrift, fallSpeed)
    
    // 设置速度上限，防止异常加速
    def.linearVelocity.y = def.linearVelocity.y.coerceAtMost(3f)
    
    val group = world.createParticleGroup(def)
    group.userData = System.currentTimeMillis() + (if (index >= 0) index * 150L else 0L)
    
    return group
  }
  
  /**
   * 创建装饰性雪花（纯视觉）
   */
  private fun createDecorativeSnowflake(index: Int = -1): Snowflake {
    val layer = Random.nextInt(3) // 随机分配到三层
    val size = when (layer) {
      0 -> 2f + Random.nextFloat() * 3f  // 远景 - 小
      1 -> 4f + Random.nextFloat() * 4f  // 中景
      else -> 6f + Random.nextFloat() * 6f // 近景 - 大
    }
    
    val speed = when (layer) {
      0 -> 0.3f + Random.nextFloat() * 0.3f  // 远景 - 慢
      1 -> 0.6f + Random.nextFloat() * 0.5f  // 中景
      else -> 1f + Random.nextFloat() * 0.8f   // 近景 - 快
    }
    
    val x = Random.nextFloat() * width
    val y = if (index >= 0) {
      -(index * 10f) // 初始化时错开，从顶部开始
    } else {
      -(Random.nextFloat() * 100f) // 重生时从顶部
    }
    
    return Snowflake(
      x = x,
      y = y,
      size = size,
      speed = speed,
      rotation = Random.nextFloat() * 360f,
      rotationSpeed = (Random.nextFloat() - 0.5f) * 2f,
      alpha = 0.4f + Random.nextFloat() * 0.6f,
      layer = layer
    )
  }
  
  fun update() {
    synchronized(this) {
      val now = System.currentTimeMillis()
      
      // 更新风力（双正弦波叠加，模拟真实风）
      windTick++
      if (windTick >= windChangeInterval) {
        windTick = 0
        val time = now / 1000f
        // 减小风力强度，防止粒子被吹飞
        windForceX = (sin(time * 0.6) * 1.5f + 
                     sin(time * 0.28) * 0.8f + 
                     cos(time * 0.15) * 0.5f).toFloat()
      }
      // 确保重力始终向下，Y轴为正
      world.gravity = Vec2(windForceX, 5.5f)
      
      // 更新并回收过期的 JBox2D 粒子组
      val expiredGroups = mutableListOf<ParticleGroup>()
      for (group in particleGroups) {
        val birthTime = group.userData as Long
        if (now - birthTime > particleLifetime) {
          expiredGroups.add(group)
        }
      }
      expiredGroups.forEach { oldGroup ->
        world.destroyParticlesInGroup(oldGroup)
        val newGroup = createParticleGroup()
        val index = particleGroups.indexOf(oldGroup)
        particleGroups[index] = newGroup
      }
      
      // 更新物理世界
      world.step(1f / 120f, 8, 3)
      
      // 更新装饰性雪花
      snowflakes.forEachIndexed { index, flake ->
        // 下落运动
        flake.y += flake.speed
        
        // 水平飘移（受风影响）
        val windEffect = windForceX * (flake.layer + 1) * 0.15f
        flake.x += windEffect + sin((now / 1000f + index) * 0.8f) * 0.3f
        
        // 旋转
        flake.rotation += flake.rotationSpeed
        
        // 回收超出屏幕的雪花
        if (flake.y > height + 50 || flake.x < -50 || flake.x > width + 50) {
          val newFlake = createDecorativeSnowflake()
          snowflakes[index] = newFlake
        }
      }
    }
  }
  
  fun render(canvas: Canvas) {
    if (!::world.isInitialized)return
    synchronized(this) {
      // 先绘制装饰性雪花（按层次从远到近）
      for (layer in 0..2) {
        val paint = paintLayers[layer]
        paint.color = Color.White.toArgb()
        
        snowflakes.filter { it.layer == layer }.forEach { flake ->
          canvas.save()
          canvas.translate(flake.x, flake.y)
          canvas.rotate(flake.rotation)
          
          // 绘制雪花（六角形或圆形）
          val adjustedAlpha = (flake.alpha * 255).toInt()
          paint.alpha = adjustedAlpha
          
          if (flake.size > 5f) {
            // 大雪花绘制为六角形
            drawHexagonSnowflake(canvas, paint, flake.size)
          } else {
            // 小雪花绘制为圆形
            canvas.drawCircle(0f, 0f, flake.size, paint)
          }
          
          canvas.restore()
        }
      }
      
      // 绘制 JBox2D 物理粒子
      val positionBuffer = world.particlePositionBuffer
      val count = positionBuffer.size
      
      for (i in 0 until count) {
        val pos = positionBuffer[i]
        val screenX = pos.x * proportion
        val screenY = (pos.y * proportion)
        
        // 边界检查
        if (screenX < -20 || screenX > width + 20 || screenY < -40 || screenY > height + 40) {
          continue
        }
        
        // 根据粒子索引选择层次
        val layer = i % 3
        val paint = paintLayers[layer]
        paint.color = Color.White.toArgb()
        
        // 大小随层次变化
        val size = when (layer) {
          0 -> 3f + (i % 3) * 0.5f
          1 -> 4f + (i % 4) * 0.5f
          else -> 5f + (i % 5) * 0.5f
        }
        
        // 透明度轻微变化
        val alpha = (160 + (i % 60)).coerceIn(100, 255)
        paint.alpha = alpha
        
        canvas.drawCircle(screenX, screenY, size, paint)
      }
    }
  }
  
  /**
   * 绘制六角形雪花
   */
  private fun drawHexagonSnowflake(canvas: Canvas, paint: android.graphics.Paint, size: Float) {
    val path = android.graphics.Path()
    for (i in 0..5) {
      val angle = Math.toRadians((i * 60).toDouble())
      val x = (size * cos(angle)).toFloat()
      val y = (size * sin(angle)).toFloat()
      if (i == 0) {
        path.moveTo(x, y)
      } else {
        path.lineTo(x, y)
      }
    }
    path.close()
    canvas.drawPath(path, paint)
    
    // 绘制内部结构线条
    paint.strokeWidth = 1f
    for (i in 0..5) {
      val angle = Math.toRadians((i * 60).toDouble())
      val x = (size * 0.6f * cos(angle)).toFloat()
      val y = (size * 0.6f * sin(angle)).toFloat()
      canvas.drawLine(0f, 0f, x, y, paint)
    }
  }
}