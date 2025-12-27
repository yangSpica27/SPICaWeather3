package me.spica.spicaweather3.ui.widget

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.AnimatedVisibilityScope
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import me.spica.spicaweather3.utils.doOnMainThreadIdle


@Composable
fun ShowOnIdleContent(
  visible: Boolean,
  modifier: Modifier = Modifier,
  enter: EnterTransition = fadeIn(),
  exit: ExitTransition = fadeOut(),
  label: String = "AnimatedVisibility",
  content: @Composable() AnimatedVisibilityScope.() -> Unit,
) {

  var showState by remember{ mutableStateOf(false) }

  LaunchedEffect(
    key1 = visible,
  ) {
    doOnMainThreadIdle({ showState = visible }, 750)
  }

  AnimatedVisibility(
    visible =  showState,
    enter = enter,
    exit = exit,
    modifier = modifier,
    label = label,
    content = content,
  )

}