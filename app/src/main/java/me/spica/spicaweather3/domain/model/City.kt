package me.spica.spicaweather3.domain.model

/**
 * 领域模型 - 城市
 * 不依赖任何 data 层类，作为 domain 层的纯业务模型
 */
data class City(
    val id: String,
    val name: String,
    val latitude: String,
    val longitude: String,
    val administrativeArea1: String,  // 省/州
    val administrativeArea2: String,  // 市
    val sortOrder: Long,
    val isUserLocation: Boolean,
    val weather: WeatherData? = null
)
