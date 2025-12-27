package me.spica.spicaweather3.ui.widget

import androidx.compose.animation.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import kotlinx.coroutines.launch
import me.spica.spicaweather3.common.WeatherAnimType
import me.spica.spicaweather3.ui.widget.cloud.CloudView
import me.spica.spicaweather3.ui.widget.galaxy.GalaxyView
import me.spica.spicaweather3.ui.widget.haze.HazeView
import me.spica.spicaweather3.ui.widget.rain.RainView
import me.spica.spicaweather3.ui.widget.snow.SnowView
import me.spica.spicaweather3.ui.widget.sun.SunView
import me.spica.spicaweather3.ui.widget.sun.SunView2


@Composable
fun WeatherBackground(
  collapsedFraction: Float,
  currentWeatherType: WeatherAnimType,
  hazeState: HazeState,
) {

  val currentTopColor = remember { Animatable(WeatherAnimType.RainLight.topColor) }
  val currentBottomColor = remember { Animatable(WeatherAnimType.RainLight.bottomColor) }

  LaunchedEffect(currentWeatherType) {
    launch {
      currentBottomColor.animateTo(currentWeatherType.bottomColor, tween(1000))
    }
    launch {
      currentTopColor.animateTo(currentWeatherType.topColor, tween(1000))
    }
  }

  Box(
    modifier = Modifier
      .fillMaxSize()
      .hazeSource(hazeState)
      .hazeEffect {
        progressive = HazeProgressive.verticalGradient(
          startIntensity = 0f,
          endIntensity = .8f
        )
      }
      .background(
        // 渐变色的绘制视线
        brush = Brush.verticalGradient(
          colors = listOf(
            currentTopColor.value,
            currentTopColor.value,
            currentTopColor.value,
            currentBottomColor.value
          )
        )
      )
  ) {
    GalaxyView(collapsedFraction, currentWeatherType.showGalaxy)
    SnowView(show = currentWeatherType.showSnow)
    RainView(show = currentWeatherType.showRain)
    CloudView(collapsedFraction, currentWeatherType.showCloud)
    SunView(collapsedFraction, currentWeatherType.showSun)
    HazeView(currentWeatherType.showHaze)
  }

}


