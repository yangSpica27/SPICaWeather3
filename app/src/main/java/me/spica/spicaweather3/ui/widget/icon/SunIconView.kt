package me.spica.spicaweather3.ui.widget.icon

import android.R.attr.rotation
import androidx.compose.animation.core.EaseOutSine
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import kotlin.math.cos
import kotlin.math.sin


@Composable
fun SunIconView(size: Dp = 35.dp, color: Color = COLOR_WHITE_100) {

  val infiniteTransition = rememberInfiniteTransition()

  val path = remember(size) { Path().asAndroidPath() }

  val path1 = remember(size) { Path().asAndroidPath() }

  val density = LocalDensity.current

  val width = remember(size) {
    with(density) {
      size.toPx()
    }
  }

  val height = remember(size) {
    with(density) {
      size.toPx()
    }
  }

  val centerX = remember(size) {
    with(density) {
      size.toPx() / 2f
    }
  }

  val centerY = remember(size) {
    with(density) {
      size.toPx() / 2f
    }
  }


  val paint = remember(color) {
    Paint().apply {
      this.color = color
      style = PaintingStyle.Stroke
      strokeWidth = with(density) { 2.dp.toPx() }
    }.asFrameworkPaint()
  }

  LaunchedEffect(size) {
    path.reset()
    path.addCircle(centerX, centerY, 0.2042f * width, android.graphics.Path.Direction.CW)
    path1.reset()
    var i = 0
    while (i < 360) {
      path1.moveTo(centerX, centerY)
      val x1 =
        ((0.3058 * width) * cos(Math.toRadians((i + rotation).toDouble())) + centerX).toFloat()
      val y1 =
        ((0.3058 * width) * sin(Math.toRadians((i + rotation).toDouble())) + centerY).toFloat()
      val X2 =
        (0.3875 * width * cos(Math.toRadians((i + rotation).toDouble())) + centerX).toFloat()
      val Y2 =
        ((0.3875 * width) * sin(Math.toRadians((i + rotation).toDouble())) + centerY).toFloat()
      path1.moveTo(x1, y1)
      path1.lineTo(X2, Y2)
      i += 45
    }
  }

  val rotationAngle = infiniteTransition.animateFloat(
    0f, 360f, infiniteRepeatable(
      animation = tween(durationMillis = 4000, easing = EaseOutSine),
      repeatMode = RepeatMode.Restart
    )
  )




  Canvas(
    modifier = Modifier
      .width(size)
      .height(size),
  ) {
    drawIntoCanvas { canvas ->
      canvas.nativeCanvas.save()
      canvas.nativeCanvas.rotate(rotationAngle.value,size.toPx()/2,size.toPx()/2)
      canvas.nativeCanvas.drawPath(path, paint)
      canvas.nativeCanvas.drawPath(path1, paint)
      canvas.nativeCanvas.restore()
    }
  }
}


@Preview
@Composable
fun Preview(modifier: Modifier = Modifier) {
  Box(
    modifier = modifier,
    contentAlignment = Alignment.Center
  ) {
    SunIconView()
  }
}