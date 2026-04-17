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
import me.spica.spicaweather3.common.model.WeatherCardType
import me.spica.spicaweather3.core.constants.MainScreenConstants
import me.spica.spicaweather3.domain.model.City
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

  private val _dataFlow = getAllCitiesUseCase()

  @OptIn(FlowPreview::class)
  val weatherPageStates: StateFlow<List<WeatherPageState>> = _dataFlow
    .map { cities -> cities.map { city -> city.toWeatherPageState() } }
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

  fun swapSort(city1: City, city2: City) {
    viewModelScope.launch(context = Dispatchers.IO) {
      manageCitiesUseCase.swapCityOrder(city1, city2)
    }
  }

  fun insertUserLoc(bdLocation: BDLocation?, shouldRefresh: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      val userLoc = locationUseCase.getUserLocationCity()
      if (userLoc == null && bdLocation == null) {
        // 默认北京
        val city = City(
          id = java.util.UUID.randomUUID().toString(),
          name = "北京",
          latitude = "39.90498",
          longitude = "116.40528",
          administrativeArea1 = "北京市",
          administrativeArea2 = "北京市",
          sortOrder = -1,
          isUserLocation = true,
          weather = null
        )
        manageCitiesUseCase.addCity(city)
      } else if (bdLocation != null) {
        // 只保存定位信息，不在此处触发天气刷新
        locationUseCase.saveUserLocation(
          bdLocation = bdLocation,
          shouldRefresh = false,
          onError = { _error.value = it },
          onSucceed = { _error.value = null }
        )
      }
      // 定位解析完成后，统一走批量刷新路径（单次 insertAll，单次 Room 发射）
      if (shouldRefresh) {
        refresh()
      }
    }
  }


  fun deleteCity(
    city: City,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch(context = Dispatchers.IO) {
      manageCitiesUseCase.deleteCity(city)
      onSuccess()
    }
  }

  fun refresh() {
    if (_isRefreshing.value) return
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
          lastRefreshedCityIds = weatherPageStates.value.map { it.city.id }
        }
      )
    }
  }

  /**
   * 根据天气数据过滤可显示的卡片
   * 
   * @param configs 所有卡片配置
   * @param animType 当前天气动画类型
   * @param hasAlerts 是否有天气预警
   * @return 过滤后的可显示卡片列表
   */
  fun getFilteredCardsForWeather(
    configs: List<WeatherCardConfig>,
    animType: WeatherAnimType,
    hasAlerts: Boolean
  ): List<WeatherCardConfig> {
    return configs.filter { config ->
      when (config.cardType) {
        // 分钟级降水预报仅在有雨的场景下显示
        WeatherCardType.MINUTELY -> animType.showRain
        // 天气预警仅在有预警数据时显示
        WeatherCardType.ALERT -> hasAlerts
        // 其他卡片默认显示
        else -> true
      }
    }
  }

  /**
   * 更新卡片顺序
   * 
   * @param reorderedCards 重新排序后的卡片列表
   */
  fun reorderCards(reorderedCards: List<WeatherCardConfig>) {
    viewModelScope.launch(Dispatchers.IO) {
      dataStoreUtil.updateCardsOrder(reorderedCards)
    }
  }


}

/**
 * 将 City 转换为 WeatherPageState
 */
fun City.toWeatherPageState(): WeatherPageState {
  return if (weather != null) {
    WeatherPageState.Data(this)
  } else {
    WeatherPageState.Empty(this)
  }
}
