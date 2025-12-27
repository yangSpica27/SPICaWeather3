package me.spica.spicaweather3.ui.widget.icon

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.theme.COLOR_WHITE_100


@Composable
fun WeatherIcon(size: Dp = 35.dp, color: Color = COLOR_WHITE_100,iconId: String) {
  return when (iconId) {
    "100", "150" -> SunIconView(size = size, color = color)
    "104" -> CloudIconView(size = size, color = color)
    "101", "102", "103", "151", "152", "153" ->  CloudIconView(size = size, color = color)
    "500", "501", "502", "503", "504", "505",
    "506", "507", "508", "509", "510", "511", "512",
    "513", "514", "515" -> FogIcon(size = size, color = color)
    "400", "401", "402", "403", "404", "405",
    "406", "407", "408", "409", "410", "456", "457",
    "499" -> SnowIcon(size = size, color = color)
    "300", "301", "302", "303", "304", "305",
    "306", "307", "308", "309", "310", "311",
    "312", "313", "314", "315", "316", "317",
    "318", "319", "320", "356", "357", "399"
      -> RainIcon(size = size, color = color)
    else -> CloudIconView(size = size, color = color)
  }
}