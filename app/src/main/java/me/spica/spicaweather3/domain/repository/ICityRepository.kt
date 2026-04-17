package me.spica.spicaweather3.domain.repository

import me.spica.spicaweather3.domain.model.SearchLocation

/**
 * 城市数据仓库接口
 * 
 * 定义城市搜索和查询相关操作
 */
interface ICityRepository {
    
    /**
     * 搜索城市
     */
    suspend fun searchCity(
        keyword: String,
        onError: (String?) -> Unit,
        onSucceed: (List<SearchLocation>) -> Unit
    )
    
    /**
     * 获取热门城市列表
     */
    suspend fun fetchTopCities(
        onError: (String?) -> Unit,
        onSucceed: (List<SearchLocation>) -> Unit
    )
}
