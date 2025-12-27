package me.spica.spicaweather3.ui.main.weather

import me.spica.spicaweather3.db.entity.CityEntity

sealed class WeatherPageState(
  val cityEntity: CityEntity
) {

  class Data(cityEntity: CityEntity) : WeatherPageState(cityEntity)

  class Empty(cityEntity: CityEntity) : WeatherPageState(cityEntity)

}