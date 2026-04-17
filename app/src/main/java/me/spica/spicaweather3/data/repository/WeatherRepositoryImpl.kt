package me.spica.spicaweather3.data.repository

import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.App
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.local.db.dao.CityDao
import me.spica.spicaweather3.data.mapper.toDomain
import me.spica.spicaweather3.data.mapper.toEntity
import me.spica.spicaweather3.data.remote.api.ApiService
import me.spica.spicaweather3.data.remote.api.model.BatchWeatherRequest
import me.spica.spicaweather3.domain.model.City
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

    override fun getAllCitiesFlow(): Flow<List<City>> {
        return cityDao.getAllFlow().map { entities ->
            entities.map { it.toDomain() }
        }
    }

    override fun getAllCities(): List<City> {
        return cityDao.getAll().map { it.toDomain() }
    }

    override suspend fun refreshWeather(
        lat: String,
        lon: String,
        onError: (String?) -> Unit,
        onSucceed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        // 查找所有城市，找到匹配的城市
        val allCities = cityDao.getAll()
        val cityEntity = allCities.find { it.lat == lat && it.lon == lon }
        
        if (cityEntity == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        val response = apiService.getWeather(
            BatchWeatherRequest(listOf(cityEntity.toWeatherRequestLocation()))
        ).getOrNull()

        if (response == null || response.code != 200 || response.data == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        val weatherData = response.data.results.firstOrNull()
        if (weatherData != null && weatherData.success) {
            // 使用 copy 替代直接修改，避免可变性问题
            val updatedEntity = cityEntity.copy(weather = weatherData.data)
            cityDao.insert(updatedEntity)
            WidgetUpdateHelper.updateAllWidgets(App.instance)
            onSucceed()
        } else {
            onError(stringProvider.getString(R.string.error_request_failed))
        }
    }

    override suspend fun refreshAllCitiesWeather(
        cities: List<City>,
        onError: (String?) -> Unit,
        onSucceed: () -> Unit
    ) = withContext(Dispatchers.IO) {
        // 转换为 Entity 以访问数据层方法
        val cityEntities = cities.map { it.toEntity() }
        
        val response = apiService.getWeather(
            BatchWeatherRequest(cityEntities.map { it.toWeatherRequestLocation() })
        ).getOrNull()

        if (response == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        if (response.code != 200 || response.data == null) {
            onError(stringProvider.getString(R.string.error_request_failed))
            return@withContext
        }

        // 创建更新后的实体列表
        val updatedEntities = cityEntities.map { entity ->
            val weatherData = response.data.results.find { it.locationId == entity.id }
            if (weatherData != null && weatherData.success) {
                entity.copy(weather = weatherData.data)
            } else {
                entity
            }
        }

        cityDao.insertAll(updatedEntities)
        WidgetUpdateHelper.updateAllWidgets(App.instance)
        onSucceed()
    }

    override suspend fun insertCity(city: City) = withContext(Dispatchers.IO) {
        cityDao.insert(city.toEntity())
    }

    override suspend fun deleteCity(city: City) = withContext(Dispatchers.IO) {
        cityDao.delete(city.toEntity())
    }

    override suspend fun swapSort(city1: City, city2: City) =
        withContext(Dispatchers.IO) {
            cityDao.swapSort(city1.toEntity(), city2.toEntity())
        }

    override suspend fun getUserLoc(): City? = withContext(Dispatchers.IO) {
        return@withContext cityDao.getUserLoc()?.toDomain()
    }

    override suspend fun insertUserLoc(city: City) = withContext(Dispatchers.IO) {
        cityDao.updateUserLoc(city.toEntity())
    }
}
