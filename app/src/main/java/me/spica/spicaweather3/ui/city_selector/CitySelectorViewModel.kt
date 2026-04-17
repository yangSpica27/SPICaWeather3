package me.spica.spicaweather3.ui.city_selector

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.data.mapper.toCity
import me.spica.spicaweather3.domain.model.SearchLocation
import me.spica.spicaweather3.domain.usecase.ManageCitiesUseCase
import me.spica.spicaweather3.domain.usecase.SearchCityUseCase

class CitySelectorViewModel(
  private val searchCityUseCase: SearchCityUseCase,
  private val manageCitiesUseCase: ManageCitiesUseCase
) : ViewModel() {

  // 用于持有当前城市搜索请求的 Job
  private var searchJob: Job? = null

  private var topsTopJob: Job? = null

  private val _topCities = MutableStateFlow<List<SearchLocation>>(arrayListOf())
  val topCities: StateFlow<List<SearchLocation>> = _topCities

  private val _errorMessage = MutableStateFlow<String?>(null)

  val errorMessage: StateFlow<String?> = _errorMessage

  private val _searchResult = MutableStateFlow<List<SearchLocation>>(arrayListOf())
  val searchResult: StateFlow<List<SearchLocation>> = _searchResult

  init {
    topsTopJob = viewModelScope.launch(Dispatchers.IO) {
      searchCityUseCase.getTopCities(
        onError = {
          _errorMessage.value = it
        },
        onSucceed = {
          _topCities.value = it
        }
      )
    }
  }

  fun saveLocation(location: SearchLocation,
                   onSucceed: () -> Unit) {
    viewModelScope.launch(Dispatchers.IO) {
      manageCitiesUseCase.addCity(location.toCity())
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
      searchCityUseCase(
        keyword = query,
        onError = {
          _errorMessage.value = it
        },
        onSucceed = {
          _searchResult.value = it
        }
      )
    }
  }

  override fun onCleared() {
    super.onCleared()
    topsTopJob?.cancel()
    searchJob?.cancel()
  }

}
