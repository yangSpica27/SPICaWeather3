package me.spica.spicaweather3.data.local.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.spica.spicaweather3.data.local.db.dao.CityDao
import me.spica.spicaweather3.data.local.db.entity.CityEntity


/**
 * 应用数据库
 * 
 * Room 数据库实例，管理本地数据持久化。
 * 当前版本：8 (添加索引优化查询性能)
 * 
 * 版本历史:
 * - v8: 为 CityEntity 添加 isUserLoc 和 sort 字段的索引
 * - v7: 之前的版本
 * 
 * @see CityEntity 城市数据实体
 * @see CityDao 城市数据访问对象
 */
@Database(
  entities = [
    CityEntity::class,
  ],
  version = 8,
  exportSchema = false  // 禁用 schema 导出，避免警告
)
abstract class AppDatabase : RoomDatabase() {
  abstract fun cityDao(): CityDao

  companion object {
    const val DATABASE_NAME = "spica_weather.db"

    val MIGRATION_7_8 = object : Migration(7, 8) {
      override fun migrate(db: SupportSQLiteDatabase) {
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_CityEntity_isUserLoc` ON `CityEntity` (`isUserLoc`)")
        db.execSQL("CREATE INDEX IF NOT EXISTS `index_CityEntity_sort` ON `CityEntity` (`sort`)")
      }
    }

    fun build(context: Context): AppDatabase {
      return Room.databaseBuilder(
        context.applicationContext,
        AppDatabase::class.java,
        DATABASE_NAME
      )
        .addMigrations(MIGRATION_7_8)
        .fallbackToDestructiveMigration(false)
        .build()
    }
  }
}
