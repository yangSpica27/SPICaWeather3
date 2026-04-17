package me.spica.spicaweather3.domain.repository

import kotlinx.coroutines.flow.Flow
import me.spica.spicaweather3.domain.model.City

/**
 * 天气数据仓库接口
 * 
 * 定义天气相关数据的操作规范
 */
interface IWeatherRepository {
    
    /**
     * 获取所有城市列表的 Flow
     */
    fun getAllCitiesFlow(): Flow<List<City>>

    /**
     * 获取所有城市列表
     */
    fun getAllCities(): List<City>
    
    /**
     * 根据经纬度刷新天气数据
     */
    suspend fun refreshWeather(
        lat: String,
        lon: String,
        onError: (String?) -> Unit,
        onSucceed: () -> Unit
    )
    
    /**
     * 批量刷新多个城市的天气
     */
    suspend fun refreshAllCitiesWeather(
        cities: List<City>,
        onError: (String?) -> Unit,
        onSucceed: () -> Unit
    )
    
    /**
     * 插入或更新城市
     */
    suspend fun insertCity(city: City)
    
    /**
     * 删除城市
     */
    suspend fun deleteCity(city: City)
    
    /**
     * 交换两个城市的排序
     */
    suspend fun swapSort(city1: City, city2: City)
    
    /**
     * 获取用户定位的城市
     */
    suspend fun getUserLoc(): City?
    
    /**
     * 插入或更新用户定位城市
     */
    suspend fun insertUserLoc(city: City)
}
