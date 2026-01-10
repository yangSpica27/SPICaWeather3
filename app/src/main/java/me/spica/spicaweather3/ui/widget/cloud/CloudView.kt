package me.spica.spicaweather3.ui.widget.cloud

import android.graphics.BlurMaskFilter
import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.EaseOutSine
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.presentation.theme.COLOR_BLACK_50
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent

@Composable
fun CloudView(
  collapsedFraction: Float, show: Boolean,
) {


  ShowOnIdleContent(
    show, enter = slideInVertically { -it }, exit = slideOutVertically { -it },
    modifier = Modifier.fillMaxSize()
  ) {

    val cloudPaint = remember { Paint().asFrameworkPaint() }


    val infiniteTransition = rememberInfiniteTransition(label = "infinite")


    val dist2 = infiniteTransition.animateFloat(
      0f, 1f, infiniteRepeatable(
        animation = tween(durationMillis = 1500, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      )
    )

    val dist3 = infiniteTransition.animateFloat(
      0f, 1f, infiniteRepeatable(
        animation = tween(durationMillis = 2750, easing = LinearEasing),
        repeatMode = RepeatMode.Reverse
      )
    )

    val dist1 = infiniteTransition.animateFloat(
      0f, 1f, infiniteRepeatable(
        animation = tween(durationMillis = 3400, easing = EaseOutSine),
        repeatMode = RepeatMode.Reverse
      )
    )

    val showProgress = animateFloatAsState(
      targetValue = if (show) 1f else 0f, spring(
        dampingRatio = .45f, stiffness = 500f
      )
    )

    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          translationY = -size.height / 20f * EaseInOutBounce.transform(collapsedFraction)
        }
        .drawWithCache {

          val shadowMaskFilter = BlurMaskFilter(40.dp.toPx(), BlurMaskFilter.Blur.OUTER)


          val shadowColor = COLOR_BLACK_50.toArgb()

          val cloudColor2 = Color(0x80FFFFFF).toArgb()
          val cloudColor = Color(0x26FFFFFF).toArgb()


          onDrawWithContent {

            scale(
              showProgress.value, showProgress.value, pivot = Offset(size.width, 0f)
            ) {
              drawIntoCanvas { canvas ->

                cloudPaint.color = cloudColor
                canvas.nativeCanvas.drawCircle(
                  0f, 0f,
                  size.width / 5f * showProgress.value + (dist2.value) * 16.dp.toPx(),
                  cloudPaint
                )

                cloudPaint.color = cloudColor2
                canvas.nativeCanvas.drawCircle(
                  40.dp.toPx(), 0f,
                  size.width / 3f * showProgress.value + (dist1.value) * 16.dp.toPx(),
                  cloudPaint
                )

                cloudPaint.color = cloudColor
                canvas.nativeCanvas.drawCircle(
                  size.width / 2, 0f,
                  size.width / 5f * showProgress.value + (dist1.value) * 16.dp.toPx(),
                  cloudPaint
                )

                cloudPaint.color = cloudColor2
                canvas.nativeCanvas.drawCircle(
                  size.width / 2 + 6.dp.toPx(), 18.dp.toPx(),
                  size.width / 3f * showProgress.value + (dist3.value) * 16.dp.toPx(),
                  cloudPaint
                )

                cloudPaint.color = cloudColor
                canvas.nativeCanvas.drawCircle(
                  size.width, -4.dp.toPx(),
                  size.width / 5f * showProgress.value + (dist1.value) * 16.dp.toPx(),
                  cloudPaint
                )

                cloudPaint.color = cloudColor2
                canvas.nativeCanvas.drawCircle(
                  size.width + 6.dp.toPx(), 8.dp.toPx(),
                  size.width / 3f * showProgress.value + (dist2.value) * 16.dp.toPx(),
                  cloudPaint
                )
              }
            }
          }
        })
  }
}