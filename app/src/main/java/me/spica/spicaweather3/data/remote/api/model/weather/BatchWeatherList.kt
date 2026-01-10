package me.spica.spicaweather3.data.remote.api.model.weather

import kotlinx.serialization.Serializable

// 批量天气查询响应
@Serializable
data class BatchWeatherList(
  val results: List<BatchWeatherResult>
)

@Serializable
data class BaseResponse<T>(
  val code: Int,
  val message: String,
  val data: T? = null
)