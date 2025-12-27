package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Immutable


@Immutable
data class LifeIndexe(
    val category: String,
    val name: String,
    val text: String,
    val type: Int
)