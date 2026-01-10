package me.spica.spicaweather3.data.local.db

import androidx.room.Database
import androidx.room.RoomDatabase
import me.spica.spicaweather3.data.local.db.dao.CityDao
import me.spica.spicaweather3.data.local.db.entity.CityEntity


/**
 * 应用数据库
 * 
 * Room 数据库实例，管理本地数据持久化。
 * 当前版本：7
 * 
 * @see CityEntity 城市数据实体
 * @see CityDao 城市数据访问对象
 */
@Database(
  entities = [
    CityEntity::class,
  ],
  version = 7,
  exportSchema = false  // 禁用 schema 导出，避免警告
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun cityDao(): CityDao
}
