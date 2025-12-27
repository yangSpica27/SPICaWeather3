package me.spica.spicaweather3

import android.app.Application
import com.baidu.location.LocationClient
import me.spica.spicaweather3.module.InjectModules
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.GlobalContext.startKoin

class App : Application() {

  override fun onCreate() {
    super.onCreate()
    initKoin()
  }


  private fun initKoin() {
    LocationClient.setAgreePrivacy(true);
    startKoin {
      androidLogger()
      androidContext(this@App)
      modules(
        InjectModules.networkModule,
        InjectModules.viewModelModules,
        InjectModules.persistenceModule,
        InjectModules.utilsModule
      )
    }
  }

}