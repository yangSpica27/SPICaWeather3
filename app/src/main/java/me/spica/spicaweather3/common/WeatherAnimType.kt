package me.spica.spicaweather3.common

import androidx.compose.ui.graphics.Color
import me.spica.spicaweather3.R
import java.util.*

sealed class WeatherAnimType(
  val key: String,
  val topColor: Color,
  val bottomColor: Color,
  val showGalaxy: Boolean = false,
  val showCloud: Boolean = false,
  val showRain: Boolean = false,
  val showSnow: Boolean = false,
  val showSun: Boolean = false,
  val showHaze: Boolean = false
) {
  // 雨/白天
  object RainLight : WeatherAnimType(
    key = "rain-light",
    topColor = Color(0xFF0062E3),
    bottomColor = Color(0xFF054184),
    showCloud = false,
    showGalaxy = false,
    showRain = true
  )

  // 雨天/夜晚
  object RainDark : WeatherAnimType(
    key = "rain-dark",
    topColor = Color(0xFF001d66),
    bottomColor = Color(0xFF002b4B),
    showCloud = false,
    showGalaxy = false,
    showRain = true
  )

  // 雪/白天
  object SnowLight : WeatherAnimType(
    key = "snow-light",
    topColor = Color(0xFF4096ff),
    bottomColor = Color(0xFF0062E3),
    showCloud = true,
    showSnow = true
  )

  // 雪天/夜晚
  object SnowDark : WeatherAnimType(
    key = "snow-dark",
    topColor = Color(0xFF001d66),
    bottomColor = Color(0xFF024DAF),
    showGalaxy = true,
    showSnow = true
  )

  // 雾/白天
  object FogLight : WeatherAnimType(
    key = "fog-light",
    topColor = Color(0xffd48806),
    bottomColor = Color(0xfffaad14),
    showCloud = false,
    showHaze = true
  )

  // 雾天/夜晚
  object FogDark : WeatherAnimType(
    key = "fog-dark",
    topColor = Color(0xFF024DAF),
    bottomColor = Color(0xFF001d66),
    showCloud = false,
    showHaze = true
  )

  // 晴/白天
  object SunLight : WeatherAnimType(
    key = "sun-light",
    topColor = Color(0xFFfa8c16),
    bottomColor = Color(0xFFffa940),
    showSun = true
  )

  // 晴天/夜晚
  object SunDark : WeatherAnimType(
    key = "sun-dark",
    topColor = Color(0xFF120338),
    bottomColor = Color(0xFF030852),
    showGalaxy = true,
  )

  // 多云/白天
  object CloudLight : WeatherAnimType(
    key = "cloud-light",
    topColor = Color(0xFF4096ff),
    bottomColor = Color(0xFF0062E3),
    showCloud = true
  )

  // 多云/夜晚
  object CloudDark : WeatherAnimType(
    key = "cloud-dark",
    topColor = Color(0xFF0B121D),
    bottomColor = Color(0xFF44505F),
    showCloud = true
  )

  // 阴/白天
  object OvercastLight : WeatherAnimType(
    key = "overcast-light",
    topColor = Color(0xFF08979c),
    bottomColor = Color(0xFf006d75),
    showCloud = true
  )

  // 阴天/夜晚
  object OvercastDark : WeatherAnimType(
    key = "overcast-dark",
    topColor = Color(0xFF002766),
    bottomColor = Color(0xFF120338),
    showCloud = true
  )


  companion object {
    fun getWeatherIconRes(iconId: String): Int {
      return when (iconId) {
        "100", "150" ->  R.drawable.ic_sunny // 晴天
        "104" -> R.drawable.ic_cloudy // 阴天
        "101", "102", "103", "151", "152", "153" -> R.drawable.ic_forecast// 云
        "500", "501", "502", "503", "504", "505",
        "506", "507", "508", "509", "510", "511", "512",
        "513", "514", "515" -> R.drawable.ic_fog // 雾天
        "400", "401", "402", "403", "404", "405",
        "406", "407", "408", "409", "410", "456", "457",
        "499" -> R.drawable.ic_snow // 雪天
        "300", "301", "302", "303", "304", "305",
        "306", "307", "308", "309", "310", "311",
        "312", "313", "314", "315", "316", "317",
        "318", "319", "320", "356", "357",  "399"
          -> R.drawable.ic_rain // 雨天
        else -> R.drawable.ic_cloudy
      }
    }
    fun getAnimType(
      iconId: String,
      isNight: Boolean = Calendar.getInstance().get(Calendar.HOUR_OF_DAY) !in 7..19
    ): WeatherAnimType {
      return when (iconId) {
        "100", "150" -> if (isNight) SunDark else SunLight // 晴天
        "104" -> if (isNight) OvercastDark else OvercastLight // 阴天
        "101", "102", "103", "151", "152", "153" -> if (isNight) CloudDark else CloudLight // 云
        "500", "501", "502", "503", "504", "505",
        "506", "507", "508", "509", "510", "511", "512",
        "513", "514", "515" ->
          if (isNight) FogDark else FogLight // 雾天
        "400", "401", "402", "403", "404", "405",
        "406", "407", "408", "409", "410", "456", "457",
        "499" -> if (isNight) SnowDark else SnowLight // 雪天
        "300", "301", "302", "303", "304", "305",
        "306", "307", "308", "309", "310", "311",
        "312", "313", "314", "315", "316", "317",
        "318", "319", "320", "356", "357", "399"
          -> if (isNight) RainDark else RainLight // 雨天
        else -> CloudLight
      }
    }
  }

}
