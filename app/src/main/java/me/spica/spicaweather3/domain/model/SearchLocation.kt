package me.spica.spicaweather3.domain.model

/**
 * 领域模型 - 搜索结果中的城市位置
 */
data class SearchLocation(
    val id: String,
    val name: String,
    val latitude: String,
    val longitude: String,
    val administrativeArea1: String,
    val administrativeArea2: String,
    val country: String,
    val type: String,
    val rank: String
)
