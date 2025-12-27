package me.spica.spicaweather3.ui.widget.sun

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.RadialGradientShader
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.scale
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent

@Composable
fun SunView2(
  collapsedFraction: Float,
  show: Boolean
) {
  val infiniteTransition = rememberInfiniteTransition(label = "sun_anim")

  // 1. 旋转动画 (慢速)
  val rotationAnim by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 360f,
    animationSpec = infiniteRepeatable(
      animation = tween(60000, easing = LinearEasing)
    ),
    label = "rotation"
  )

  // 2. 呼吸动画 (光晕)
  val pulseAnim by infiniteTransition.animateFloat(
    initialValue = 0.95f,
    targetValue = 1.05f,
    animationSpec = infiniteRepeatable(
      animation = tween(3000, easing = FastOutSlowInEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "pulse"
  )

  // 3. 射线闪烁
  val rayAlphaAnim by infiniteTransition.animateFloat(
    initialValue = 0.3f,
    targetValue = 0.7f,
    animationSpec = infiniteRepeatable(
      animation = tween(2000, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "ray_alpha"
  )

  val showAnim = animateFloatAsState(
    targetValue = if (show) 1f else 0f,
    animationSpec = spring(stiffness = Spring.StiffnessLow),
    label = "show"
  )

  ShowOnIdleContent(
    visible = show,
    modifier = Modifier.fillMaxSize()
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .drawWithCache {
          // 颜色定义
          val coreColor = Color(0xFFFFFFFF)
          val innerColor = Color(0xFFFFD54F) // Amber 300
          val outerColor = Color(0xFFFF6F00) // Amber 900
          val haloColor = Color(0x40FFECB3) // Amber 100 with alpha

          onDrawWithContent {
            if (showAnim.value <= 0f) return@onDrawWithContent

            val progress = showAnim.value
            val sunX = size.width * 0.85f
            val sunY = size.height * 0.15f - (collapsedFraction * 100.dp.toPx()) // 随折叠上移

            drawIntoCanvas { canvas ->
              // 整体缩放和透明度
              canvas.save()
              canvas.scale(progress, progress, sunX, sunY)
              // 1. 绘制大光晕 (Halo)
              val haloRadius = size.width * 0.6f * pulseAnim
              val haloPaint = Paint().apply {
                shader = RadialGradientShader(
                  center = Offset(sunX, sunY),
                  radius = haloRadius,
                  colors = listOf(haloColor, Color.Transparent),
                  tileMode = TileMode.Clamp
                )
              }
              canvas.drawCircle(Offset(sunX, sunY), haloRadius, haloPaint)

              // 2. 绘制旋转光芒 (两层)
              // 内层光芒
              canvas.save()
              canvas.translate(sunX, sunY)
              canvas.rotate(rotationAnim)

              val innerRayPaint = Paint().apply {
                shader = RadialGradientShader(
                  center = Offset.Zero,
                  radius = size.width * 0.4f,
                  colors = listOf(innerColor.copy(alpha = 0.8f), Color.Transparent)
                )
                isAntiAlias = true
              }

              val rayCount = 12
              for (i in 0 until rayCount) {
                canvas.save()
                canvas.rotate(i * (360f / rayCount))
                // 绘制
                val path = Path()
                path.moveTo(0f, -6.dp.toPx())
                path.lineTo(size.width * 0.4f, 0f)
                path.lineTo(0f, 6.dp.toPx())
                path.close()
                canvas.drawPath(path, innerRayPaint)
                canvas.restore()
              }
              canvas.restore()

              // 外层光芒 (逆向旋转，更细长)
              canvas.save()
              canvas.translate(sunX, sunY)
              canvas.rotate(-rotationAnim * 0.5f)

              val outerRayPaint = Paint().apply {
                shader = RadialGradientShader(
                  center = Offset.Zero,
                  radius = size.width * 0.5f,
                  colors = listOf(outerColor.copy(alpha = rayAlphaAnim), Color.Transparent)
                )
                isAntiAlias = true
              }

              val outerRayCount = 20
              for (i in 0 until outerRayCount) {
                canvas.save()
                canvas.rotate(i * (360f / outerRayCount) + 15f)
                val path = Path()
                path.moveTo(0f, -2.dp.toPx())
                path.lineTo(size.width * 0.5f, 0f)
                path.lineTo(0f, 2.dp.toPx())
                path.close()
                canvas.drawPath(path, outerRayPaint)
                canvas.restore()
              }
              canvas.restore()

              // 3. 绘制核心 (Core)
              val coreRadius = 40.dp.toPx()
              val corePaint = Paint().apply {
                shader = RadialGradientShader(
                  center = Offset(sunX, sunY),
                  radius = coreRadius,
                  colors = listOf(coreColor, innerColor, Color.Transparent),
                  colorStops = listOf(0.2f, 0.8f, 1f)
                )
              }
              canvas.drawCircle(Offset(sunX, sunY), coreRadius, corePaint)

              canvas.restore()
            }
          }
        }
    )
  }
}