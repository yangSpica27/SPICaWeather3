package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Stable

@Stable
data class WeatherData(
  val air: Air,
  val dailyWeather: List<DailyWeather>,
  val descriptionForToWeek: String,
  val descriptionForToday: String,
  val hourlyWeather: List<HourlyWeather>,
  val lifeIndexes: List<LifeIndexe>,
  val minutely: List<Minutely>,
  val todayWeather: TodayWeather,
  val warnings: List<WeatherWarning>,
  val air2: Air2
){
  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as WeatherData

    if (air != other.air) return false
    if (dailyWeather != other.dailyWeather) return false
    if (descriptionForToWeek != other.descriptionForToWeek) return false
    if (descriptionForToday != other.descriptionForToday) return false
    if (hourlyWeather != other.hourlyWeather) return false
    if (lifeIndexes != other.lifeIndexes) return false
    if (minutely != other.minutely) return false
    if (todayWeather != other.todayWeather) return false
    if (warnings != other.warnings) return false
    if (air2 != other.air2) return false

    return true
  }

  override fun hashCode(): Int {
    var result = air.hashCode()
    result = 31 * result + dailyWeather.hashCode()
    result = 31 * result + descriptionForToWeek.hashCode()
    result = 31 * result + descriptionForToday.hashCode()
    result = 31 * result + hourlyWeather.hashCode()
    result = 31 * result + lifeIndexes.hashCode()
    result = 31 * result + minutely.hashCode()
    result = 31 * result + todayWeather.hashCode()
    result = 31 * result + warnings.hashCode()
    result = 31 * result + air2.hashCode()
    return result
  }
}