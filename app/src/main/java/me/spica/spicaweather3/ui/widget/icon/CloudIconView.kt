package me.spica.spicaweather3.ui.widget.icon

import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_100


@Composable
fun CloudIconView(
	modifier: Modifier = Modifier,
	size: Dp = 35.dp,
	color: Color = COLOR_WHITE_100,
) {

	val density = LocalDensity.current
	val sizePx = with(density) { size.toPx() }
	val width = sizePx
	val height = sizePx

	val rearCloudPath = remember { android.graphics.Path() }
	val frontCloudPath = remember { android.graphics.Path() }

	val infiniteTransition = rememberInfiniteTransition(label = "cloud-shift")

	val rearShift = infiniteTransition.animateFloat(
		initialValue = -0.02f,
		targetValue = 0.02f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 3200, easing = EaseInOutQuad),
			repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
		),
		label = "rear-shift"
	)

	val frontShift = infiniteTransition.animateFloat(
		initialValue = 0.03f,
		targetValue = -0.03f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 2600, easing = EaseInOutQuad),
			repeatMode = androidx.compose.animation.core.RepeatMode.Reverse
		),
		label = "front-shift"
	)

	val rearPaint = remember(color) {
		Paint().apply {
			this.color = color.copy(alpha = 0.65f)
			style = PaintingStyle.Fill
			isAntiAlias = true
		}.asFrameworkPaint()
	}

	val frontPaint = remember(color) {
		Paint().apply {
			this.color = color
			style = PaintingStyle.Fill
			isAntiAlias = true
		}.asFrameworkPaint()
	}

	LaunchedEffect(sizePx) {
		// 后景云朵：顶部多层弧线，底部保持平直
		rearCloudPath.reset()
		rearCloudPath.moveTo(0.20f * width, 0.60f * height)
		rearCloudPath.cubicTo(0.16f * width, 0.46f * height, 0.34f * width, 0.32f * height, 0.42f * width, 0.40f * height)
		rearCloudPath.cubicTo(0.48f * width, 0.24f * height, 0.70f * width, 0.24f * height, 0.76f * width, 0.40f * height)
		rearCloudPath.cubicTo(0.88f * width, 0.38f * height, 0.96f * width, 0.50f * height, 0.88f * width, 0.60f * height)
		rearCloudPath.cubicTo(0.94f * width, 0.66f * height, 0.88f * width, 0.74f * height, 0.78f * width, 0.73f * height)
		rearCloudPath.cubicTo(0.60f * width, 0.78f * height, 0.38f * width, 0.78f * height, 0.28f * width, 0.70f * height)
		rearCloudPath.cubicTo(0.18f * width, 0.68f * height, 0.16f * width, 0.64f * height, 0.20f * width, 0.60f * height)
		rearCloudPath.close()

		// 前景云朵：更紧凑，位于图标下方
		frontCloudPath.reset()
		frontCloudPath.moveTo(0.28f * width, 0.66f * height)
		frontCloudPath.cubicTo(0.20f * width, 0.56f * height, 0.34f * width, 0.46f * height, 0.40f * width, 0.52f * height)
		frontCloudPath.cubicTo(0.46f * width, 0.42f * height, 0.66f * width, 0.42f * height, 0.70f * width, 0.56f * height)
		frontCloudPath.cubicTo(0.80f * width, 0.56f * height, 0.84f * width, 0.68f * height, 0.74f * width, 0.74f * height)
		frontCloudPath.cubicTo(0.70f * width, 0.80f * height, 0.28f * width, 0.80f * height, 0.28f * width, 0.66f * height)
		frontCloudPath.close()
	}

	Canvas(
		modifier = modifier
			.width(size)
			.aspectRatio(1f)
	) {
		drawIntoCanvas { canvas ->
			val rearOffsetX = rearShift.value * width
			val frontOffsetX = frontShift.value * width

			canvas.nativeCanvas.save()
			canvas.nativeCanvas.translate(rearOffsetX, 0f)
			canvas.nativeCanvas.drawPath(rearCloudPath, rearPaint)
			canvas.nativeCanvas.restore()

			canvas.nativeCanvas.save()
			canvas.nativeCanvas.translate(frontOffsetX, 0f)
			canvas.nativeCanvas.drawPath(frontCloudPath, frontPaint)
			canvas.nativeCanvas.restore()
		}
	}
}