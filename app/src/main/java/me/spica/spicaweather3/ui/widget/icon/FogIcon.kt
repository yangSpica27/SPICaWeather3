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


@Composable
fun FogIcon(

	size: Dp = 35.dp,
	color: Color = COLOR_WHITE_100,
) {

	val density = LocalDensity.current
	val sizePx = with(density) { size.toPx() }
	val width = sizePx
	val height = sizePx

	val bands = 4
	val bandHeight = height * 0.12f
	val spacing = bandHeight * 0.35f

	val infiniteTransition = rememberInfiniteTransition(label = "fog-sway")

	val drift = infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 3200, easing = LinearEasing),
		),
		label = "fog-drift"
	)

	val bandPaint = remember(color) {
		Paint().apply {
			this.color = color
			style = PaintingStyle.Stroke
			strokeCap = StrokeCap.Round
			strokeWidth = bandHeight * 0.55f
			isAntiAlias = true
		}.asFrameworkPaint()
	}

	Canvas(
		modifier = Modifier
			.width(size)
			.aspectRatio(1f)
	) {
		drawIntoCanvas { canvas ->
			val baseAngle = (drift.value * Math.PI * 2).toFloat()
			val sway = kotlin.math.sin(baseAngle) * width * 0.08f
			val alphaShift = 0.85f + 0.15f * ((kotlin.math.sin(baseAngle) + 1f) * 0.5f)

			repeat(bands) { index ->
				val y = (height * 0.3f) + index * (bandHeight + spacing)
				val phase = index * 0.25f
				val phaseAngle = ((drift.value + phase) * Math.PI * 2).toFloat()
				val offset = sway * (1f - 0.15f * index) * kotlin.math.sin(phaseAngle)
				bandPaint.alpha = (color.alpha * 255 * (0.8f - index * 0.1f) * alphaShift).toInt().coerceIn(0, 255)
				canvas.nativeCanvas.drawLine(
					0.15f * width + offset,
					y,
					0.85f * width + offset,
					y,
					bandPaint
				)
			}
		}
	}
}