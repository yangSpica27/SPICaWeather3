package me.spica.spicaweather3.data.repository

import com.skydoves.sandwich.message
import com.skydoves.sandwich.onFailure
import com.skydoves.sandwich.onSuccess
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.remote.api.ApiService
import me.spica.spicaweather3.data.remote.api.model.Location
import me.spica.spicaweather3.domain.repository.ICityRepository
import me.spica.spicaweather3.utils.StringProvider

/**
 * 城市搜索仓库实现
 * 
 * 实现 ICityRepository 接口，提供城市搜索和热门城市获取功能。
 * 
 * @param apiService 远程 API 服务
 * @param stringProvider 字符串资源提供者
 */
class CityRepositoryImpl(
    private val apiService: ApiService,
    private val stringProvider: StringProvider
) : ICityRepository {

    override suspend fun searchCity(
        keyword: String,
        onError: (String?) -> Unit,
        onSucceed: (List<Location>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
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
    }

    override suspend fun fetchTopCities(
        onError: (String?) -> Unit,
        onSucceed: (List<Location>) -> Unit
    ) {
        withContext(Dispatchers.IO) {
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
}
