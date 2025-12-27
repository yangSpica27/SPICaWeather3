package me.spica.spicaweather3.ui

import androidx.compose.animation.AnimatedContentScope
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalRippleConfiguration
import androidx.compose.material3.RippleConfiguration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.rememberMultiplePermissionsState
import me.spica.spicaweather3.common.SharedContentKey
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.route.Routes
import me.spica.spicaweather3.ui.air_quality.AirQualityScreen
import me.spica.spicaweather3.ui.city_selector.CitySelectorScreen
import me.spica.spicaweather3.ui.main.MainScreen
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.weather_list.WeatherListScreen
import me.spica.spicaweather3.ui.widget.BottomSheetMenu
import me.spica.spicaweather3.ui.widget.LocalMenuState
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import me.spica.spicaweather3.ui.widget.materialSharedAxisXIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZOut
import me.spica.spicaweather3.utils.DataStoreUtil
import me.spica.spicaweather3.utils.LocationHelper
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.theme.darkColorScheme
import top.yukonga.miuix.kmp.theme.lightColorScheme


@OptIn(ExperimentalSharedTransitionApi::class)
val LocalSharedTransitionScope = compositionLocalOf<SharedTransitionScope> {
  error("LocalSharedTransitionScope not provided")
}

val LocalAnimatedContentScope = compositionLocalOf<AnimatedContentScope> {
  error("LocalAnimatedContentScope not provided")
}

@OptIn(ExperimentalSharedTransitionApi::class, ExperimentalPermissionsApi::class)
@Composable
fun AppMain() {
  val colors = if (isSystemInDarkTheme()) {
    darkColorScheme()
  } else {
    lightColorScheme()
  }

  val navController = rememberNavController()

  val menuState = LocalMenuState.current

  val blurEnable = menuState.isVisible

  val blurRadius = animateDpAsState(
    targetValue = if (blurEnable) 12.dp else 0.dp,
    animationSpec = tween(easing = LinearEasing, durationMillis = 125)
  ).value

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

  LaunchedEffect(locationPermissionState.allPermissionsGranted) {
    if (locationPermissionState.allPermissionsGranted){
      locationHelper.fetchLocation(
        onSuccess = { loc -> 
          weatherViewModel.insertUserLoc(loc, shouldRefresh = true)
        },
        onFailure = { 
          weatherViewModel.insertUserLoc(null, shouldRefresh = true)
        }
      )
    }else{
      weatherViewModel.insertUserLoc(null, shouldRefresh = true)
    }
  }

  MiuixTheme(
    colors = colors
  ) {
    SharedTransitionLayout(modifier = Modifier.fillMaxSize()) {
      Box(
        modifier = Modifier
          .fillMaxSize()
          .blur(blurRadius)
          .background(MiuixTheme.colorScheme.surface)
      ) {
        CompositionLocalProvider(
          LocalSharedTransitionScope provides this@SharedTransitionLayout,
          LocalNavController provides navController,
          LocalRippleConfiguration provides RippleConfiguration(color = Color.Transparent)
        ) {
          NavHost(
            startDestination = Routes.Main,
            navController = navController,
            modifier = Modifier
              .fillMaxSize()
              .background(
                MiuixTheme.colorScheme.surface
              ),
            enterTransition = {
              slideInHorizontally { i -> i }
            },
            exitTransition = {
              slideOutHorizontally { i -> -i }
            },
          ) {
            composable<Routes.Main> {
              CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                MainScreen()
              }
            }

            composable<Routes.CitySelect> {
              CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                CitySelectorScreen()
              }
            }

            composable<Routes.WeatherList> {
              CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                WeatherListScreen()
              }
            }
            composable<Routes.AirQuality> {
              CompositionLocalProvider(LocalAnimatedContentScope provides this) {
                AirQualityScreen()
              }
            }
          }
        }
        BottomSheetMenu(
          state = LocalMenuState.current,
          modifier = Modifier.align(Alignment.BottomCenter),
        )
      }
    }
  }

}