package me.spica.spicaweather3.ui.widget.sun

import android.annotation.SuppressLint
import android.graphics.Path
import android.view.animation.OvershootInterpolator
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent


@SuppressLint("InvalidColorHexValue")
@Composable
fun SunView(
  collapsedFraction: Float,
  show: Boolean
) {

  val sunPaint = remember {
    Paint().asFrameworkPaint().apply {
      color = Color(0x59FFE5C9).toArgb()
      style = android.graphics.Paint.Style.FILL
    }
  }

  val infiniteTransition = rememberInfiniteTransition(label = "infinite")

  val overshootInterceptor = remember { OvershootInterpolator(1.2f) }

  val rippleAnim1 = infiniteTransition.animateFloat(
    0f, 1f, infiniteRepeatable(
      animation = tween(
        durationMillis = 3000,
        easing = Easing { overshootInterceptor.getInterpolation(it) }),
      repeatMode = RepeatMode.Reverse
    )
  )

  val rippleAnim2 = infiniteTransition.animateFloat(
    0f, 1f, infiniteRepeatable(
      animation = tween(
        durationMillis = 3400,
        easing = Easing { x ->
          overshootInterceptor.getInterpolation(x)
        }),
      repeatMode = RepeatMode.Reverse
    )
  )

  // 从里到外四层
  val path1 = remember { Path() }
  val path2 = remember { Path() }
  val path3 = remember { Path() }
  val path4 = remember { Path() }

  val showAnim = animateFloatAsState(
    targetValue = if (show) 1f else 0f,
    animationSpec = spring()
  )

  ShowOnIdleContent(
    visible = show,
    enter = slideInHorizontally { it } + slideInVertically { -it },
    exit = slideOutVertically { it } + slideOutHorizontally { it }
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .drawWithCache {

          path1.reset()
          path2.reset()
          path3.reset()
          path4.reset()

          val width = size.width


          path1.moveTo(width * 1f, 0f)
          path1.lineTo(width * 1f, width * 1f / 10f * 6f + 20.dp.toPx() * rippleAnim2.value)
          path1.cubicTo(
            width * 1f / 10f * 9f,
            width * 1f / 10f * 6f + 12.dp.toPx() * rippleAnim2.value,
            width * 1f / 10f * 4f,
            width * 1f / 10f * 5f + 10.dp.toPx() * rippleAnim2.value,
            width / 10f * 4f - 30.dp.toPx() * rippleAnim2.value,
            0f,
          )

          path2.moveTo(width * 1f, 0f)
          path2.lineTo(width * 1f, width * 1f / 10f * 7f + 25.dp.toPx() * rippleAnim2.value)
          path2.cubicTo(
            width * 1f / 10f * 8f,
            width * 1f / 10f * 7f + 20.dp.toPx() * rippleAnim2.value,
            width * 1f / 10f * 4f,
            width * 1f / 10f * 6f + 15.dp.toPx() * rippleAnim2.value,
            width / 10f * 4f - 20.dp.toPx() * rippleAnim1.value,
            0f,
          )

          path3.moveTo(width * 1f, 0f)
          path3.lineTo(width * 1f, width * 1f / 10f * 6f + 20.dp.toPx() * rippleAnim1.value)
          path3.cubicTo(
            width * 1f / 10f * 8f,
            width * 1f / 10f * 6f + 15.dp.toPx() * rippleAnim2.value,
            width * 1f / 10f * 4f,
            width * 1f / 10f * 5f + 20.dp.toPx() * rippleAnim2.value,
            width / 10f * 3f - 24.dp.toPx() * rippleAnim2.value,
            0f,
          )

          path4.moveTo(width * 1f, 0f)
          path4.lineTo(width * 1f, width * 1f / 10f * 8f + 40.dp.toPx() * rippleAnim1.value)
          path4.cubicTo(
            width * 1f / 10f * 8f,
            width * 1f / 10f * 8f + 40.dp.toPx() * rippleAnim1.value,
            width * 1f / 10f * 2f,
            width * 1f / 10f * 6f + 40.dp.toPx() * rippleAnim1.value,
            width / 10f * 2f - 40.dp.toPx() * rippleAnim1.value,
            0f,
          )

          onDrawWithContent {
            drawIntoCanvas { canvas ->
              scale(1f * showAnim.value, 1f * showAnim.value, Offset(size.width, 0f)) {
                canvas.nativeCanvas.drawPath(path1, sunPaint)
                canvas.nativeCanvas.drawPath(path2, sunPaint)
                canvas.nativeCanvas.drawPath(path3, sunPaint)
                canvas.nativeCanvas.drawPath(path4, sunPaint)
              }
            }
          }
        }
    )
  }


}