package me.spica.spicaweather3.module

import android.app.Application
import android.content.SharedPreferences
import androidx.room.Room
import com.google.gson.GsonBuilder
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import me.spica.spicaweather3.db.AppDatabase
import me.spica.spicaweather3.db.PersistenceRepository
import me.spica.spicaweather3.db.dao.CityDao
import me.spica.spicaweather3.network.ApiRepository
import me.spica.spicaweather3.network.ApiService
import me.spica.spicaweather3.ui.city_selector.CitySelectorViewModel
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.utils.DataStoreUtil
import me.spica.spicaweather3.utils.LocationHelper
import me.spica.spicaweather3.utils.StringProvider
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

object InjectModules {

  /**
   * 网络的注入
   */
  val networkModule = module {
    single<OkHttpClient> {
      OkHttpClient.Builder()
        .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
        .retryOnConnectionFailure(true).connectTimeout(3500L, TimeUnit.MILLISECONDS)
        .readTimeout(3000L, TimeUnit.MILLISECONDS).callTimeout(3000L, TimeUnit.MILLISECONDS)
        .writeTimeout(3000L, TimeUnit.MILLISECONDS)
        .build()
    }
    single<Retrofit> {

      Retrofit.Builder().client(get()).baseUrl("https://n85egdbbrr.re.qweatherapi.com/")
        .addConverterFactory(
          GsonConverterFactory.create(
            GsonBuilder().serializeNulls().create()
          )
        ).addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
        .build()
    }
    single<ApiService> {
      get<Retrofit>().create(ApiService::class.java)
    }
    single<ApiRepository> {
      ApiRepository(get<ApiService>(), get(), get())
    }
  }

  val viewModelModules = module {
    viewModel {
      CitySelectorViewModel(get(), get())
    }
    viewModel {
      WeatherViewModel(
        persistenceRepository = get(),
        apiRepository = get(),
        dataStoreUtil = get(),
      )
    }
  }

  val utilsModule = module {
    single<LocationHelper> {
      LocationHelper(androidContext())
    }
    single<StringProvider> {
      StringProvider(androidContext())
    }
  }

  val persistenceModule = module {
    single<SharedPreferences> {
      androidContext().getSharedPreferences("spica_weather", Application.MODE_PRIVATE)
    }
    single<DataStoreUtil>{
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

    single<PersistenceRepository>(
      createdAtStart = true,
    ) {
      PersistenceRepository(get())
    }

  }

}