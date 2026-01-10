package me.spica.spicaweather3.data.remote.api.model

import androidx.annotation.Keep


@Keep
data class TopCityResponse(
  val code: String,
  val topCityList: List<Location>,
  val refer: Refer
)