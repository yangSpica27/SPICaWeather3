package me.spica.spicaweather3.db.type_converter

import androidx.annotation.Keep
import androidx.room.TypeConverter
import com.google.gson.GsonBuilder
import com.google.gson.reflect.TypeToken
import me.spica.spicaweather3.network.model.weather.AggregatedWeatherData

@Keep
@Suppress("unused")
class RoomTypeConverters {

  private val gson = GsonBuilder().serializeNulls().create()


  @TypeConverter
  fun nowToString(data: AggregatedWeatherData?): String? {
    if (data == null) return ""
    return gson.toJson(data)
  }

  @TypeConverter
  fun stringToNow(json: String?): AggregatedWeatherData? {
    if (json.isNullOrEmpty()) return null
    return gson.fromJson<AggregatedWeatherData?>(
      json,
      object : TypeToken<AggregatedWeatherData?>() {}.type
    )
  }

}