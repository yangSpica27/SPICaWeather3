package me.spica.spicaweather3.network

import com.skydoves.sandwich.getOrNull
import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.R
import me.spica.spicaweather3.db.dao.CityDao
import me.spica.spicaweather3.network.model.Location
import me.spica.spicaweather3.utils.StringProvider


class ApiRepository(
  private val apiService: ApiService,
  private val cityDao: CityDao,
  private val stringProvider: StringProvider
) {


  suspend fun fetchWeather(
    onError: (String?) -> Unit,
    onSucceed: () -> Unit
  ) = withContext(Dispatchers.IO) {
    val cities = cityDao.getAll()
    cities.forEach { item ->
      val response = apiService.getWeather("${item.lon},${item.lat}").getOrNull()
      if (response == null || response.code != 200) {
        cityDao.insertAll(cities)
        onError(stringProvider.getString(R.string.error_request_failed))
        return@withContext
      }
      item.weather = response.data
    }
    if (!isActive) return@withContext
    cityDao.insertAll(cities)
    onSucceed.invoke()
  }

  suspend fun fetchCity(
    keyword: String,
    onError: (String?) -> Unit,
    onSucceed: (List<Location>) -> Unit
  ) = withContext(Dispatchers.IO) {
    apiService.lookupCity(keyword)
      .onSuccess {
        if (data.code == "200") {
          onSucceed(data.location)
        } else {
          onError(stringProvider.getString(R.string.error_request_failed))
        }
      }
      .onFailure {
        onError(message())
      }
  }


  suspend fun fetchTop(
    onError: (String?) -> Unit,
    onSucceed: (List<Location>) -> Unit
  ) = withContext(Dispatchers.IO) {
    apiService.topCity()
      .onSuccess {
        if (data.code == "200") {
          onSucceed(data.topCityList)
        } else {
          onError(stringProvider.getString(R.string.error_request_failed))
        }
      }
      .onFailure {
        onError(message())
      }
  }
}