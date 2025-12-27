package me.spica.spicaweather3.route

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController
import kotlinx.serialization.Serializable

/**
 * 页面路由
 */
object Routes {

  // 主页
  @Serializable
  data object Main

  // 天气列表
  @Serializable
  data object WeatherList

  // 城市列表
  @Serializable
  data object CitySelect

  // 空气质量
  @Serializable
  data object AirQuality

}

val LocalNavController = staticCompositionLocalOf<NavHostController> {
  error("No NavController provided")
}
