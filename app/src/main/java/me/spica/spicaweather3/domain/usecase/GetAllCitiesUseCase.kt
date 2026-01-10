package me.spica.spicaweather3.domain.usecase

import kotlinx.coroutines.flow.Flow
import me.spica.spicaweather3.data.local.db.entity.CityEntity
import me.spica.spicaweather3.domain.repository.IWeatherRepository

/**
 * 获取所有城市列表的用例
 */
class GetAllCitiesUseCase(
    private val repository: IWeatherRepository
) {
    operator fun invoke(): Flow<List<CityEntity>> {
        return repository.getAllCitiesFlow()
    }

}
