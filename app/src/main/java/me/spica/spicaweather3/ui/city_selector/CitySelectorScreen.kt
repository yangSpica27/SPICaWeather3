package me.spica.spicaweather3.ui.city_selector

import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.itemsIndexed
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.ui.LocalAnimatedContentScope
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.utils.noRippleClickable
import org.koin.androidx.compose.koinViewModel
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.HorizontalDivider
import top.yukonga.miuix.kmp.basic.InputField
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.SearchBar
import top.yukonga.miuix.kmp.basic.SmallTitle
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import top.yukonga.miuix.kmp.utils.pressable
import kotlin.random.Random


@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun CitySelectorScreen() {

  var searchText by rememberSaveable { mutableStateOf("") }

  val viewModel = koinViewModel<CitySelectorViewModel>()

  val errorMessage = viewModel.errorMessage.collectAsStateWithLifecycle()

  val context = LocalContext.current

  LaunchedEffect(errorMessage.value) {
    if (errorMessage.value?.isNotEmpty() == false) {
      Toast.makeText(context, "${errorMessage.value}", Toast.LENGTH_SHORT).show()
    }
  }

  LaunchedEffect(searchText) {
    viewModel.searchCities(searchText)
  }

  Scaffold {
    Column(
      modifier = Modifier
        .fillMaxSize()
        .padding(it),
    ) {
      // 顶部搜索栏：与城市管理页共享转场，实现统一的搜索入口体验
      SearchBar(
        inputField = {
          with(LocalSharedTransitionScope.current) {
            InputField(
              label = stringResource(R.string.city_selector_input_hint),
              modifier = Modifier
                .fillMaxWidth()
                .sharedBounds(
                  rememberSharedContentState("search_bar"),
                  animatedVisibilityScope = LocalAnimatedContentScope.current
                ),
              query = searchText,
              onQueryChange = { txt -> searchText = txt },
              onSearch = {},
              expanded = false,
              onExpandedChange = {},
            )
          }
        }, onExpandedChange = {}
      ) {
        TopCities()
      }

      // 根据输入内容动态切换展示热门城市或搜索结果
      AnimatedContent(
        searchText, modifier = Modifier
          .weight(1f)
          .fillMaxWidth()
      ) { searchText ->
        if (searchText.isEmpty()) {
          TopCities()
        } else {
          ListSelector()
        }
      }
    }
  }
}

@Composable
private fun ListSelector() {
  val viewModel: CitySelectorViewModel = koinViewModel()
  val cities = viewModel.searchResult.collectAsStateWithLifecycle()
  val navController = LocalNavController.current

  val weatherViewModel = koinActivityViewModel<WeatherViewModel>()

  // 搜索结果列表：按输入关键字实时返回城市并支持点击添加
  LazyColumn(
    modifier = Modifier
      .fillMaxWidth()
      .overScrollVertical()
      .padding(horizontal = 16.dp),
    contentPadding = PaddingValues(bottom = 48.dp, top = 20.dp)
  ) {

    itemsIndexed(cities.value, key = { _, item -> item.id }) { index, item ->
      Column(
        modifier = Modifier
          .fillMaxWidth()
          .pressable(delay = 0)
          .noRippleClickable {
            viewModel.saveLocation(item, onSucceed = {
              weatherViewModel.refresh()
              navController.popBackStack()
            })
          }
      ) {
        if (index != 0) {
          HorizontalDivider()
        }
        Spacer(modifier = Modifier.height(8.dp))
        Text(item.name, style = MiuixTheme.textStyles.subtitle)
        Spacer(modifier = Modifier.height(8.dp))
        Text("${item.adm2}, ${item.adm1}", style = MiuixTheme.textStyles.body1)
        Spacer(modifier = Modifier.height(8.dp))
      }
    }

  }

}

@Composable
private fun TopCities(modifier: Modifier = Modifier) {
  val viewModel: CitySelectorViewModel = koinViewModel()
  val tops = viewModel.topCities.collectAsStateWithLifecycle()
  val navController = LocalNavController.current
  val weatherViewModel = koinActivityViewModel<WeatherViewModel>()
  // 热门城市宫格：展示预置热门城市，便于快速选择
  Column(
    modifier = modifier
      .fillMaxWidth()
      .padding(horizontal = 16.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    Spacer(modifier = Modifier.height(10.dp))
    SmallTitle(stringResource(R.string.city_selector_hot_cities))
    LazyVerticalGrid(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      columns = GridCells.Fixed(3),
      verticalArrangement = Arrangement.spacedBy(12.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      itemsIndexed(tops.value, key = { _, location -> location.id }) { _, location ->
        val density = LocalDensity.current
        val appearAnim = remember(location.id) { Animatable(0f) }
        val delayMillis = remember(location.id) { Random.nextInt(0, 200) }
        val durationMillis = remember(location.id) { Random.nextInt(260, 520) }

        LaunchedEffect(location.id) {
          appearAnim.snapTo(0f)
          appearAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
              durationMillis = durationMillis,
              delayMillis = delayMillis
            )
          )
        }

        val progress = appearAnim.value
        val translationY = with(density) { 36.dp.toPx() * (1f - progress) }
        val scale = 0.5f + 0.5f * progress

        Box(
          modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer {
              this.translationY = translationY
              scaleX = scale
              scaleY = scale
              alpha = progress
            }
            .pressable(delay = 0)
            .background(
              MiuixTheme.colorScheme.surfaceContainerHigh, ContinuousRoundedRectangle(8.dp)
            )
            .noRippleClickable {
              viewModel.saveLocation(location = location, onSucceed = {
                weatherViewModel.refresh()
                navController.popBackStack()
              })
            }
            .padding(
              vertical = 12.dp
            ),
          contentAlignment = Alignment.Center,
        ) {
          Text(location.name)
        }
      }
    }
  }
}
