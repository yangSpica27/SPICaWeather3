package me.spica.spicaweather3.db

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.db.dao.CityDao
import me.spica.spicaweather3.db.entity.CityEntity

class PersistenceRepository(
  val cityDao: CityDao
) {


  suspend fun insertCity(cityEntity: CityEntity) = withContext(Dispatchers.IO) {
    cityDao.insert(cityEntity = cityEntity)
  }

  suspend fun swapSort(cityEntity: CityEntity, cityEntity2: CityEntity) =
    withContext(Dispatchers.IO) {
      cityDao.swapSort(city1 = cityEntity, city2 = cityEntity2)
    }

  suspend fun getUserLoc() = withContext(Dispatchers.IO) {
    return@withContext cityDao.getUserLoc()
  }


  suspend fun insertUserLoc(cityEntity: CityEntity) {
    withContext(Dispatchers.IO) {
      cityDao.updateUserLoc(cityEntity = cityEntity)
    }
  }

  fun getAllCitiesFlow() = cityDao.getAllFlow()

  suspend fun deleteCity(cityEntity: CityEntity) = withContext(Dispatchers.IO) {
    cityDao.delete(cityEntity = cityEntity)
  }

}