package me.spica.spicaweather3.domain.usecase

import me.spica.spicaweather3.domain.model.SearchLocation
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
        onSucceed: (List<SearchLocation>) -> Unit = {}
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
        onSucceed: (List<SearchLocation>) -> Unit = {}
    ) {
        repository.fetchTopCities(onError, onSucceed)
    }
}
