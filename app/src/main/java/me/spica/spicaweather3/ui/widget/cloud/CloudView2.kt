package me.spica.spicaweather3.ui.widget.cloud

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import kotlin.math.sin

/**
 * 华丽的多云背景天气动画组件
 *
 * 特性：
 * - 多层云朵，制造景深效果
 * - 视差滚动动画，不同层以不同速度漂移
 * - 模糊效果增加真实感
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
    BoxWithConstraints(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          // 页面折叠时产生视差效果
          translationY = -size.height / 15f * collapsedFraction
        }
    ) {
      val infiniteTransition = rememberInfiniteTransition(label = "cloud_animation")

      // 第一层云（最慢，最远）- 水平漂移动画
      val drift1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 12000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart
        ),
        label = "drift1"
      )

      // 第二层云（中速）- 水平漂移动画
      val drift2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 8000, easing = LinearEasing),
          repeatMode = RepeatMode.Restart
        ),
        label = "drift2"
      )

      // 第三层云（最快，最近）- 水平漂移动画
      val drift3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 5500, easing = LinearEasing),
          repeatMode = RepeatMode.Restart
        ),
        label = "drift3"
      )

      // 云朵呼吸动画 - 第一层
      val breathe1 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 4000, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "breathe1"
      )

      // 云朵呼吸动画 - 第二层
      val breathe2 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 3200, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "breathe2"
      )

      // 云朵呼吸动画 - 第三层
      val breathe3 by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
          animation = tween(durationMillis = 2800, easing = LinearEasing),
          repeatMode = RepeatMode.Reverse
        ),
        label = "breathe3"
      )

      // 显示/隐藏动画
      val showProgress by animateFloatAsState(
        targetValue = if (show) 1f else 0f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = 300f),
        label = "show_progress"
      )

      val density = LocalDensity.current

      // 创建不同层次的云朵画笔，带有模糊效果
      val cloudPaint1 = remember {
        Paint().asFrameworkPaint().apply {
          color = Color(0x18FFFFFF).toArgb() // 最远层，透明度最低
//          maskFilter = BlurMaskFilter(with(density) { 50.dp.toPx() }, BlurMaskFilter.Blur.NORMAL)
          isAntiAlias = true
        }
      }

      val cloudPaint2 = remember {
        Paint().asFrameworkPaint().apply {
          color = Color(0x28FFFFFF).toArgb() // 中间层
//          maskFilter = BlurMaskFilter(with(density) { 35.dp.toPx() }, BlurMaskFilter.Blur.NORMAL)
          isAntiAlias = true
        }
      }

      val cloudPaint3 = remember {
        Paint().asFrameworkPaint().apply {
          color = Color(0x40FFFFFF).toArgb() // 最近层，透明度最高
//          maskFilter = BlurMaskFilter(with(density) { 25.dp.toPx() }, BlurMaskFilter.Blur.NORMAL)
          isAntiAlias = true
        }
      }

      Box(
        modifier = Modifier
          .fillMaxSize()
          .drawWithCache {
            val width = size.width
            val height = size.height



            onDrawWithContent {
              scale(showProgress, showProgress, pivot = Offset(width, 0f)) {
                drawIntoCanvas { canvas ->
                  val nCanvas = canvas.nativeCanvas

                  // ===== 第一层云（最远，最慢）=====
                  val layer1Y = height * 0.08f
                  val layer1DriftX = (drift1 * width * 1.5f - width * 0.25f)
                  val layer1Breathe = 1f + sin(breathe1 * Math.PI.toFloat()) * 0.08f

                  nCanvas.save()
                  nCanvas.translate(layer1DriftX, layer1Y)
                  nCanvas.scale(layer1Breathe, layer1Breathe)

                  // 绘制云朵组（由多个圆形组成）
                  drawCloudGroup(
                    nCanvas,
                    cloudPaint1,
                    baseRadius = width / 6f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // 第一层的第二朵云（位置错开）
                  nCanvas.save()
                  nCanvas.translate(layer1DriftX + width * 0.7f, layer1Y + 20.dp.toPx())
                  nCanvas.scale(layer1Breathe * 0.9f, layer1Breathe * 0.9f)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint1,
                    baseRadius = width / 7f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // ===== 第二层云（中速）=====
                  val layer2Y = height * 0.15f
                  val layer2DriftX = (drift2 * width * 1.3f - width * 0.15f)
                  val layer2Breathe = 1f + sin(breathe2 * Math.PI.toFloat()) * 0.1f

                  nCanvas.save()
                  nCanvas.translate(layer2DriftX, layer2Y)
                  nCanvas.scale(layer2Breathe, layer2Breathe)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint2,
                    baseRadius = width / 5f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // 第二层的第二朵云
                  nCanvas.save()
                  nCanvas.translate(layer2DriftX + width * 0.55f, layer2Y - 15.dp.toPx())
                  nCanvas.scale(layer2Breathe * 1.1f, layer2Breathe * 1.1f)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint2,
                    baseRadius = width / 5.5f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // 第二层的第三朵云
                  nCanvas.save()
                  nCanvas.translate(layer2DriftX - width * 0.3f, layer2Y + 30.dp.toPx())
                  nCanvas.scale(layer2Breathe * 0.85f, layer2Breathe * 0.85f)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint2,
                    baseRadius = width / 6.5f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // ===== 第三层云（最近，最快）=====
                  val layer3Y = height * 0.25f
                  val layer3DriftX = (drift3 * width * 1.2f - width * 0.1f)
                  val layer3Breathe = 1f + sin(breathe3 * Math.PI.toFloat()) * 0.12f

                  nCanvas.save()
                  nCanvas.translate(layer3DriftX, layer3Y)
                  nCanvas.scale(layer3Breathe, layer3Breathe)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint3,
                    baseRadius = width / 4.5f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // 第三层的第二朵云
                  nCanvas.save()
                  nCanvas.translate(layer3DriftX + width * 0.6f, layer3Y + 10.dp.toPx())
                  nCanvas.scale(layer3Breathe * 0.95f, layer3Breathe * 0.95f)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint3,
                    baseRadius = width / 5f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()

                  // 第三层的第三朵云
                  nCanvas.save()
                  nCanvas.translate(layer3DriftX - width * 0.25f, layer3Y - 20.dp.toPx())
                  nCanvas.scale(layer3Breathe * 1.05f, layer3Breathe * 1.05f)

                  drawCloudGroup(
                    nCanvas,
                    cloudPaint3,
                    baseRadius = width / 5.2f,
                    offsetX = 0f,
                    offsetY = 0f
                  )

                  nCanvas.restore()
                }
              }
            }
          }
      )
    }
  }
}

/**
 * 绘制一组云朵（由多个圆形组成，模拟真实云朵的形态）
 */
