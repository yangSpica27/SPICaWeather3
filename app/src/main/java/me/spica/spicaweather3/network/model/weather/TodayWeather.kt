package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Stable

@Stable
data class TodayWeather(
    val feelTemp: Int,
    val fxLink: Any,
    val iconId: Int,
    val obsTime: String,
    val temp: Int,
    val water: Int,
    val weatherName: String,
    val windPa: Int,
    val windSpeed: Int
)