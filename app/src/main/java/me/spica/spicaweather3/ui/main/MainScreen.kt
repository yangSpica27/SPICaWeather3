@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

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
import androidx.compose.foundation.layout.calculateEndPadding
import androidx.compose.foundation.layout.calculateStartPadding
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
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.TopAppBarScrollBehavior
import androidx.compose.material3.adaptive.WindowAdaptiveInfo
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.PullToRefreshDefaults
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeInput
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.glass.GlassStyle
import dev.chrisbanes.haze.glass.hazeGlass
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.rememberHazeState
import kotlinx.coroutines.launch
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.core.constants.MainScreenConstants
import me.spica.spicaweather3.presentation.theme.MAIN_PLUS_BUTTON_SIZE
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.route.Routes
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.adaptive.AdaptiveWeatherLayout
import me.spica.spicaweather3.ui.adaptive.CityListPane
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

/**
 * 主屏幕 - 展示天气信息的主要界面
 * 支持多城市横向滑动浏览，共享元素转场动画
 */
@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalHazeApi::class)
@Composable
fun MainScreen(
    windowAdaptiveInfo: WindowAdaptiveInfo,
    modifier: Modifier = Modifier,
) {
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
    val initialCityIndex = viewModel.initIndex.collectAsStateWithLifecycle()
    // 只提供初始位置，窗口变化和返回页面时由 PagerState 恢复当前城市。
    val pagerState = rememberPagerState(initialPage = initialCityIndex.value) {
        weatherPageStates.value.size
    }
    val coroutineScope = rememberCoroutineScope()

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
            currentPageData.value?.city
        }
    }.value

    // ==================== 副作用 ====================
    // 根据当前天气更新背景动画类型
    LaunchedEffect(currentPageData.value) {
        val pageData = currentPageData.value
        if (pageData is WeatherPageState.Data) {
            val iconId = pageData.city.weather?.current?.icon ?: "100"
            viewModel.weatherAnimType.value = WeatherAnimType.getAnimType(iconId)
        }
    }

    // ==================== UI 相关状态 ====================
    // 滚动行为控制器
    val scrollBehavior = TopAppBarDefaults.exitUntilCollapsedScrollBehavior()

    // 导航控制器
    val navigator = LocalNavController.current

    // 毛玻璃效果状态
    val hazeState = rememberHazeState()
    val topBarGlassStyle = remember {
        GlassStyle.regular.then {
            shape(RoundedCornerShape(0.dp))
            edgeShadow(Color.Transparent)
            specularIntensity(0f)
            optics(blurRadius = 8.dp, refractionDisplacement = 10.dp)
            chromaMultiplier(1.6f)
            chromaticAberrationStrength(0.1f)
        }
    }

    val pullToRefreshState = rememberPullToRefreshState()

    // ==================== UI 布局 ====================
    AdaptiveWeatherLayout(
        modifier = modifier,
        windowAdaptiveInfo = windowAdaptiveInfo,
        listPane = {
            CityListPane(
                cities = weatherPageStates.value,
                selectedIndex = pagerState.currentPage,
                onCitySelected = { index ->
                    coroutineScope.launch { pagerState.scrollToPage(index) }
                },
                onManageCities = { navigator.add(Routes.WeatherList) },
                modifier = Modifier.fillMaxSize(),
            )
        },
    ) {
        Scaffold(
            containerColor = MaterialTheme.colorScheme.surface,
            modifier = Modifier
                .fillMaxSize(),
            // 浮动添加按钮 - 跳转到城市列表
            floatingActionButton = {
                AddCityButton(
                    hazeState = hazeState,
                    onClick = { navigator.add(Routes.WeatherList) }
                )
            },
            // 顶部标题栏
            topBar = {
                MainTopBar(
                    modifier = Modifier
                        .hazeGlass(
                            input = HazeInput.Sources(hazeState),
                            style = topBarGlassStyle
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
                    .hazeSource(hazeState) // 毛玻璃效果源
                    .fillMaxSize()
            ) {
                // 下拉刷新容器
                PullToRefreshBox(
                    isRefreshing = isRefreshing.value,
                    onRefresh = { viewModel.refresh() },
                    modifier = Modifier.fillMaxSize(),
                    state = pullToRefreshState,
                    indicator = {
                        PullToRefreshDefaults.Indicator(
                            modifier = Modifier
                                .align(Alignment.TopCenter)
                                .padding(top = paddingValues.calculateTopPadding()),
                            state = pullToRefreshState,
                            isRefreshing = isRefreshing.value,
                            containerColor = MaterialTheme.colorScheme.surfaceContainer,
                            color = MaterialTheme.colorScheme.primary,
                        )
                    },
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
@OptIn(ExperimentalHazeApi::class)
@Composable
private fun AddCityButton(
    hazeState: HazeState,
    onClick: () -> Unit
) {
    with(LocalSharedTransitionScope.current) {
        val glassColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .1f)
        val glassStyle = remember(glassColor) {
            GlassStyle.regular.then {
                shape(CircleShape)
                tint(glassColor)
                optics(blurRadius = 8.dp, refractionDisplacement = 22.dp)
                chromaMultiplier(1.6f)
                chromaticAberrationStrength(0.1f)
            }
        }
        Box(
            modifier = Modifier
                .size(MAIN_PLUS_BUTTON_SIZE)
                .noRippleClickable(onClick = onClick)
                .clip(CircleShape)
                .hazeGlass(
                    input = HazeInput.Sources(hazeState),
                    style = glassStyle
                ),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = Icons.Filled.Add,
                contentDescription = stringResource(R.string.cd_add_city),
                tint = MaterialTheme.colorScheme.onSurface,
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
            color = MaterialTheme.colorScheme.onSurface,
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
    scrollBehavior: TopAppBarScrollBehavior,
    paddingValues: PaddingValues
) {
    val layoutDirection = LocalLayoutDirection.current
    HorizontalPager(
        state = pagerState,
        modifier = Modifier.fillMaxSize(),
        key = { weatherPageStates.getOrNull(it)?.city?.id?:-1 },
        beyondViewportPageCount = MainScreenConstants.PAGER_BEYOND_VIEWPORT_PAGE_COUNT,
        pageContent = { currentIndex ->
            // 仅在目标页面时显示内容，优化性能
            if (weatherPageStates.isNotEmpty()) {
                ShowOnIdleContent(visible = currentIndex == pagerState.targetPage) {
                    WeatherPage(
                        weatherPageStates[currentIndex],
                        scrollBehavior = scrollBehavior,
                        modifier = Modifier
                            .fillMaxSize(),
                        paddingValues = PaddingValues(
                            top = paddingValues.calculateTopPadding(),
                            start = paddingValues.calculateStartPadding(layoutDirection) + 15.dp,
                            end = paddingValues.calculateEndPadding(layoutDirection) + 15.dp,
                            bottom = paddingValues.calculateBottomPadding()
                        )
                    )
                }
            }

        }
    )
}



