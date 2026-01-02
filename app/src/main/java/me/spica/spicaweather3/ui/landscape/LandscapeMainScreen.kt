package me.spica.spicaweather3.ui.landscape

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.material3.VerticalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica.spicaweather3.ui.main.WeatherViewModel
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 横屏主布局 - 左右双栏结构
 * 左栏：城市列表
 * 右栏：天气卡片/详情页
 */
@Composable
fun LandscapeMainScreen() {
  val viewModel = koinActivityViewModel<WeatherViewModel>()
  val weatherPageStates = viewModel.weatherPageStates.collectAsStateWithLifecycle()

  // 当前选中的城市索引（使用 rememberSaveable 保持横竖屏切换时的状态）
  val selectedCityIndex = rememberSaveable { mutableIntStateOf(0) }

  Scaffold(
    modifier = Modifier
      .fillMaxSize()
      .background(MiuixTheme.colorScheme.surface)
  ) {
    Row(
      modifier = Modifier.fillMaxSize()
    ) {
      // 左栏 - 城市列表 (固定宽度 280dp)
      CityListPane(
        cities = weatherPageStates.value,
        selectedIndex = selectedCityIndex.intValue,
        onCitySelected = { index ->
          selectedCityIndex.intValue = index
        },
        modifier = Modifier
          .width(280.dp)
          .fillMaxHeight()
      )

      // 分隔线
      VerticalDivider(
        color = MiuixTheme.colorScheme.outline.copy(alpha = 0.3f)
      )

      // 右栏 - 内容区（卡片/详情）
      WeatherContentPane(
        cityState = weatherPageStates.value.getOrNull(selectedCityIndex.intValue),
        modifier = Modifier
          .weight(1f)
          .fillMaxHeight()
      )
    }
  }
}
