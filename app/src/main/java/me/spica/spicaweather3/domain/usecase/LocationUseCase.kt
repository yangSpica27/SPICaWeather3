package me.spica.spicaweather3.domain.usecase

import androidx.annotation.WorkerThread
import com.baidu.location.BDLocation
import me.spica.spicaweather3.data.local.db.entity.CityEntity
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
    suspend fun getUserLocationCity(): CityEntity? {
        return repository.getUserLoc()
    }

    /**
     * 获取用户定位城市list
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
        val city = CityEntity(
            name = bdLocation.street ?: bdLocation.district ?: "NA",
            lat = bdLocation.latitude.toString(),
            lon = bdLocation.longitude.toString(),
            adm1 = bdLocation.province ?: "",
            adm2 = bdLocation.city ?: "",
            isUserLoc = true
        )
        
        repository.insertUserLoc(city)
        
        if (shouldRefresh) {
            repository.refreshWeather(
                city.lat,
                city.lon,
                onError,
                onSucceed
            )
        } else {
            onSucceed()
        }
    }
}
