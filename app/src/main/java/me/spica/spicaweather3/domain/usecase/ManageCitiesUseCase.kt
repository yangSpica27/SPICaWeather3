package me.spica.spicaweather3.domain.usecase

import me.spica.spicaweather3.domain.model.City
import me.spica.spicaweather3.domain.repository.IWeatherRepository

/**
 * 管理城市的用例
 */
class ManageCitiesUseCase(
    private val repository: IWeatherRepository
) {
    /**
     * 添加城市
     */
    suspend fun addCity(city: City) {
        repository.insertCity(city)
    }
    
    /**
     * 删除城市
     */
    suspend fun deleteCity(city: City) {
        repository.deleteCity(city)
    }
    
    /**
     * 交换两个城市的排序
     */
    suspend fun swapCityOrder(city1: City, city2: City) {
        repository.swapSort(city1, city2)
    }
}
