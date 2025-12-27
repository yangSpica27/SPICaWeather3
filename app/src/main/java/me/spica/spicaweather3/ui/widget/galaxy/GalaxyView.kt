package me.spica.spicaweather3.ui.widget.galaxy

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import kotlin.math.pow

@Composable
fun GalaxyView(
  collapsedFraction: Float,
  show: Boolean
) {
  BoxWithConstraints(
    modifier = Modifier
      .fillMaxSize()
      .clip(RoundedCornerShape(0))
  ) {
    val random = remember { kotlin.random.Random.Default }

    val stars = remember { mutableListOf<Star>() }

    val viewWidth = with(LocalDensity.current) {
      maxWidth.toPx()
    }


    val viewHeight = with(LocalDensity.current) {
      maxHeight.toPx()
    }

    val translateY = remember { androidx.compose.animation.core.Animatable(0f) }

    LaunchedEffect(
      collapsedFraction
    ) {

      translateY.animateTo(
        targetValue = -viewHeight / 5 * collapsedFraction,
        animationSpec = tween(durationMillis = 550)
      )
    }

    SideEffect {
      if (stars.isNotEmpty()) return@SideEffect
      val canvasSize =
        (viewWidth.toDouble().pow(2.0) + viewHeight.toDouble().pow(2.0)).pow(0.5).toInt()
      val width = (1.0 * canvasSize).toInt()
      val height = ((canvasSize - viewHeight) * 0.5 + viewWidth * 1.1111).toInt()
      val radius = (0.00125 * canvasSize * (0.5 + random.nextFloat())).toFloat()

      val colors = intArrayOf(
        Color(210, 247, 255).toArgb(),
        Color(208, 233, 255).toArgb(),
        Color(175, 201, 228).toArgb(),
        Color(164, 194, 220).toArgb(),
        Color(97, 171, 220).toArgb(),
        Color(74, 141, 193).toArgb(),
        Color(0xff820014).toArgb(),
        Color(240, 220, 151).toArgb(),
        Color(0xFFfffbe6).toArgb(),
        Color(236, 234, 213).toArgb(),
      )
      stars.addAll(Array(70) { i ->
        val x = (random.nextInt(width) - 0.5 * (canvasSize - viewWidth)).toInt()
        val y = (random.nextInt(height) - 0.5 * (canvasSize - viewHeight)).toInt()
        val duration = (2500 + random.nextFloat() * 2500).toLong()
        Star(
          x.toFloat(),
          y.toFloat(),
          radius,
          colors[i % colors.size],
          duration
        )
      })
    }


    // 使用 Canvas 进行绘制
    ShowOnIdleContent(
      visible = show,
      enter = fadeIn() + slideInVertically { it },
      exit = slideOutVertically { it } + fadeOut()
    ) {

      var frameTick by remember { mutableLongStateOf(0L) }

      LaunchedEffect(Unit) {
        // 无限循环，只要 Composable 存在，它就一直运行
        val startTime = withFrameMillis { frameTimeMillis -> frameTimeMillis }
        launch(Dispatchers.Default) {
          while (isActive) {
            // withFrameNanos 是实现每帧回调的关键
            // 它会在下一帧准备好时执行代码块
            withFrameMillis { frameTimeMillis ->
              // 遍历列表并更新每个星星的状态
              stars.forEach { it.shine((frameTimeMillis - startTime)) }
              frameTick = frameTimeMillis
            }
            delay(16)
            awaitFrame()
          }
        }
      }

      Box(
        modifier =
          Modifier
            .fillMaxSize()
            .drawWithCache {
              val paint = Paint()
              val tick = frameTick
              onDrawWithContent {
                drawIntoCanvas { canvas ->
                  stars.forEach { star ->
                    paint.color = Color(star.color)
                    paint.alpha = star.alpha
                    canvas.nativeCanvas.drawCircle(
                      star.centerX,
                      star.centerY,
                      star.radius,
                      paint.asFrameworkPaint()
                    )
                  }
                }
              }
            }
      )
    }


    DisposableEffect(Unit) {
      onDispose {
        stars.clear() // 清空列表
      }
    }

  }
}