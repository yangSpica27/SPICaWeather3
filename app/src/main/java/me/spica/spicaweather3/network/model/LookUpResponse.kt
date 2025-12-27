package me.spica.spicaweather3.network.model

data class LookUpResponse(
    val code: String,
    val location: List<Location>,
    val refer: Refer
)