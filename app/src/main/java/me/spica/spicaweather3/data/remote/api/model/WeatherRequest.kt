package me.spica.spicaweather3.data.remote.api.model

import kotlinx.serialization.Serializable

@Serializable
data class BatchWeatherRequest(
  val locations: List<LocationRequest>,
  val includeHourly: Boolean? = true,
  val includeMinutely: Boolean? = true,
  val includeAqi: Boolean? = true,
  val includeAlerts: Boolean? = true
)

@Serializable
data class LocationRequest(
  val locationId: String,  // 自定义位置ID，用于识别结果
  val longitude: String,
  val latitude: String,
  val name: String? = null
)