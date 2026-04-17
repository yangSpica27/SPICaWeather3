package me.spica.spicaweather3.domain.usecase

import androidx.annotation.WorkerThread
import com.baidu.location.BDLocation
import me.spica.spicaweather3.domain.model.City
import me.spica.spicaweather3.domain.repository.IWeatherRepository

/**
 * 处理用户定位的用例
 */
class LocationUseCase(
    private val repository: IWeatherRepository
) {
    /**
     * 获取用户定位城市
     */
    suspend fun getUserLocationCity(): City? {
        return repository.getUserLoc()
    }

    /**
     * 获取所有城市列表
     */
    @WorkerThread
    fun getAllCitiesFlow() = repository.getAllCities()
    
    /**
     * 保存用户定位城市
     */
    suspend fun saveUserLocation(
        bdLocation: BDLocation,
        shouldRefresh: Boolean = false,
        onError: (String?) -> Unit = {},
        onSucceed: () -> Unit = {}
    ) {
        val city = City(
            id = java.util.UUID.randomUUID().toString(),
            name = bdLocation.street ?: bdLocation.district ?: "NA",
            latitude = bdLocation.latitude.toString(),
            longitude = bdLocation.longitude.toString(),
            administrativeArea1 = bdLocation.province ?: "",
            administrativeArea2 = bdLocation.city ?: "",
            sortOrder = System.currentTimeMillis(),
            isUserLocation = true,
            weather = null
        )
        
        repository.insertUserLoc(city)
        
        if (shouldRefresh) {
            repository.refreshWeather(
                city.latitude,
                city.longitude,
                onError,
                onSucceed
            )
        } else {
            onSucceed()
        }
    }
}
