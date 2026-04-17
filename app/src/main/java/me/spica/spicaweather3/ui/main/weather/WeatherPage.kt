package me.spica.spicaweather3.ui.main.weather

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_100
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.widget.AnimateOnEnter
import org.koin.compose.koinInject
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


/**
 * 天气页面
 * 
 * 根据天气数据状态展示不同的页面：
 * - Data: 展示天气卡片网格
 * - Empty: 展示空状态提示
 * 
 * @param weatherState 天气页面状态
 * @param modifier 修饰符
 * @param scrollBehavior 滚动行为
 */
@Composable
fun WeatherPage(
  weatherState: WeatherPageState,
  modifier: Modifier = Modifier,
  scrollBehavior: ScrollBehavior,
  paddingValues: PaddingValues
) {
  AnimatedContent(
    weatherState,
    modifier = modifier,
    label = "WeatherPage",
    contentKey = { it },
  ) { state ->
    when (state) {
      is WeatherPageState.Data -> {
        DataPage(
          city = state.city,
          scrollBehavior = scrollBehavior,
          paddingValues = paddingValues
        )
      }

      is WeatherPageState.Empty -> {
        EmptyPage()
      }
    }
  }
}

/**
 * 数据页面
 * 
 * 展示天气数据和可配置的天气卡片网格。
 * 
 * @param city 城市领域模型（包含天气数据）
 * @param scrollBehavior 滚动行为
 */
@Composable
private fun DataPage(
  city: me.spica.spicaweather3.domain.model.City,
  scrollBehavior: ScrollBehavior,
  paddingValues: PaddingValues
) {
  val viewModel = koinInject<WeatherViewModel>()
  
  // 获取天气数据（已保证非空）
  val weatherData = city.weather!!
  
  // 获取所有卡片配置
  val allCardsConfigs = viewModel.cardsConfig.collectAsStateWithLifecycle().value

  // 计算当前天气动画类型
  val currentAnimType = remember(weatherData) {
    derivedStateOf {
      WeatherAnimType.getAnimType(weatherData.current.icon)
    }
  }.value

  // 根据天气数据过滤可显示的卡片
  val hasAlerts = weatherData.weatherAlerts?.isNotEmpty() ?: false
  val filteredCards = remember(allCardsConfigs, currentAnimType, hasAlerts) {
    viewModel.getFilteredCardsForWeather(allCardsConfigs, currentAnimType, hasAlerts)
  }

  // 天气卡片网格
  WeatherCardsGrid(
    cards = filteredCards,
    weatherData = weatherData,
    onReorder = { reorderedCards ->
      // 仅传递当前可见的已排序卡片，由 DataStore 层负责与不可见卡片合并
      // 避免不可见卡片（如晴天时的 MINUTELY）被挤到末尾
      viewModel.reorderCards(reorderedCards)
    },
    scrollBehavior = scrollBehavior,
    paddingValues = paddingValues
  )
}

/**
 * 卡片容器
 * 
 * 为卡片提供入场动画和缩放效果的容器组件。
 * 
 * @param modifier 修饰符
 * @param content 卡片内容，参数表示动画是否已结束
 * @param animationSpec 动画规格
 * @param ratio 宽高比，0 表示不限制
 */
@Composable
fun CardContainer(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.(isAnimEnd: Boolean) -> Unit,
  animationSpec: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
  ),
  ratio: Float = 0f
) {
  val ratioModifier = if (ratio > 0f) {
    Modifier.aspectRatio(ratio)
  } else {
    Modifier
  }

  AnimateOnEnter(
    modifier = modifier
      .fillMaxWidth()
      .then(ratioModifier),
    animationSpec = animationSpec
  ) { progress, anim ->
    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          this.alpha = progress
          scaleX = 1.0f * progress
          scaleY = 1.0f * progress
          transformOrigin = TransformOrigin(0.5f, 0.5f)
        }
        .cardBackground()
        .clip(WIDGET_CARD_CORNER_SHAPE),
      contentAlignment = Alignment.Center
    ) {
      content(!anim.isRunning && anim.value > 0)
    }
  }
}

/**
 * 空状态页面
 * 
 * 当没有天气数据时显示的空状态提示。
 */
@Composable
private fun EmptyPage() {
  Box(
    modifier = Modifier.fillMaxSize(),
    contentAlignment = Alignment.Center
  ) {
    Text(
      stringResource(R.string.empty_state_no_data),
      style = MiuixTheme.textStyles.title2,
      color = COLOR_WHITE_100
    )
  }
}

/**
 * 卡片背景修饰符
 */
@Composable
private fun Modifier.cardBackground() = this.background(
  color = MiuixTheme.colorScheme.surfaceContainer,
  shape = WIDGET_CARD_CORNER_SHAPE
)
