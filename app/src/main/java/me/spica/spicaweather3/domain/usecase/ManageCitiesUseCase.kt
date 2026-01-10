package me.spica.spicaweather3.domain.usecase

import me.spica.spicaweather3.data.local.db.entity.CityEntity
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
    suspend fun addCity(city: CityEntity) {
        repository.insertCity(city)
    }
    
    /**
     * 删除城市
     */
    suspend fun deleteCity(city: CityEntity) {
        repository.deleteCity(city)
    }
    
    /**
     * 交换两个城市的排序
     */
    suspend fun swapCityOrder(city1: CityEntity, city2: CityEntity) {
        repository.swapSort(city1, city2)
    }
}
