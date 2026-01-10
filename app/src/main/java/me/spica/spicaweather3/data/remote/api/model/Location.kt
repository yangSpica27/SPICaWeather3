package me.spica.spicaweather3.data.remote.api.model

import androidx.annotation.Keep
import me.spica.spicaweather3.data.local.db.entity.CityEntity


@Keep
data class Location(
  val adm1: String,
  val adm2: String,
  val country: String,
  val fxLink: String,
  val id: String,
  val isDst: String,
  val lat: String,
  val lon: String,
  val name: String,
  val rank: String,
  val type: String,
  val tz: String,
  val utcOffset: String
) {

  fun toCity(): CityEntity = CityEntity(
    name = name,
    lat = lat,
    lon = lon,
    adm1 = adm1,
    adm2 = adm2,
  )

}