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
import kotlinx.coroutines.launch
import me.spica.spicaweather3.common.WeatherAnimType
import me.spica.spicaweather3.common.WeatherCardConfig
import me.spica.spicaweather3.db.PersistenceRepository
import me.spica.spicaweather3.db.entity.CityEntity
import me.spica.spicaweather3.network.ApiRepository
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.utils.DataStoreUtil

class WeatherViewModel(
  private val apiRepository: ApiRepository,
  private val persistenceRepository: PersistenceRepository,
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

  private val _dataFlow = persistenceRepository.getAllCitiesFlow()

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
      persistenceRepository.swapSort(city1, city2)
    }
  }

  fun insertUserLoc(bdLocation: BDLocation?, shouldRefresh: Boolean = false) {
    viewModelScope.launch(Dispatchers.IO) {
      val userLoc = persistenceRepository.getUserLoc()
      if (userLoc == null) {
        val city = CityEntity(
          name = "北京",
          lat = "39.90498",
          lon = "116.40528",
          adm1 = "北京市",
          adm2 = "北京市",
          sort = -1,
          isUserLoc = true
        )
        persistenceRepository.insertUserLoc(cityEntity = city)
        if (shouldRefresh) {
          refresh()
        }
      } else if (bdLocation != null) {
        val city = CityEntity(
          name = "${bdLocation.street}",
          lat = "%.2f".format(bdLocation.latitude),
          lon = "%.2f".format(bdLocation.longitude),
          adm1 = bdLocation.city,
          adm2 = "${bdLocation.street}",
          sort = -1,
          isUserLoc = true
        )
        persistenceRepository.insertUserLoc(cityEntity = city)
        if (shouldRefresh) {
          refresh()
        }
      }
    }
  }


  fun deleteCity(
    city: CityEntity,
    onSuccess: () -> Unit = {}
  ) {
    viewModelScope.launch(context = Dispatchers.IO) {
      persistenceRepository.deleteCity(cityEntity = city)
      onSuccess()
    }
  }

  private var refreshJob: kotlinx.coroutines.Job? = null

  fun refresh() {
    val currentTime = System.currentTimeMillis()
    val currentCityIds = weatherPageStates.value.map { it.cityEntity.id }

    // 如果城市列表没有变化，且距离上次刷新不超过3秒，跳过刷新
    if (currentCityIds == lastRefreshedCityIds &&
      currentTime - lastRefreshTime < REFRESH_INTERVAL_MS
    ) {
      _isRefreshing.value = false
      return
    }
    _isRefreshing.value = true
    refreshJob?.cancel(java.util.concurrent.CancellationException())
    refreshJob = viewModelScope.launch {
      apiRepository.fetchWeather(
        onError = {
          _error.value = it
          _isRefreshing.value = false
        },
        onSucceed = {
          _isRefreshing.value = false
          _error.value = null
          // 刷新成功后更新时间戳和城市列表快照
          lastRefreshTime = System.currentTimeMillis()
          lastRefreshedCityIds = weatherPageStates.value.map { it.cityEntity.id }
        }
      )
    }
  }


}
