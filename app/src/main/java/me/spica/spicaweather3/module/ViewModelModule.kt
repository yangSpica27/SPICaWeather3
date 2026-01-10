package me.spica.spicaweather3.module

import me.spica.spicaweather3.ui.city_selector.CitySelectorViewModel
import me.spica.spicaweather3.ui.main.WeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel 依赖注入模块
 * 
 * 注册所有 ViewModel 实例
 */
val viewModelModule = module {
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
