package me.spica.spicaweather3.core.di

import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import me.spica.spicaweather3.data.local.db.AppDatabase
import me.spica.spicaweather3.data.local.db.dao.CityDao
import me.spica.spicaweather3.utils.DataStoreUtil
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 数据库迁移：从版本 7 到版本 8
 * 添加索引以优化查询性能
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
  override fun migrate(db: SupportSQLiteDatabase) {
    // 为 isUserLoc 字段添加索引，优化用户定位城市查询
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_CityEntity_isUserLoc` ON `CityEntity` (`isUserLoc`)")
    // 为 sort 字段添加索引，优化排序查询
    db.execSQL("CREATE INDEX IF NOT EXISTS `index_CityEntity_sort` ON `CityEntity` (`sort`)")
  }
}

/**
 * 数据持久化层依赖注入模块
 * 
 * 提供 Room 数据库、DataStore、SharedPreferences 等持久化依赖
 */
val persistenceModule = module {
  single<SharedPreferences> {
    androidContext().getSharedPreferences("spica_weather", Application.MODE_PRIVATE)
  }

  single<DataStoreUtil> {
    DataStoreUtil(androidContext())
  }

  single<AppDatabase> {
    Room.databaseBuilder(
      get<Application>(),
      AppDatabase::class.java,
      "spica_weather.db",
    )
      .addMigrations(MIGRATION_7_8)
      .fallbackToDestructiveMigration(false)
      .build()
  }

  single<CityDao>(createdAtStart = true) {
    get<AppDatabase>().cityDao()
  }
}
