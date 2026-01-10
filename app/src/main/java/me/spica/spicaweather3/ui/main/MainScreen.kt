package me.spica.spicaweather3.ui.main

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ContentTransform
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PagerState
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy
import com.kyant.backdrop.highlight.Highlight
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.route.Routes
import me.spica.spicaweather3.presentation.theme.MAIN_PLUS_BUTTON_SIZE
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.main.weather.WeatherPage
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.ui.widget.MainTopBar
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import me.spica.spicaweather3.ui.widget.materialSharedAxisXIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisXOut
import me.spica.spicaweather3.ui.widget.materialSharedAxisZIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZOut
import me.spica.spicaweather3.utils.noRippleClickable
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.PullToRefresh
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 主屏幕 - 展示天气信息的主要界面
 * 支持多城市横向滑动浏览，共享元素转场动画
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun MainScreen() {
  // ==================== 状态管理 ====================
  // 底部弹窗显示状态
  val showBottomSheet = rememberSaveable { mutableStateOf(false) }

  // ViewModel 实例
  val viewModel = koinActivityViewModel<WeatherViewModel>()

  // 收集 ViewModel 状态
  val isRefreshing = viewModel.isRefreshing.collectAsStateWithLifecycle()
  val weatherPageStates = viewModel.weatherPageStates.collectAsStateWithLifecycle()

  // ==================== Pager 状态管理 ====================
  // 横向分页器状态，页数等于城市数量
  val pagerState = rememberPagerState {
    weatherPageStates.value.size
  }

  // 监听初始索引变化，用于从其他页面返回时定位
  val initIndex = viewModel.initIndex.collectAsStateWithLifecycle()
  LaunchedEffect(initIndex.value) {
    pagerState.requestScrollToPage(initIndex.value)
  }

  // ==================== 派生状态 ====================
  // 当前页面的天气数据
  val currentPageData = remember(pagerState.currentPage, weatherPageStates.value) {
    derivedStateOf {
      weatherPageStates.value.getOrNull(pagerState.currentPage)
    }
  }

  // 当前城市信息
  val currentCity = remember(currentPageData.value) {
    derivedStateOf {
      currentPageData.value?.cityEntity
    }
  }.value

  // ==================== 副作用 ====================
  // 根据当前天气更新背景动画类型
  LaunchedEffect(currentPageData.value) {
    val pageData = currentPageData.value
    if (pageData is WeatherPageState.Data) {
      val iconId = pageData.cityEntity.weather?.current?.icon?:"100"
      viewModel.weatherAnimType.value = WeatherAnimType.getAnimType(iconId)
    }
  }

  // ==================== UI 相关状态 ====================
  // 滚动行为控制器
  val scrollBehavior = MiuixScrollBehavior()

  // 导航控制器
  val navigator = LocalNavController.current

  // 毛玻璃效果状态
  val backdrop = rememberLayerBackdrop()

  val refreshTexts = listOf(
    stringResource(R.string.refresh_pull_down),
    stringResource(R.string.refresh_release),
    stringResource(R.string.refresh_refreshing),
    stringResource(R.string.refresh_complete)
  )

  // ==================== UI 布局 ====================
  with(LocalSharedTransitionScope.current) {
    Scaffold(
      modifier = Modifier
        .fillMaxSize(),
      // 浮动添加按钮 - 跳转到城市列表
      floatingActionButton = {
        AddCityButton(
          backdrop = backdrop,
          onClick = { navigator.navigate(Routes.WeatherList) }
        )
      },
      // 顶部标题栏
      topBar = {
        MainTopBar(
          modifier = Modifier
            .drawPlainBackdrop(
              backdrop = backdrop,
              shape = { RoundedCornerShape(0.dp) },
              effects = {
                vibrancy()
                blur(8.dp.toPx())
                this.colorControls(
                  saturation = 1.6f,
                  brightness = 0.3f
                )
              },
            )
            .fillMaxWidth(),
          scrollBehavior = scrollBehavior,
          // 大标题（展开状态）
          largeTitle = {
            AnimatedCityTitle(
              cityName = currentCity?.name,
              transitionSpec = {
                materialSharedAxisXIn(true) togetherWith materialSharedAxisXOut(true)
              },
              onClick = { showBottomSheet.value = true }
            )
          },
          // 小标题（折叠状态）
          title = {
            AnimatedCityTitle(
              cityName = currentCity?.name,
              transitionSpec = {
                materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(true)
              },
              onClick = { showBottomSheet.value = true }
            )
          }
        )
      }
    ) { paddingValues ->

      // 主内容区域
      Box(
        modifier = Modifier
          .layerBackdrop(backdrop) // 毛玻璃效果源
          .fillMaxSize()
      ) {
        // 下拉刷新容器
        PullToRefresh(
          isRefreshing = isRefreshing.value,
          onRefresh = { viewModel.refresh() },
          contentPadding = PaddingValues(top = 120.dp),
          color = MiuixTheme.colorScheme.onSurface,
          topAppBarScrollBehavior = scrollBehavior,
          refreshTexts = refreshTexts
        ) {
          // 横向分页器 - 多城市滑动浏览
          WeatherPager(
            pagerState = pagerState,
            weatherPageStates = weatherPageStates.value,
            scrollBehavior = scrollBehavior,
            paddingValues = paddingValues
          )
        }
      }
    }
  }
}

