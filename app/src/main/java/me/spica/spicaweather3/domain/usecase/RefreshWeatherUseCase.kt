package me.spica.spicaweather3.domain.usecase

import me.spica.spicaweather3.data.local.db.entity.CityEntity
import me.spica.spicaweather3.domain.repository.IWeatherRepository

/**
 * 刷新天气数据的用例
 */
class RefreshWeatherUseCase(
    private val repository: IWeatherRepository
) {
    /**
     * 刷新单个城市的天气
     */
    suspend fun refreshCity(
        lat: String,
        lon: String,
        onError: (String?) -> Unit = {},
        onSucceed: () -> Unit = {}
    ) {
        repository.refreshWeather(lat, lon, onError, onSucceed)
    }
    
    /**
     * 批量刷新多个城市的天气
     */
    suspend fun refreshAllCities(
        cities: List<CityEntity>,
        onError: (String?) -> Unit = {},
        onSucceed: () -> Unit = {}
    ) {
        repository.refreshAllCitiesWeather(cities, onError, onSucceed)
    }
}
