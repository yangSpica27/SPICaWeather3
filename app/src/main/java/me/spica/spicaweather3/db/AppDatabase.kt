package me.spica.spicaweather3.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.spica.spicaweather3.db.dao.CityDao
import me.spica.spicaweather3.db.entity.CityEntity


@Database(
  entities = [
    CityEntity::class,
  ],
  version = 7,
  exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun cityDao(): CityDao
}
