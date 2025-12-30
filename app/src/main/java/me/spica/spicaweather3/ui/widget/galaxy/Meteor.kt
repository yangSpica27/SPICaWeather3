package me.spica.spicaweather3.ui.widget.galaxy

import androidx.annotation.ColorInt
import kotlin.math.cos
import kotlin.math.sin
import kotlin.random.Random

/**
 * 流星类
 * @param startX 起始 X 坐标
 * @param startY 起始 Y 坐标
 * @param length 流星尾巴长度
 * @param angle 流星运动角度（弧度）
 * @param speed 流星速度
 * @param color 流星颜色
 * @param duration 流星持续时间（毫秒）
 */
class Meteor(
  private val startX: Float,
  private val startY: Float,
  private val length: Float,
  private val angle: Float,
  private val speed: Float,
  @ColorInt val color: Int,
  val duration: Long
) {
  var currentX: Float = startX
  var currentY: Float = startY
  var alpha: Float = 0f
  var isActive: Boolean = true
  private var startTime: Long = -1L

  /**
   * 更新流星位置和透明度
   * @param currentTime 当前时间（毫秒）
   */
  fun update(currentTime: Long) {
    if (startTime == -1L) {
      startTime = currentTime
    }

    val elapsed = currentTime - startTime
    if (elapsed > duration) {
      isActive = false
      return
    }

    // 计算进度 (0 到 1)
    val progress = elapsed.toFloat() / duration

    // 位置更新：沿着角度方向移动
    val distance = speed * elapsed / 16f // 基于 60fps 计算
    currentX = startX + distance * cos(angle)
    currentY = startY + distance * sin(angle)

    // 透明度：淡入淡出效果
    alpha = when {
      progress < 0.2f -> progress / 0.2f // 前 20% 淡入
      progress > 0.8f -> (1f - progress) / 0.2f // 后 20% 淡出
      else -> 1f // 中间 60% 保持满透明度
    }
  }

  /**
   * 获取尾巴终点坐标
   */
  fun getTailX(): Float = currentX - length * cos(angle)
  fun getTailY(): Float = currentY - length * sin(angle)

  companion object {
    /**
         * 创建随机流星
         */
    fun createRandom(
      viewWidth: Float,
      viewHeight: Float,
      random: Random = Random.Default
    ): Meteor {
      // 从屏幕上方和右侧随机生成
      val fromTop = random.nextBoolean()
      val startX = if (fromTop) random.nextFloat() * viewWidth else viewWidth
      val startY = if (fromTop) -50f else random.nextFloat() * viewHeight * 0.3f

      // 流星角度：向右下方倾斜 30-60 度
      val angle = Math.toRadians((30 + random.nextFloat() * 30).toDouble()).toFloat() + Math.PI.toFloat() / 4

      // 流星属性
      val length = 40f + random.nextFloat() * 60f // 尾巴长度 40-100
      val speed = 8f + random.nextFloat() * 6f // 速度 8-14
      val duration = 800L + random.nextLong(700L) // 持续 0.8-1.5 秒

      // 流星颜色：白色到淡蓝色
      val colors = intArrayOf(
        0xFFFFFFFF.toInt(), // 纯白
        0xFFE6F5FF.toInt(), // 淡蓝白
        0xFFD0E8FF.toInt(), // 浅蓝白
        0xFFF0F8FF.toInt(), // 爱丽丝蓝
      )
      val color = colors[random.nextInt(colors.size)]

      return Meteor(startX, startY, length, angle, speed, color, duration)
    }
  }
}
