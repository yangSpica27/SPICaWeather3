package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Stable

@Stable
data class WeatherData(
  val dailyWeather: List<DailyWeather>,
  val descriptionForToWeek: String,
  val descriptionForToday: String,
  val hourlyWeather: List<HourlyWeather>,
  val lifeIndexes: List<LifeIndexe>,
  val minutely: List<Minutely>,
  val todayWeather: TodayWeather,
  val warnings: List<WeatherWarning>,
  val air2: Air2
)