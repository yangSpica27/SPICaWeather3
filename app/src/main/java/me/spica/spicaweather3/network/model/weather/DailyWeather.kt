package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Stable
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


private val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
private val sdf2 = SimpleDateFormat("HH:mm", Locale.getDefault())
// 格式化为"周几"
private val sdfWeek = SimpleDateFormat("E", Locale.getDefault())
@Stable
data class DailyWeather(
  val cloud: Int,
  val dayWindDir: String,
  val dayWindSpeed: String,
  val fxTime: String,
  val iconId: Int,
  val maxTemp: Int,
  val minTemp: Int,
  val moonParse: String,
  val nightWindDir: String,
  val nightWindSpeed: String,
  val precip: Double,
  val pressure: String,
  val sunriseDate: String,
  val sunsetDate: String,
  val wind360Day: String,
  val wind360Night: String,
  val windDirDay: String,
  val windDirNight: String,
  val windScaleDay: String,
  val windScaleNight: String,
  val windSpeedDay: String,
  val windSpeedNight: String,
  val uv: String,
  val vis: Int,
  val water: Int,
  val weatherNameDay: String,
  val weatherNameNight: String,
  val winSpeed: Int,
  val windPa: Int
) {
  // 获取紫外线强度的描述
  fun getUVLevelDescription(locale: Locale = Locale.getDefault()): String =
    when (uv.toIntOrNull()) {
      in 0..2 -> if (locale.isChinese()) "低" else "Low"
      in 3..5 -> if (locale.isChinese()) "中等" else "Moderate"
      in 6..7 -> if (locale.isChinese()) "高" else "High"
      in 8..10 -> if (locale.isChinese()) "很高" else "Very high"
      else -> if (locale.isChinese()) "极高" else "Extreme"
    }

  fun getWeatherText(): String{
    return if (weatherNameDay == weatherNameNight){
      weatherNameDay
    }else{
      "$weatherNameDay to $weatherNameNight"
    }
  }

  // 获取紫外线的描述
  fun getUVDescription(locale: Locale = Locale.getDefault()): String =
    when (uv.toIntOrNull()) {
      in 0..2 -> if (locale.isChinese()) "不需采取防护措施" else "No protection needed"
      in 3..5 -> if (locale.isChinese()) "涂擦 SPF 大于 15、PA+防晒护肤品" else "Use SPF 15+ sunscreen"
      in 6..7 -> if (locale.isChinese()) "尽量减少外出，需要涂抹高倍数防晒霜" else "Limit time outside, apply high SPF"
      in 8..10 -> if (locale.isChinese()) "尽量减少外出，需要涂抹高倍数防晒霜" else "Avoid direct sun, use high SPF"
      else -> if (locale.isChinese()) "尽量减少外出，需要涂抹高倍数防晒霜" else "Avoid direct sun, use high SPF"
    }

  fun fxTime(): Date {
    try {
      return sdf.parse(fxTime) ?: Date()
    } catch (e: Exception) {
      e.printStackTrace()
    }
    return Date()
  }




  fun isToday(): Boolean {
    val date = fxTime()
    return sdfWeek.format(date) == sdfWeek.format(Date())
  }

  fun getDayOfWeekLabel(): String {
    return sdfWeek.format(fxTime())
  }

  fun sunriseDate(): Date {
    try {
      return sdf2.parse(sunriseDate)!!
    } catch (_: Exception) {
    }
    return sdf2.parse("6:00")!!
  }

  fun sunsetDate(): Date {
    try {
      return sdf2.parse(sunsetDate)!!
    } catch (_: Exception) {
    }
    return sdf2.parse("18:00")!!
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as DailyWeather

    if (cloud != other.cloud) return false
    if (iconId != other.iconId) return false
    if (maxTemp != other.maxTemp) return false
    if (minTemp != other.minTemp) return false
    if (precip != other.precip) return false
    if (vis != other.vis) return false
    if (water != other.water) return false
    if (winSpeed != other.winSpeed) return false
    if (windPa != other.windPa) return false
    if (dayWindDir != other.dayWindDir) return false
    if (dayWindSpeed != other.dayWindSpeed) return false
    if (fxTime != other.fxTime) return false
    if (moonParse != other.moonParse) return false
    if (nightWindDir != other.nightWindDir) return false
    if (nightWindSpeed != other.nightWindSpeed) return false
    if (pressure != other.pressure) return false
    if (sunriseDate != other.sunriseDate) return false
    if (sunsetDate != other.sunsetDate) return false
    if (wind360Day != other.wind360Day) return false
    if (wind360Night != other.wind360Night) return false
    if (windDirDay != other.windDirDay) return false
    if (windDirNight != other.windDirNight) return false
    if (windScaleDay != other.windScaleDay) return false
    if (windScaleNight != other.windScaleNight) return false
    if (windSpeedDay != other.windSpeedDay) return false
    if (windSpeedNight != other.windSpeedNight) return false
    if (uv != other.uv) return false
    if (weatherNameDay != other.weatherNameDay) return false
    if (weatherNameNight != other.weatherNameNight) return false

    return true
  }

  override fun hashCode(): Int {
    var result = cloud
    result = 31 * result + iconId
    result = 31 * result + maxTemp
    result = 31 * result + minTemp
    result = 31 * result + precip.hashCode()
    result = 31 * result + vis
    result = 31 * result + water
    result = 31 * result + winSpeed
    result = 31 * result + windPa
    result = 31 * result + dayWindDir.hashCode()
    result = 31 * result + dayWindSpeed.hashCode()
    result = 31 * result + fxTime.hashCode()
    result = 31 * result + moonParse.hashCode()
    result = 31 * result + nightWindDir.hashCode()
    result = 31 * result + nightWindSpeed.hashCode()
    result = 31 * result + pressure.hashCode()
    result = 31 * result + sunriseDate.hashCode()
    result = 31 * result + sunsetDate.hashCode()
    result = 31 * result + wind360Day.hashCode()
    result = 31 * result + wind360Night.hashCode()
    result = 31 * result + windDirDay.hashCode()
    result = 31 * result + windDirNight.hashCode()
    result = 31 * result + windScaleDay.hashCode()
    result = 31 * result + windScaleNight.hashCode()
    result = 31 * result + windSpeedDay.hashCode()
    result = 31 * result + windSpeedNight.hashCode()
    result = 31 * result + uv.hashCode()
    result = 31 * result + weatherNameDay.hashCode()
    result = 31 * result + weatherNameNight.hashCode()
    return result
  }


}

private fun Locale.isChinese(): Boolean = language.startsWith("zh")