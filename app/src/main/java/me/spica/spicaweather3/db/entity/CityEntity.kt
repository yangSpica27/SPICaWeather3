package me.spica.spicaweather3.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import me.spica.spicaweather3.db.type_converter.RoomTypeConverters
import me.spica.spicaweather3.network.model.LocationRequest
import me.spica.spicaweather3.network.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import java.util.*

@TypeConverters(RoomTypeConverters::class)
@Entity
data class CityEntity(
  @PrimaryKey
  val id: String = UUID.randomUUID().toString(),
  var name: String,
  var lat: String,
  var lon: String,
  var adm1: String,
  var adm2: String,
  var sort: Long = System.currentTimeMillis(),
  var isUserLoc: Boolean = false,
  var weather: AggregatedWeatherData? = null
) {

  fun toWeatherRequestLocation() =
    LocationRequest(
      locationId = id,
      longitude = lon,
      latitude = lat,
      name = name
    )

  fun toWeatherData(): WeatherPageState {
    if (weather == null) return WeatherPageState.Empty(this)
    return WeatherPageState.Data(this)
  }

  override fun toString(): String {
    return "CityEntity(id='$id', name='$name', lat='$lat', lon='$lon', adm1='$adm1', adm2='$adm2', sort=$sort, weather=$weather)"
  }

  override fun equals(other: Any?): Boolean {
    if (this === other) return true
    if (javaClass != other?.javaClass) return false

    other as CityEntity

    if (sort != other.sort) return false
    if (id != other.id) return false
    if (name != other.name) return false
    if (lat != other.lat) return false
    if (lon != other.lon) return false
    if (adm1 != other.adm1) return false
    if (adm2 != other.adm2) return false
    if (weather != other.weather) return false

    return true
  }

  override fun hashCode(): Int {
    var result = sort.hashCode()
    result = 31 * result + id.hashCode()
    result = 31 * result + name.hashCode()
    result = 31 * result + lat.hashCode()
    result = 31 * result + lon.hashCode()
    result = 31 * result + adm1.hashCode()
    result = 31 * result + adm2.hashCode()
    result = 31 * result + (weather?.hashCode() ?: 0)
    return result
  }


}


