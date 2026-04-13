package me.spica.spicaweather3.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.LocalNavAnimatedContentScope
import androidx.navigation3.ui.NavDisplay
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.route.Routes
import me.spica.spicaweather3.ui.air_quality.AirQualityScreen
import me.spica.spicaweather3.ui.city_selector.CitySelectorScreen
import me.spica.spicaweather3.ui.landscape.LandscapeMainScreen
import me.spica.spicaweather3.ui.main.MainScreen
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.weather_list.WeatherListScreen
import me.spica.spicaweather3.ui.widget.BottomSheetMenu
import me.spica.spicaweather3.ui.widget.DropdownMenuOverlay
import me.spica.spicaweather3.ui.widget.LocalDropdownMenuController
import me.spica.spicaweather3.ui.widget.LocalMenuState
import me.spica.spicaweather3.ui.widget.materialSharedAxisZIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZOut
import me.spica.spicaweather3.utils.DataStoreUtil
import me.spica.spicaweather3.utils.LocationHelper
import me.spica.spicaweather3.utils.isLandscape
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.theme.ColorSchemeMode
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.ThemeController
import top.yukonga.miuix.kmp.theme.defaultTextStyles


@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
    error("LocalSharedTransitionScope not provided")
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalPermissionsApi::class)
@Composable
fun AppMain() {

    val locationHelper = koinInject<LocationHelper>()

    val dataStoreUtil = koinInject<DataStoreUtil>()

    val locationPermissionState = rememberMultiplePermissionsState(
        permissions = listOf(
            android.Manifest.permission.ACCESS_COARSE_LOCATION,
            android.Manifest.permission.ACCESS_FINE_LOCATION
        )
    )

    val isFirstLaunch = dataStoreUtil.getIsFirstLaunch().collectAsStateWithLifecycle(false).value

    LaunchedEffect(isFirstLaunch, locationPermissionState) {
        if (!locationPermissionState.allPermissionsGranted) {
            locationPermissionState.launchMultiplePermissionRequest()
        }
    }

    val weatherViewModel = koinActivityViewModel<WeatherViewModel>()

    val themeController = remember { ThemeController(ColorSchemeMode.System) }

    // 缓存屏幕方向状态，减少重组
    val isLandscapeMode = isLandscape()

    LaunchedEffect(locationPermissionState.allPermissionsGranted) {
        if (locationPermissionState.allPermissionsGranted) {
            locationHelper.fetchLocation(
                onSuccess = { loc ->
                    weatherViewModel.insertUserLoc(loc, shouldRefresh = true)
                },
                onFailure = {
                    weatherViewModel.insertUserLoc(null, shouldRefresh = true)
                }
            )
        } else {
            weatherViewModel.insertUserLoc(null, shouldRefresh = true)
        }
    }

    val menuState = LocalMenuState.current

    val dropdownMenuController = LocalDropdownMenuController.current

    MiuixTheme(
        controller = themeController,
        textStyles =
            defaultTextStyles(
                main =
                    TextStyle(
                        fontSize = 17.sp,
                        letterSpacing = 0.1.sp,
                    ),
                paragraph =
                    TextStyle(
                        fontSize = 17.sp,
                        lineHeight = 1.2f.em,
                    ),
                body1 =
                    TextStyle(
                        fontSize = 16.sp,
                        letterSpacing = 0.5.sp,
                    ),
                body2 =
                    TextStyle(
                        fontSize = 14.sp,
                        letterSpacing = 0.4.sp,
                    ),
                button =
                    TextStyle(
                        fontSize = 17.sp,
                        letterSpacing = 0.4.sp,
                    ),
                footnote1 =
                    TextStyle(
                        fontSize = 13.sp,
                        letterSpacing = 0.5.sp,
                    ),
                footnote2 =
                    TextStyle(
                        fontSize = 11.sp,
                        letterSpacing = 0.5.sp,
                    ),
                headline1 =
                    TextStyle(
                        fontSize = 17.sp,
                    ),
                headline2 =
                    TextStyle(
                        fontSize = 16.sp,
                    ),
                subtitle =
                    TextStyle(
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.1.sp,
                    ),
                title1 =
                    TextStyle(
                        fontSize = 32.sp,
                    ),
                title2 =
                    TextStyle(
                        fontSize = 24.sp,
                        letterSpacing = 0.1.sp,
                    ),
                title3 =
                    TextStyle(
                        fontSize = 20.sp,
                        letterSpacing = 0.15.sp,
                    ),
                title4 =
                    TextStyle(
                        fontSize = 18.sp,
                        letterSpacing = 0.1.sp,
                    ),
            ),
    ) {
        // 使用已缓存的屏幕方向状态
        LaunchedEffect(isLandscapeMode) {
            menuState.dismiss()
            dropdownMenuController.dismiss()
        }

        if (isLandscapeMode) {
            // 横屏模式 - 双栏布局
            LandscapeMainScreen()
        } else {
            // 竖屏模式 - 原有导航布局
            PortraitMainScreen()
        }
    }

}

/**
 * 竖屏主界面布局
 * 包含导航、底部菜单、下拉菜单等
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
private fun PortraitMainScreen() {

    val backStack = rememberNavBackStack(Routes.Main)

    val blurRadius2 = animateDpAsState(
        targetValue = if (LocalDropdownMenuController.current.isVisible) 10.dp else 0.dp,
        label = "DropdownMenuBlur",
        animationSpec = tween(550)
    )

    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MiuixTheme.colorScheme.surface)
        ) {
            CompositionLocalProvider(
                LocalNavController provides backStack,
                LocalSharedTransitionScope provides this@SharedTransitionLayout,
            ) {
                NavDisplay(
                    backStack = backStack,
                    modifier = Modifier
                        .fillMaxSize()
                        .blur(blurRadius2.value)
                        .background(MiuixTheme.colorScheme.surface),
                    sharedTransitionScope = this@SharedTransitionLayout,
                    transitionSpec = {
                        materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(true)
                    },
                    popTransitionSpec = {
                        materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(true)
                    },
                    predictivePopTransitionSpec = {
                        materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(true)
                    },
                    entryProvider = entryProvider {
                        entry<Routes.Main> {
                            MainScreen()
                        }
                        entry<Routes.CitySelect> {
                            CitySelectorScreen()
                        }
                        entry<Routes.WeatherList> {
                            WeatherListScreen()
                        }
                        entry<Routes.AirQuality> {
                            AirQualityScreen()
                        }
                    }
                )
            }
            BottomSheetMenu(
                state = LocalMenuState.current,
                modifier = Modifier.align(Alignment.BottomCenter),
            )
            // 下拉菜单叠加层
            DropdownMenuOverlay()
        }
    }
}