private fun drawCloudGroup(
  canvas: android.graphics.Canvas,
  paint: android.graphics.Paint,
  baseRadius: Float,
  offsetX: Float,
  offsetY: Float
) {
  // 中心主圆
  canvas.drawCircle(offsetX, offsetY, baseRadius, paint)

  // 左侧圆
  canvas.drawCircle(
    offsetX - baseRadius * 0.65f,
    offsetY + baseRadius * 0.15f,
    baseRadius * 0.7f,
    paint
  )

  // 右侧圆
  canvas.drawCircle(
    offsetX + baseRadius * 0.7f,
    offsetY + baseRadius * 0.1f,
    baseRadius * 0.75f,
    paint
  )

  // 上方圆（制造蓬松感）
  canvas.drawCircle(
    offsetX - baseRadius * 0.15f,
    offsetY - baseRadius * 0.5f,
    baseRadius * 0.6f,
    paint
  )

  // 右上方小圆
  canvas.drawCircle(
    offsetX + baseRadius * 0.35f,
    offsetY - baseRadius * 0.35f,
    baseRadius * 0.5f,
    paint
  )

  // 底部小圆（增加层次）
  canvas.drawCircle(
    offsetX + baseRadius * 0.2f,
    offsetY + baseRadius * 0.4f,
    baseRadius * 0.55f,
    paint
  )
}