/**
 * 添加城市按钮组件
 */
@Composable
private fun AddCityButton(
  backdrop: LayerBackdrop,
  onClick: () -> Unit
) {
 with(LocalSharedTransitionScope.current){
   val glassColor = MiuixTheme.colorScheme.onSurface.copy(alpha = .1f)
   Box(
     modifier = Modifier
       .size(MAIN_PLUS_BUTTON_SIZE)
       .noRippleClickable(onClick = onClick)
       .clip(CircleShape)
       .drawBackdrop(
         highlight = {
           Highlight.Plain
         },
         backdrop = backdrop,
         shape = { CircleShape },
         onDrawSurface = {
           drawCircle(
             color = glassColor,
             radius = size.minDimension / 2f
           )
         },
         effects = {
           vibrancy()
           blur(8.dp.toPx())
           this.colorControls(
             saturation = 1.6f,
             brightness = 0.3f
           )
           lens(
             12f.dp.toPx(),
             22f.dp.toPx(),
             chromaticAberration = true
           )
         }
       ),
     contentAlignment = Alignment.Center
   ) {
     Icon(
       imageVector = Icons.Filled.Add,
      contentDescription = stringResource(R.string.cd_add_city),
       tint = MiuixTheme.colorScheme.onSurface,
       modifier = Modifier.size(32.dp)
     )
   }
 }
}

/**
 * 动画城市标题组件
 */
@Composable
private fun AnimatedCityTitle(
  cityName: String?,
  transitionSpec: () -> ContentTransform = {
    (fadeIn(animationSpec = tween(220, delayMillis = 90)) +
        scaleIn(initialScale = 0.92f, animationSpec = tween(220, delayMillis = 90)))
      .togetherWith(fadeOut(animationSpec = tween(90)))
  },
  onClick: () -> Unit
) {
  AnimatedContent(
    targetState = cityName,
    contentKey = { it.toString() },
    transitionSpec = { transitionSpec() }
  ) { name ->
    val fallbackName = stringResource(R.string.main_city_unknown)
    Text(
      text = name ?: fallbackName,
      color = MiuixTheme.colorScheme.onSurface,
      fontSize = 22.sp,
      modifier = Modifier
        .noRippleClickable(onClick = onClick)
        .padding(vertical = 22.dp)
    )
  }
}

/**
 * 天气分页器组件 - 多城市横向滑动
 */
@Composable
private fun WeatherPager(
  pagerState: PagerState,
  weatherPageStates: List<WeatherPageState>,
  scrollBehavior: ScrollBehavior,
  paddingValues: PaddingValues
) {
  HorizontalPager(
    state = pagerState,
    modifier = Modifier.fillMaxSize(),
    beyondViewportPageCount = 0,
    pageContent = { currentIndex ->
      // 仅在目标页面时显示内容，优化性能
      if (weatherPageStates.isNotEmpty()) {
        ShowOnIdleContent(visible = currentIndex == pagerState.targetPage) {
          WeatherPage(
            weatherPageStates[currentIndex],
            scrollBehavior = scrollBehavior,
            modifier = Modifier
              .fillMaxSize()
              .padding(paddingValues = paddingValues)
          )
        }
      }

    }
  )
}



