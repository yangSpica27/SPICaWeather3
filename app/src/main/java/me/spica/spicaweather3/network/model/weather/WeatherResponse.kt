package me.spica.spicaweather3.network.model.weather

data class WeatherResponse(
  val code: Int,
  val `data`: WeatherData,
  val message: String
)