package me.spica.spicaweather3.data.repository

import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.App
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.local.db.dao.CityDao
import me.spica.spicaweather3.data.local.db.entity.CityEntity
import me.spica.spicaweather3.data.remote.api.ApiService
import me.spica.spicaweather3.data.remote.api.model.BatchWeatherRequest
import me.spica.spicaweather3.domain.repository.IWeatherRepository
import me.spica.spicaweather3.ui.app_widget.WidgetUpdateHelper
import me.spica.spicaweather3.utils.StringProvider

/**
 * 天气数据仓库实现
 * 
 * 整合了本地数据库和远程 API 的访问，实现 IWeatherRepository 接口。
 * 负责处理所有与天气数据和城市管理相关的数据操作。
 * 
 * @param apiService 远程 API 服务
 * @param cityDao 本地城市数据库访问对象
 * @param stringProvider 字符串资源提供者
 */
class WeatherRepositoryImpl(
    private val apiService: ApiService,
    private val cityDao: CityDao,
    private val stringProvider: StringProvider
) : IWeatherRepository {

    override fun getAllCitiesFlow(): Flow<List<CityEntity>> {
        return cityDao.getAllFlow()
    }

    override fun getAllCities(): List<CityEntity> {
        return cityDao.getAll()
    }

    override suspend fun refreshWeather(
        lat: String,
        lon: String,
        onError: (String?) -> Unit,
        onSucceed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        // 查找所有城市，找到匹配的城市
        val allCities = cityDao.getAll()
        val city = allCities.find { it.lat == lat && it.lon == lon }
        
        if (city == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        val response = apiService.getWeather(
            BatchWeatherRequest(listOf(city.toWeatherRequestLocation()))
        ).getOrNull()

        if (response == null || response.code != 200 || response.data == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        val weatherData = response.data.results.firstOrNull()
        if (weatherData != null && weatherData.success) {
            city.weather = weatherData.data
            cityDao.insert(city)
            WidgetUpdateHelper.updateTodayInfoWidgets(App.instance)
            onSucceed()
        } else {
            onError(stringProvider.getString(R.string.error_request_failed))
        }
    }

    override suspend fun refreshAllCitiesWeather(
        cities: List<CityEntity>,
        onError: (String?) -> Unit,
        onSucceed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        val response = apiService.getWeather(
            BatchWeatherRequest(cities.map { it.toWeatherRequestLocation() })
        ).getOrNull()

        if (response == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        if (response.code != 200 || response.data == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        cities.forEach { city ->
            val weatherData = response.data.results.find { it.locationId == city.id }
            if (weatherData != null && weatherData.success) {
                city.weather = weatherData.data
            }
        }

        cityDao.insertAll(cities)
        WidgetUpdateHelper.updateTodayInfoWidgets(App.instance)
        onSucceed()
    }

    override suspend fun insertCity(city: CityEntity) = withContext(Dispatchers.IO) {
        cityDao.insert(city)
    }

    override suspend fun deleteCity(city: CityEntity) = withContext(Dispatchers.IO) {
        cityDao.delete(city)
    }

    override suspend fun swapSort(city1: CityEntity, city2: CityEntity) =
        withContext(Dispatchers.IO) {
            cityDao.swapSort(city1, city2)
        }

    override suspend fun getUserLoc(): CityEntity? = withContext(Dispatchers.IO) {
        return@withContext cityDao.getUserLoc()
    }

    override suspend fun insertUserLoc(city: CityEntity) = withContext(Dispatchers.IO) {
        cityDao.updateUserLoc(city)
    }
}
