package me.spica.spicaweather3.module

import me.spica.spicaweather3.utils.LocationHelper
import me.spica.spicaweather3.utils.StringProvider
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

/**
 * 工具类依赖注入模块
 * 
 * 提供 LocationHelper、StringProvider 等工具类依赖
 */
val utilsModule = module {
  single<LocationHelper> {
    LocationHelper(androidContext())
  }

  single<StringProvider> {
    StringProvider(androidContext())
  }
}
