package me.spica.spicaweather3.ui.landscape

import androidx.activity.compose.BackHandler
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.ui.NavDisplay
import kotlinx.serialization.Serializable
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.ui.widget.materialSharedAxisZIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZOut
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

@Serializable
private sealed interface PaneRoute : NavKey {
    @Serializable data object Cards : PaneRoute
    @Serializable data class Detail(val cardType: String) : PaneRoute
}

/**
 * 右栏 - 天气内容区
 * 包含内部导航：卡片列表 <-> 详情页
 */
@Composable
fun WeatherContentPane(
    cityState: WeatherPageState?,
    modifier: Modifier = Modifier
) {
    val paneBackStack = rememberNavBackStack(PaneRoute.Cards)

    val isOnDetail = remember(paneBackStack.lastOrNull()) { paneBackStack.lastOrNull() is PaneRoute.Detail }
    BackHandler(enabled = isOnDetail) {
        paneBackStack.removeLastOrNull()
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(MiuixTheme.colorScheme.surface)
    ) {
        if (cityState == null) {
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
            NavDisplay(
                backStack = paneBackStack,
                modifier = Modifier.fillMaxSize(),
                transitionSpec = { materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(false) },
                popTransitionSpec = { materialSharedAxisZIn(false) togetherWith materialSharedAxisZOut(true) },
                entryProvider = entryProvider<NavKey> {
                    entry<PaneRoute.Cards> {
                        PlaceholderCardGrid(
                            cityState = cityState,
                            onCardClick = { cardType ->
                                paneBackStack.add(PaneRoute.Detail(cardType))
                            }
                        )
                    }
                    entry<PaneRoute.Detail> { detail ->
                        CardDetailPlaceholder(
                            cardType = detail.cardType,
                            cityState = cityState,
                            onBack = { paneBackStack.removeLastOrNull() }
                        )
                    }
                }
            )
        }
    }
}
