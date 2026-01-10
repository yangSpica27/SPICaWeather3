package me.spica.spicaweather3.domain.usecase

import me.spica.spicaweather3.data.remote.api.model.Location
import me.spica.spicaweather3.domain.repository.ICityRepository

/**
 * 搜索城市的用例
 */
class SearchCityUseCase(
    private val repository: ICityRepository
) {
    /**
     * 根据关键字搜索城市
     */
    suspend operator fun invoke(
        keyword: String,
        onError: (String?) -> Unit = {},
        onSucceed: (List<Location>) -> Unit = {}
    ) {
        if (keyword.isEmpty()) {
            onSucceed(emptyList())
            return
        }
        repository.searchCity(keyword, onError, onSucceed)
    }
    
    /**
     * 获取热门城市
     */
    suspend fun getTopCities(
        onError: (String?) -> Unit = {},
        onSucceed: (List<Location>) -> Unit = {}
    ) {
        repository.fetchTopCities(onError, onSucceed)
    }
}
