package me.spica.spicaweather3.ui.widget.icon

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
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
import kotlin.math.PI
import kotlin.math.sin


@Composable
fun RainIcon(
	modifier: Modifier = Modifier,
	size: Dp = 35.dp,
	color: Color = COLOR_WHITE_100,
) {

	val density = LocalDensity.current
	val sizePx = with(density) { size.toPx() }
	val width = sizePx
	val height = sizePx

	val dropPath = remember { android.graphics.Path() }

	val infiniteTransition = rememberInfiniteTransition()

	val fallProgress = infiniteTransition.animateFloat(
		initialValue = 0f,
		targetValue = 1f,
		animationSpec = infiniteRepeatable(
			animation = tween(durationMillis = 1200, easing = LinearEasing),
			repeatMode = RepeatMode.Restart
		)
	)

	val dropPaint = remember(color) {
		Paint().apply {
			this.color = color.copy(alpha = 0.85f)
			style = PaintingStyle.Fill
			isAntiAlias = true
		}.asFrameworkPaint()
	}

	LaunchedEffect(sizePx) {
		dropPath.reset()
		val dropHeight = 0.24f * height
		val dropWidth = dropHeight * 0.45f
		dropPath.moveTo(0f, 0f)
		dropPath.cubicTo(dropWidth, dropHeight * 0.32f, dropWidth * 0.55f, dropHeight * 0.92f, 0f, dropHeight)
		dropPath.cubicTo(-dropWidth * 0.55f, dropHeight * 0.92f, -dropWidth, dropHeight * 0.32f, 0f, 0f)
		dropPath.close()
	}

	Canvas(
		modifier = modifier
			.width(size)
			.aspectRatio(1f)
	) {
		drawIntoCanvas { canvas ->
			val dropSlots = listOf(0.38f, 0.5f, 0.62f)
			val progressOffsets = listOf(0f, 0.3f, 0.6f)
			val speedFactors = listOf(1f, 0.85f, 1.1f)
			val rainStartY = 0.34f * height
			val travel = 0.34f * height
			val baseAlpha = dropPaint.alpha

			dropSlots.forEachIndexed { index, slot ->
				val offsetProgress = (fallProgress.value * speedFactors[index] + progressOffsets[index]) % 1f
				val eased = (offsetProgress + 0.1f * sin(offsetProgress * (2f * PI).toFloat())).let {
					val wrapped = it % 1f
					if (wrapped < 0f) wrapped + 1f else wrapped
				}
				val fadeStart = 0.75f
				val fadeFactor = if (eased > fadeStart) {
					1f - ((eased - fadeStart) / (1f - fadeStart))
				} else {
					1f
				}
				dropPaint.alpha = (baseAlpha * fadeFactor).toInt().coerceIn(0, baseAlpha)
				val dropX = slot * width
				val dropY = rainStartY + eased * travel
				canvas.nativeCanvas.save()
				canvas.nativeCanvas.translate(dropX, dropY)
				canvas.nativeCanvas.drawPath(dropPath, dropPaint)
				canvas.nativeCanvas.restore()
			}

			dropPaint.alpha = baseAlpha
		}
	}
}