package me.spica.spicaweather3

import android.app.Application
import androidx.work.Configuration
import com.baidu.location.LocationClient
import me.spica.spicaweather3.core.di.networkModule
import me.spica.spicaweather3.core.di.persistenceModule
import me.spica.spicaweather3.core.di.utilsModule
import me.spica.spicaweather3.core.di.viewModelModule
import me.spica.spicaweather3.data.work.WeatherRefreshScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class App : Application(), Configuration.Provider {


  override val workManagerConfiguration: Configuration
    get() = Configuration.Builder().build()

  companion object {
    lateinit var instance: App
      private set
  }

  override fun onCreate() {
    super.onCreate()
    initKoin()
    instance = this
    WeatherRefreshScheduler.enqueue(this)
  }


  private fun initKoin() {
    // 必须先设置百度定位隐私同意，再初始化 Koin
    LocationClient.setAgreePrivacy(true)
    startKoin {
      androidLogger()
      androidContext(this@App)
      modules(
        networkModule,
        persistenceModule,
        viewModelModule,
        utilsModule
      )
    }
  }

}
