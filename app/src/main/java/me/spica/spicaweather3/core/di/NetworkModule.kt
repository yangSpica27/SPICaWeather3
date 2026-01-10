package me.spica.spicaweather3.core.di

import com.google.gson.GsonBuilder
import com.skydoves.sandwich.retrofit.adapters.ApiResponseCallAdapterFactory
import me.spica.spicaweather3.common.config.HefengConfig
import me.spica.spicaweather3.data.remote.api.ApiService
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import org.koin.dsl.module
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.util.concurrent.TimeUnit

/**
 * 网络层依赖注入模块
 * 
 * 提供 OkHttp、Retrofit、ApiService 等网络相关依赖
 */
val networkModule = module {
  single<OkHttpClient> {
    OkHttpClient.Builder()
      .addInterceptor(HttpLoggingInterceptor().setLevel(HttpLoggingInterceptor.Level.BODY))
      .retryOnConnectionFailure(true)
      .connectTimeout(3500L, TimeUnit.MILLISECONDS)
      .readTimeout(3000L, TimeUnit.MILLISECONDS)
      .callTimeout(3000L, TimeUnit.MILLISECONDS)
      .writeTimeout(3000L, TimeUnit.MILLISECONDS)
      .build()
  }

  single<Retrofit> {
    Retrofit.Builder()
      .client(get())
      .baseUrl(HefengConfig.BASE_URL)
      .addConverterFactory(
        GsonConverterFactory.create(
          GsonBuilder().serializeNulls().create()
        )
      )
      .addCallAdapterFactory(ApiResponseCallAdapterFactory.create())
      .build()
  }

  single<ApiService> {
    get<Retrofit>().create(ApiService::class.java)
  }
}
