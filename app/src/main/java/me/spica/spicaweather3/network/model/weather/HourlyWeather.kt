package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Stable
import java.text.SimpleDateFormat
import java.util.*


private val sdf =
  SimpleDateFormat(
    "yyyy-MM-dd'T'HH:mm+08:00",
    Locale.CHINA,
  )

@Stable
data class HourlyWeather(
  val fxTime: String,
  val iconId: Int,
  val pop: Int,
  val temp: Int,
  val water: Int,
  val weatherName: String,
  val windPa: Int,
  val windSpeed: Int,
  val wind360: String,
  val windDir: String
) {
  fun fxTime(): Date {
    try {
      return sdf.parse(fxTime) ?: Date()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return Date()
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as HourlyWeather

    if (iconId != other.iconId) return false
    if (pop != other.pop) return false
    if (temp != other.temp) return false
    if (water != other.water) return false
    if (windPa != other.windPa) return false
    if (windSpeed != other.windSpeed) return false
    if (fxTime != other.fxTime) return false
    if (weatherName != other.weatherName) return false
    if (wind360 != other.wind360) return false
    if (windDir != other.windDir) return false

    return true
  }

  override fun hashCode(): Int {
    var result = iconId
    result = 31 * result + pop
    result = 31 * result + temp
    result = 31 * result + water
    result = 31 * result + windPa
    result = 31 * result + windSpeed
    result = 31 * result + fxTime.hashCode()
    result = 31 * result + weatherName.hashCode()
    result = 31 * result + wind360.hashCode()
    result = 31 * result + windDir.hashCode()
    return result
  }
}