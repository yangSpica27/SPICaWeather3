package me.spica.spicaweather3.data.remote.api.model

data class LookUpResponse(
    val code: String,
    val location: List<Location>,
    val refer: Refer
)