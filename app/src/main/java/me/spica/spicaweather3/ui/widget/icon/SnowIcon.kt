package me.spica.spicaweather3.ui.widget.icon

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PaintingStyle
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_100
import kotlin.math.PI
import kotlin.math.sin


@Composable
fun SnowIcon(
	modifier: Modifier = Modifier,
	size: Dp = 35.dp,
	color: Color = COLOR_WHITE_100,
) {
	val density = LocalDensity.current
	val sizePx = with(density) { size.toPx() }
	val width = sizePx
	val height = sizePx

	val specs = remember {
		listOf(
			SnowSpec(0.32f, 0.30f, 1f, 0f, 1f),
			SnowSpec(0.62f, 0.32f, 0.85f, 0.25f, 0.85f),
			SnowSpec(0.48f, 0.36f, 0.75f, 0.5f, 0.7f)
		)
	}

	val infiniteTransition = rememberInfiniteTransition(label = "snow-icon")
	val fallProgress = infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 2800, easing = LinearEasing),
		),
		label = "snow-fall"
	)

	val flakePaint = remember(color) {
		Paint().apply {
			this.color = color
			style = PaintingStyle.Stroke
			strokeCap = StrokeCap.Round
			isAntiAlias = true
		}.asFrameworkPaint()
	}

	Canvas(
		modifier = modifier
			.width(size)
			.aspectRatio(1f)
	) {
		drawIntoCanvas { canvas ->
			specs.forEach { spec ->
				val centerX = spec.x * width
				val startY = spec.y * height
				val travel = height * 0.45f
				val progress = ((fallProgress.value + spec.phase) % 1f).let { if (it < 0f) it + 1f else it }
				val easedFall = progress * progress
				val centerY = startY + travel * easedFall
				val alphaFactor = (1f - progress).coerceIn(0f, 1f)
				val sway = sin((progress + spec.phase) * 2f * PI).toFloat() * sizePx * 0.02f * spec.sway
				val armLength = sizePx * 0.16f * spec.scale
				val minorLength = armLength * 0.6f
				val stroke = sizePx * 0.052f * spec.scale

				flakePaint.strokeWidth = stroke
				flakePaint.alpha = (color.alpha * 255 * alphaFactor).toInt().coerceIn(0, 255)

				canvas.nativeCanvas.save()
				canvas.nativeCanvas.translate(centerX + sway, centerY)

				canvas.nativeCanvas.drawLine(0f, -armLength, 0f, armLength, flakePaint)
				canvas.nativeCanvas.drawLine(-armLength, 0f, armLength, 0f, flakePaint)
				canvas.nativeCanvas.rotate(45f)
				canvas.nativeCanvas.drawLine(0f, -minorLength, 0f, minorLength, flakePaint)
				canvas.nativeCanvas.drawLine(-minorLength, 0f, minorLength, 0f, flakePaint)

				canvas.nativeCanvas.restore()
			}
		}
	}
}

private data class SnowSpec(
	val x: Float,
	val y: Float,
	val scale: Float,
	val phase: Float,
	val sway: Float,
)