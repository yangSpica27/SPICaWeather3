package me.spica.spicaweather3.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.AnimationVector1D
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


@Composable
fun AnimateOnEnter(
  modifier: Modifier = Modifier,
  animationSpec: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMedium
  ),
  delayMillis: Int = 0,
  initialValue: Float = 0f,
  targetValue: Float = 1f,
  content: @Composable (animatedValue: Float, Animatable<Float, AnimationVector1D>) -> Unit
) {

  val animatable = remember { Animatable(initialValue) }
  val visible = rememberSaveable { mutableStateOf(false) }
  val view = LocalView.current
  
  LaunchedEffect(visible.value) {
    if (visible.value) {
      if (delayMillis > 0) {
        kotlinx.coroutines.delay(delayMillis.toLong())
      }
      animatable.animateTo(targetValue, animationSpec)
    }
  }

  val coroutineScope = rememberCoroutineScope()

  Box(
    modifier = modifier
      .onGloballyPositioned { layoutCoordinates ->
        coroutineScope.launch(Dispatchers.Default) {
          if (!visible.value) {
            visible.value = layoutCoordinates.boundsInWindow().height > 40
          }
        }
      }
  ) {
    content(animatable.value,animatable)
  }

}