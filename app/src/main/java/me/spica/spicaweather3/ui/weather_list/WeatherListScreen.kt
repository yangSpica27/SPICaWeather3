package me.spica.spicaweather3.ui.weather_list

import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import me.spica.spicaweather3.data.local.db.entity.CityEntity
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.route.Routes
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import org.koin.compose.viewmodel.koinActivityViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.utils.overScrollVertical

/**
 * 城市管理页面
 * 
 * 展示已添加的城市列表，支持拖拽排序和删除操作。
 * 包含搜索栏、删除确认对话框、灭霸消散效果等功能。
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WeatherListScreen() {
  val navController = LocalNavController.current
  val scrollBehavior = MiuixScrollBehavior()
  val viewModel = koinActivityViewModel<WeatherViewModel>()
  val hapticFeedback = LocalHapticFeedback.current

  // ==================== 状态管理 ====================
  val trackedCities = viewModel.weatherPageStates.collectAsStateWithLifecycle().value
  var tempList by remember { mutableStateOf(listOf<WeatherPageState>()) }
  var isDrag by remember { mutableStateOf(false) }
  val showDialog = remember { mutableStateOf(false) }
  var selectedCity by remember { mutableStateOf<CityEntity?>(null) }
  var disintegratingCityId by remember { mutableStateOf<Long?>(null) }

  // 监听城市列表变化，非拖拽状态时同步到临时列表
  LaunchedEffect(trackedCities) {
    if (!isDrag) {
      tempList = trackedCities
    }
  }

  val listHazeState = rememberHazeState()

  // ==================== UI 布局 ====================
  Scaffold(
    topBar = {
      WeatherListTopBar(
        scrollBehavior = scrollBehavior,
        hazeState = listHazeState,
        onNavigateBack = { navController.popBackStack() },
        onSearchClick = { navController.navigate(Routes.CitySelect) }
      )
    }
  ) { paddingValues ->

    // 删除城市确认对话框
    DeleteCityDialog(
      cityName = selectedCity?.name.orEmpty(),
      show = showDialog,
      onConfirm = {
        selectedCity?.let { city ->
          disintegratingCityId = city.id.hashCode().toLong()
        }
      },
      onDismiss = { showDialog.value = false }
    )

    // ==================== 可拖拽排序列表 ====================
    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }

    // 拖拽排序状态，限制首个 item 不参与拖拽
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
      if (from.index == 0 || to.index == 0) return@rememberReorderableLazyListState

      val city1 = tempList[from.index].cityEntity
      val city2 = tempList[to.index].cityEntity
      viewModel.swapSort(city1, city2)
      
      tempList = tempList.toMutableList().apply {
        add(to.index, removeAt(from.index))
      }
      
      hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyColumn(
      state = listState,
      modifier = Modifier
        .hazeSource(listHazeState)
        .fillMaxSize()
        .padding(paddingValues)
        .nestedScroll(scrollBehavior.nestedScrollConnection)
        .overScrollVertical(),
      contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
      overscrollEffect = null
    ) {
      itemsIndexed(
        items = tempList,
        key = { _, city -> city.cityEntity.id }
      ) { index, item ->
        ReorderableItem(
          reorderableLazyListState,
          key = item.cityEntity.id,
          enabled = index != 0
        ) {
          WeatherListItem(
            item = item,
            index = index,
            isDrag = isDrag,
            disintegratingCityId = disintegratingCityId,
            onDragStart = { isDrag = true },
            onDragStop = { isDrag = false },
            onClick = {
              if (!item.cityEntity.isUserLoc && disintegratingCityId == null) {
                selectedCity = item.cityEntity
                showDialog.value = true
              }
            },
            onDisintegrationComplete = {
              selectedCity?.let { city ->
                viewModel.deleteCity(city)
              }
              disintegratingCityId = null
              selectedCity = null
            }
          )
        }
      }
    }
  }
}