package me.spica.spicaweather3.network.model

import androidx.annotation.Keep


@Keep
data class TopCityResponse(
  val code: String,
  val topCityList: List<Location>,
  val refer: Refer
)