package me.spica.spicaweather3.ui.landscape

import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.ui.widget.materialSharedAxisZIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZOut
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 右栏 - 天气内容区
 * 包含内部导航：卡片列表 <-> 详情页
 */
@Composable
fun WeatherContentPane(
    cityState: WeatherPageState?,
    modifier: Modifier = Modifier
) {
    // 右栏内部导航控制器
    val paneNavController = rememberNavController()
    
    // 处理返回键 - 如果在详情页，返回到卡片列表
    BackHandler(
        enabled = paneNavController.currentBackStackEntry?.destination?.route?.startsWith("detail") == true
    ) {
        paneNavController.popBackStack()
    }
    
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        if (cityState == null) {
            // 无数据占位
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "暂无城市数据",
                    color = MiuixTheme.colorScheme.onSurfaceVariantSummary
                )
            }
        } else {
            // 内部导航
            NavHost(
                navController = paneNavController,
                startDestination = "cards",
                modifier = Modifier.fillMaxSize(),
                enterTransition = { materialSharedAxisZIn(true) },
                exitTransition = { materialSharedAxisZOut(false) },
            ) {
                // 卡片列表页
                composable("cards") {
                    PlaceholderCardGrid(
                        cityState = cityState,
                        onCardClick = { cardType ->
                            paneNavController.navigate("detail/$cardType")
                        }
                    )
                }
                
                // 详情页
                composable("detail/{cardType}") { backStackEntry ->
                    val cardType = backStackEntry.arguments?.getString("cardType") ?: ""
                    CardDetailPlaceholder(
                        cardType = cardType,
                        cityState = cityState,
                        onBack = { paneNavController.popBackStack() }
                    )
                }
            }
        }
    }
}
