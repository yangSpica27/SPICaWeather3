package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Stable


@Stable
data class Minutely(
    val fxTime: String,
    val precip: String,
    val type: String
)