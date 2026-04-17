package me.spica.spicaweather3.ui.main.weather

import me.spica.spicaweather3.domain.model.City

sealed class WeatherPageState(
  val city: City
) {

  class Data(city: City) : WeatherPageState(city)

  class Empty(city: City) : WeatherPageState(city)

}