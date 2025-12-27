package me.spica.spicaweather3.network.model.weather

data class Air(
    val aqi: Int,
    val category: String,
    val co: Double,
    val fxLink: Any,
    val no2: Int,
    val o3: Int,
    val pm10: Int,
    val pm2p5: Int,
    val primary: String,
    val so2: Int
)