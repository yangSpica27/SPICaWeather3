package me.spica.spicaweather3.core.di

import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import me.spica.spicaweather3.data.local.db.AppDatabase
import me.spica.spicaweather3.data.local.db.dao.CityDao
import me.spica.spicaweather3.utils.DataStoreUtil
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

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
      .fallbackToDestructiveMigration(false)
      .build()
  }

  single<CityDao>(createdAtStart = true) {
    get<AppDatabase>().cityDao()
  }
}
