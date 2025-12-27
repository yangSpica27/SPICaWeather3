package me.spica.spicaweather3.ui.widget.icon

import android.graphics.Matrix
import android.graphics.PathMeasure
import android.graphics.RectF
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.PI
import kotlin.math.sin


@Composable
fun WindIconView(size: Dp = 35.dp, color: Color = COLOR_WHITE_100) {

  val density = LocalDensity.current

  val sizePx = with(density) { size.toPx() }

  val windPath = remember { android.graphics.Path() }

  val leafPath = remember { android.graphics.Path() }

  val tracePath = remember { android.graphics.Path() }

  val infiniteTransition = rememberInfiniteTransition()

  val loopProgress by infiniteTransition.animateFloat(
    initialValue = 0f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(durationMillis = 3400, easing = LinearEasing),
      repeatMode = RepeatMode.Restart
    )
  )

  val pathMeasure = remember { PathMeasure() }
  val traceMeasure = remember { PathMeasure() }

  var pathLength by remember { mutableFloatStateOf(0f) }
  var traceLength by remember { mutableFloatStateOf(0f) }

  val gustSegmentPath = remember { android.graphics.Path() }
  val traceSegmentPath = remember { android.graphics.Path() }

  val gustPaint = remember(size, color, density) {
    val stroke = max(sizePx * 0.06f, with(density) { 1.6.dp.toPx() })
    Paint().apply {
      this.color = color
      style = PaintingStyle.Stroke
      strokeWidth = stroke
      strokeCap = StrokeCap.Round
      strokeJoin = StrokeJoin.Round
      isAntiAlias = true
    }.asFrameworkPaint()
  }

  val trailPaint = remember(size, color, density) {
    val stroke = max(sizePx * 0.04f, with(density) { 1.2.dp.toPx() })
    Paint().apply {
      this.color = color.copy(alpha = 0.65f)
      style = PaintingStyle.Stroke
      strokeWidth = stroke
      strokeCap = StrokeCap.Round
      strokeJoin = StrokeJoin.Round
      isAntiAlias = true
    }.asFrameworkPaint()
  }

  val leafPaint = remember(color) {
    Paint().apply {
      this.color = color
      style = PaintingStyle.Fill
      isAntiAlias = true
    }.asFrameworkPaint()
  }

  val pos = remember { FloatArray(2) }
  val tan = remember { FloatArray(2) }

  LaunchedEffect(sizePx) {
    val w = sizePx
    val h = sizePx

    // 主风轨迹：以椭圆模拟高速气流，轻微旋转营造倾斜感
    windPath.reset()
    val gustCenterX = 0.55f * w
    val gustCenterY = 0.44f * h
    val gustRadiusX = 0.38f * w
    val gustRadiusY = 0.26f * h
    val gustRect = RectF(
      gustCenterX - gustRadiusX,
      gustCenterY - gustRadiusY,
      gustCenterX + gustRadiusX,
      gustCenterY + gustRadiusY
    )
    windPath.addOval(gustRect, android.graphics.Path.Direction.CW)
    val gustMatrix = Matrix().apply {
      setRotate(-12f, gustCenterX, gustCenterY)
    }
    windPath.transform(gustMatrix)

    // 下方拖尾：更扁的椭圆，形成层次感
    tracePath.reset()
    val trailCenterX = 0.50f * w
    val trailCenterY = 0.60f * h
    val trailRadiusX = 0.32f * w
    val trailRadiusY = 0.20f * h
    val trailRect = RectF(
      trailCenterX - trailRadiusX,
      trailCenterY - trailRadiusY,
      trailCenterX + trailRadiusX,
      trailCenterY + trailRadiusY
    )
    tracePath.addOval(trailRect, android.graphics.Path.Direction.CW)
    val trailMatrix = Matrix().apply {
      setRotate(-6f, trailCenterX, trailCenterY)
    }
    tracePath.transform(trailMatrix)

    leafPath.reset()
    val leafLength = min(0.28f * w, 0.28f * h)
    val leafWidth = leafLength * 0.45f
    leafPath.moveTo(0f, -leafLength / 2)
    leafPath.quadTo(leafWidth, 0f, 0f, leafLength / 2)
    leafPath.quadTo(-leafWidth, 0f, 0f, -leafLength / 2)
    leafPath.close()

    pathMeasure.setPath(windPath, false)
    pathLength = pathMeasure.length

    traceMeasure.setPath(tracePath, false)
    traceLength = traceMeasure.length
  }


  Canvas(
    modifier = Modifier
      .width(size)
      .aspectRatio(1f),
  ) {
    drawIntoCanvas { canvas ->
      // 可复用的路径片段绘制逻辑，支持跨越首尾的流动效果
      fun drawSegment(
        totalLength: Float,
        measure: PathMeasure,
        segmentPath: android.graphics.Path,
        startFraction: Float,
        visibleFraction: Float,
        paint: android.graphics.Paint,
      ) {
        if (totalLength <= 0f) return
        val clampedStart = (startFraction % 1f).let { if (it < 0f) it + 1f else it }
        val segmentLength = totalLength * visibleFraction
        val startDistance = clampedStart * totalLength
        val endDistance = startDistance + segmentLength
        if (endDistance <= totalLength) {
          segmentPath.reset()
          measure.getSegment(startDistance, endDistance, segmentPath, true)
          canvas.nativeCanvas.drawPath(segmentPath, paint)
        } else {
          val remain = endDistance - totalLength
          segmentPath.reset()
          measure.getSegment(startDistance, totalLength, segmentPath, true)
          canvas.nativeCanvas.drawPath(segmentPath, paint)
          if (remain > 0f) {
            segmentPath.reset()
            measure.getSegment(0f, remain, segmentPath, true)
            canvas.nativeCanvas.drawPath(segmentPath, paint)
          }
        }
      }

      drawSegment(
        totalLength = pathLength,
        measure = pathMeasure,
        segmentPath = gustSegmentPath,
        startFraction = loopProgress,
        visibleFraction = 0.45f,
        paint = gustPaint,
      )

      drawSegment(
        totalLength = traceLength,
        measure = traceMeasure,
        segmentPath = traceSegmentPath,
        startFraction = (loopProgress + 0.2f) % 1f,
        visibleFraction = 0.35f,
        paint = trailPaint,
      )

      if (pathLength > 0f) {
        // 叶片进度加上正弦扰动，使速度忽快忽慢
        val leafProgress = run {
          val modulated = loopProgress + 0.1f * sin(loopProgress * (2f * PI).toFloat())
          val wrapped = modulated % 1f
          if (wrapped < 0f) wrapped + 1f else wrapped
        }
        val distance = leafProgress * pathLength
        pathMeasure.getPosTan(distance, pos, tan)
        val angle = atan2(tan[1], tan[0]) * 180f / PI
        canvas.nativeCanvas.save()
        canvas.nativeCanvas.translate(pos[0], pos[1])
        canvas.nativeCanvas.rotate(angle.toFloat())
        canvas.nativeCanvas.drawPath(leafPath, leafPaint)
        canvas.nativeCanvas.restore()
      }
    }
  }

}