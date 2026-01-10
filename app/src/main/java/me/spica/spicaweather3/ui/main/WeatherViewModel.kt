package me.spica.spicaweather3.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.baidu.location.BDLocation
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.common.model.WeatherCardConfig
import me.spica.spicaweather3.data.local.db.entity.CityEntity
import me.spica.spicaweather3.domain.usecase.GetAllCitiesUseCase
import me.spica.spicaweather3.domain.usecase.LocationUseCase
import me.spica.spicaweather3.domain.usecase.ManageCitiesUseCase
import me.spica.spicaweather3.domain.usecase.RefreshWeatherUseCase
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.utils.DataStoreUtil

class WeatherViewModel(
  private val getAllCitiesUseCase: GetAllCitiesUseCase,
  private val refreshWeatherUseCase: RefreshWeatherUseCase,
  private val manageCitiesUseCase: ManageCitiesUseCase,
  private val locationUseCase: LocationUseCase,
  val dataStoreUtil: DataStoreUtil
) : ViewModel() {

  // 上次刷新时间戳
  private var lastRefreshTime: Long = 0L

  // 上次刷新时的城市列表快照（使用城市ID列表进行比较）
  private var lastRefreshedCityIds: List<String> = emptyList()

  // 刷新间隔阈值（毫秒）
  private companion object {
    const val REFRESH_INTERVAL_MS = 3000L
  }

  private val _dataFlow = getAllCitiesUseCase()

  @OptIn(FlowPreview::class)
  val weatherPageStates: StateFlow<List<WeatherPageState>> = _dataFlow
    .map { value -> value.map { cityEntity -> cityEntity.toWeatherData() } }
    .conflate()
    .distinctUntilChanged()
    .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())


  val cardsConfig: StateFlow<List<WeatherCardConfig>> =
    dataStoreUtil.getWeatherCardsConfig().conflate()
      .stateIn(viewModelScope, SharingStarted.Eagerly, emptyList())

  private val _isRefreshing = MutableStateFlow(false)

  val isRefreshing: StateFlow<Boolean> = _isRefreshing

  private val _error = MutableStateFlow<String?>(null)

  val error: StateFlow<String?> = _error.asStateFlow()

  val weatherAnimType = MutableStateFlow<WeatherAnimType>(WeatherAnimType.CloudLight)

  val initIndex = MutableStateFlow(0)

  fun swapSort(city1: CityEntity, city2: CityEntity) {
    viewModelScope.launch(context = Dispatchers.IO) {
      manageCitiesUseCase.swapCityOrder(city1, city2)
    }
  }

  fun insertUserLoc(bdLocation: BDLocation?, shouldRefresh: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      val userLoc = locationUseCase.getUserLocationCity()
      if (userLoc == null && bdLocation == null) {
        // 默认北京
        val city = CityEntity(
          name = "北京",
          lat = "39.90498",
          lon = "116.40528",
          adm1 = "北京市",
          adm2 = "北京市",
          sort = -1,
          isUserLoc = true
        )
        manageCitiesUseCase.addCity(city)
        if (shouldRefresh) {
          refresh()
        }
      } else if (bdLocation != null) {
        locationUseCase.saveUserLocation(
          bdLocation = bdLocation,
          shouldRefresh = shouldRefresh,
          onError = { _error.value = it },
          onSucceed = { if (!shouldRefresh) _error.value = null }
        )
      }
    }
  }


  fun deleteCity(
    city: CityEntity,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch(context = Dispatchers.IO) {
      manageCitiesUseCase.deleteCity(city)
      onSuccess()
    }
  }

  fun refresh() {
    _isRefreshing.update { true }
    viewModelScope.launch(Dispatchers.IO) {
      val cities = locationUseCase.getAllCitiesFlow()
      refreshWeatherUseCase.refreshAllCities(
        cities = cities,
        onError = {
          _error.value = it
          _isRefreshing.update { false }
        },
        onSucceed = {
          _error.value = null
          _isRefreshing.update { false }
          // 刷新成功后更新时间戳和城市列表快照
          lastRefreshTime = System.currentTimeMillis()
          lastRefreshedCityIds = weatherPageStates.value.map { it.cityEntity.id }
        }
      )
    }
  }


}
