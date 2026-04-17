package me.spica.spicaweather3.ui.widget.cloud

import androidx.compose.animation.core.EaseInOutBounce
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.drawscope.scale
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import kotlin.math.PI
import kotlin.math.sin

@Composable
fun CloudView(
    collapsedFraction: Float,
    show: Boolean,
) {
    ShowOnIdleContent(
        show,
        enter = slideInVertically { -it },
        exit = slideOutVertically { -it },
        modifier = Modifier.fillMaxSize()
    ) {
        val showProgress by animateFloatAsState(
            targetValue = if (show) 1f else 0f,
            animationSpec = spring(dampingRatio = .45f, stiffness = 500f),
            label = "show_progress"
        )

        val transition = rememberInfiniteTransition(label = "cloud_transition")
        val phase1 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 5400, easing = LinearEasing),
            ),
            label = "cloud_phase_1"
        )
        val phase2 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 3500, easing = LinearEasing),
            ),
            label = "cloud_phase_2"
        )
        val phase3 by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(durationMillis = 2750, easing = LinearEasing),
            ),
            label = "cloud_phase_3"
        )

        Canvas(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    translationY = -size.height / 2f * EaseInOutBounce.transform(collapsedFraction)
                }
        ) {
            val width = size.width
            val step = 16f * density
            val dist1 = sin(phase1 * 2f * PI.toFloat())
            val dist2 = sin(phase2 * 2f * PI.toFloat())
            val dist3 = sin(phase3 * 2f * PI.toFloat())

            scale(
                scaleX = showProgress,
                scaleY = showProgress,
                pivot = Offset(width, 0f)
            ) {
                drawCircle(
                    color = Color.White.copy(alpha = 0x26 / 255f),
                    radius = (width / 5f * showProgress + dist2 * step).coerceAtLeast(0f),
                    center = Offset(0f, 0f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0x80 / 255f),
                    radius = (width / 3f * showProgress + dist1 * step).coerceAtLeast(0f),
                    center = Offset(40f * density, 0f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0x26 / 255f),
                    radius = (width / 5f * showProgress + dist1 * step).coerceAtLeast(0f),
                    center = Offset(width / 2f, 0f)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0x80 / 255f),
                    radius = (width / 3f * showProgress + dist3 * step).coerceAtLeast(0f),
                    center = Offset(width / 2f + 6f * density, 18f * density)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0x26 / 255f),
                    radius = (width / 5f * showProgress + dist1 * step).coerceAtLeast(0f),
                    center = Offset(width, -4f * density)
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0x80 / 255f),
                    radius = (width / 3f * showProgress + dist2 * step).coerceAtLeast(0f),
                    center = Offset(width + 6f * density, 8f * density)
                )
            }
        }
    }
}
