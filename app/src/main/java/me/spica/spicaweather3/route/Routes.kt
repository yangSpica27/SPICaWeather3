package me.spica.spicaweather3.route

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

/**
 * 页面路由
 */
object Routes {

  // 主页
  @Serializable
  data object Main : NavKey

  // 天气列表
  @Serializable
  data object WeatherList : NavKey

  // 城市列表
  @Serializable
  data object CitySelect : NavKey

  // 空气质量
  @Serializable
  data object AirQuality : NavKey

}

val LocalNavController = staticCompositionLocalOf<NavBackStack<NavKey>> {
  error("No NavBackStack provided")
}
