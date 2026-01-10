package me.spica.spicaweather3.core.di

import me.spica.spicaweather3.data.repository.CityRepositoryImpl
import me.spica.spicaweather3.data.repository.WeatherRepositoryImpl
import me.spica.spicaweather3.domain.repository.ICityRepository
import me.spica.spicaweather3.domain.repository.IWeatherRepository
import me.spica.spicaweather3.domain.usecase.GetAllCitiesUseCase
import me.spica.spicaweather3.domain.usecase.LocationUseCase
import me.spica.spicaweather3.domain.usecase.ManageCitiesUseCase
import me.spica.spicaweather3.domain.usecase.RefreshWeatherUseCase
import me.spica.spicaweather3.domain.usecase.SearchCityUseCase
import me.spica.spicaweather3.ui.city_selector.CitySelectorViewModel
import me.spica.spicaweather3.ui.main.WeatherViewModel
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * ViewModel 依赖注入模块
 * 
 * 注册所有 ViewModel、Repository 和 UseCase 实例
 */
val viewModelModule = module {
  // Repository 实现
  single<IWeatherRepository> {
    WeatherRepositoryImpl(
      apiService = get(),
      cityDao = get(),
      stringProvider = get()
    )
  }
  
  single<ICityRepository> {
    CityRepositoryImpl(
      apiService = get(),
      stringProvider = get()
    )
  }
  
  // UseCase
  factory {
    GetAllCitiesUseCase(get())
  }
  
  factory {
    RefreshWeatherUseCase(get())
  }
  
  factory {
    ManageCitiesUseCase(get())
  }
  
  factory {
    LocationUseCase(get())
  }
  
  factory {
    SearchCityUseCase(get())
  }

  // ViewModels
  viewModel {
    CitySelectorViewModel(
      searchCityUseCase = get(),
      manageCitiesUseCase = get()
    )
  }

  viewModel {
    WeatherViewModel(
      getAllCitiesUseCase = get(),
      refreshWeatherUseCase = get(),
      manageCitiesUseCase = get(),
      locationUseCase = get(),
      dataStoreUtil = get()
    )
  }
}
