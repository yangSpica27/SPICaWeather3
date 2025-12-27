package me.spica.spicaweather3.ui.city_selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.db.PersistenceRepository
import me.spica.spicaweather3.network.ApiRepository
import me.spica.spicaweather3.network.model.Location

class CitySelectorViewModel(
  private val apiRepository: ApiRepository,
  private val persistenceRepository: PersistenceRepository
) : ViewModel() {

  // 用于持有当前城市搜索请求的 Job
  private var searchJob: Job? = null

  private var topsTopJob: Job? = null

  private val _topCities = MutableStateFlow<List<Location>>(arrayListOf())
  val topCities: StateFlow<List<Location>> = _topCities

  private val _errorMessage = MutableStateFlow<String?>(null)

  val errorMessage: StateFlow<String?> = _errorMessage

  private val _searchResult = MutableStateFlow<List<Location>>(arrayListOf())
  val searchResult: StateFlow<List<Location>> = _searchResult

  init {
    topsTopJob = viewModelScope.launch(Dispatchers.IO) {
      apiRepository.fetchTop(
        onError = {
          _errorMessage.value = it
        },
        onSucceed = {
          _topCities.value = it
        }
      )
    }
  }

  fun saveLocation(location: Location,
                   onSucceed: () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      persistenceRepository.insertCity(location.toCity())
      withContext(Dispatchers.Main){
        onSucceed()
      }
    }
  }

  fun searchCities(query: String) {
    searchJob?.cancel()
    if (query.isEmpty()) {
      _searchResult.value = arrayListOf()
      return
    }
    searchJob = viewModelScope.launch(Dispatchers.IO) {
      apiRepository.fetchCity(query, onError = {
        _errorMessage.value = it
      }, onSucceed = {
        _searchResult.value = it
      })
    }
  }

  override fun onCleared() {
    super.onCleared()
    topsTopJob?.cancel()
    searchJob?.cancel()
  }

}
