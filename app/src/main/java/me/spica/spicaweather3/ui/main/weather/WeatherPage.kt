package me.spica.spicaweather3.ui.main.weather

import android.util.Log
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.common.model.WeatherCardConfig
import me.spica.spicaweather3.common.model.WeatherCardType
import me.spica.spicaweather3.network.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import me.spica.spicaweather3.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.main.cards.AlertCard
import me.spica.spicaweather3.ui.main.cards.AqiCard
import me.spica.spicaweather3.ui.main.cards.DailyCard
import me.spica.spicaweather3.ui.main.cards.FeelTempCard
import me.spica.spicaweather3.ui.main.cards.HourlyCard
import me.spica.spicaweather3.ui.main.cards.HumidityCard
import me.spica.spicaweather3.ui.main.cards.MinutelyCard
import me.spica.spicaweather3.ui.main.cards.NowCard
import me.spica.spicaweather3.ui.main.cards.PrecipitationCard
import me.spica.spicaweather3.ui.main.cards.SunriseCard
import me.spica.spicaweather3.ui.main.cards.UVCard
import me.spica.spicaweather3.ui.main.cards.WindCard
import me.spica.spicaweather3.ui.widget.AnimateOnEnter
import me.spica.spicaweather3.ui.widget.LocalMenuState
import org.koin.compose.koinInject
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.random.Random


@Composable
fun WeatherPage(
  weatherState: WeatherPageState, modifier: Modifier = Modifier, scrollBehavior: ScrollBehavior
) {

  AnimatedContent(
    weatherState,
    modifier = modifier,
    label = "WeatherPage",
    contentKey = { it },
  ) { state ->
    when (state) {

      is WeatherPageState.Data -> {
        DataPage(weatherEntity = state.cityEntity.weather!!, scrollBehavior = scrollBehavior)
      }

      is WeatherPageState.Empty -> {
        EmptyPage()
      }
    }
  }
}

@Composable
private fun DataPage(
  weatherEntity: AggregatedWeatherData, scrollBehavior: ScrollBehavior
) {

  val viewModel = koinInject<WeatherViewModel>()

  val cardsConfigs = viewModel.cardsConfig.collectAsStateWithLifecycle().value

  val currentAnimType = remember(weatherEntity) {
    derivedStateOf {
      WeatherAnimType.getAnimType(weatherEntity.current.icon)
    }
  }.value

  // 拖拽状态标记
  var isDrag by remember { mutableStateOf(false) }

  var cardsConfigs2 by remember {
    mutableStateOf<List<WeatherCardConfig>>(emptyList())
  }

  // iOS 风格的抖动动画 - 当有任何 item 被拖拽且当前 item 未被拖拽时启用
  val infiniteTransition = rememberInfiniteTransition(label = "shake")
  val shakeOffset by infiniteTransition.animateFloat(
    initialValue = -1f,
    targetValue = 1f,
    animationSpec = infiniteRepeatable(
      animation = tween(80, easing = LinearEasing),
      repeatMode = RepeatMode.Reverse
    ),
    label = "shake_offset"
  )


  LaunchedEffect(cardsConfigs, weatherEntity) {
    if (!isDrag) {
      val temp = cardsConfigs.filter { cardsConfig ->
        var include = true
        if (!currentAnimType.showRain && cardsConfig.cardType == WeatherCardType.MINUTELY) {
          include = false
        }
        if (cardsConfig.cardType == WeatherCardType.ALERT && weatherEntity.weatherAlerts.isNullOrEmpty()) {
          include = false
        }
        return@filter include
      }.toMutableList()

      cardsConfigs2 = temp
    }
  }

  LaunchedEffect(cardsConfigs2) {
    Log.i("WeatherPage", "cardsConfigs2 changed: $cardsConfigs2")
  }

  val menuState = LocalMenuState.current

  val dataStoreUtil = viewModel.dataStoreUtil

  val listState = rememberLazyGridState()

  // 触觉反馈控制器
  val hapticFeedback = LocalHapticFeedback.current

  val reorderableLazyListState = rememberReorderableLazyGridState(listState) { from, to ->
    val mutableList = cardsConfigs2.toMutableList()
    val fromItem = mutableList[from.index]
    val toItem = mutableList[to.index]
    val item = mutableList.removeAt(from.index)
    mutableList.add(to.index, item)
    cardsConfigs2 = mutableList

    // 更新实际数据采用原始数据进行存储
    val originalList = cardsConfigs.toMutableList()
    val fromIndex = originalList.indexOfFirst { it.cardType == fromItem.cardType }
    val toIndex = originalList.indexOfFirst { it.cardType == toItem.cardType }
    val originalItem = originalList.removeAt(fromIndex)
    originalList.add(toIndex, originalItem)
    dataStoreUtil.updateCardsOrder(originalList)
    // 触发触觉反馈
    hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
  }

  LazyVerticalGrid(
    modifier = Modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    state = listState,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    cardsConfigs2.forEach { cardsConfig ->
      item(
        key = cardsConfig.cardType.key, span = { GridItemSpan(cardsConfig.cardType.spanSize) }
      ) {

        // 优化延迟和持续时间，创造更流畅的波浪式入场效果
        val delayMillis = remember(cardsConfig.cardType.key) { Random.nextInt(50, 280) }
        val durationMillis = remember(cardsConfig.cardType.key) { Random.nextInt(100, 700) }

        ReorderableItem(
          key = cardsConfig.cardType.key, state = reorderableLazyListState
        ) { isDragging ->

          val shouldShake = isDrag && !isDragging

          CardContainer(
            animationSpec = tween(
              durationMillis = durationMillis, delayMillis = delayMillis,
              easing = EaseOutCubic
            ),
            modifier = Modifier
              .graphicsLayer {
                if (shouldShake) {
                  rotationZ = shakeOffset * 0.5f  // 轻微旋转
                  translationX = shakeOffset * 1.5f  // 轻微水平偏移
                }
              }
              .longPressDraggableHandle(
                enabled = true,
                onDragStarted = {
                  isDrag = true
                },
                onDragStopped = {
                  isDrag = false
                }
              ),
            ratio = if (cardsConfig.cardType.spanSize == 2) 0f else 1f,
            content = { isAnimEnd ->
              when (cardsConfig.cardType) {
                WeatherCardType.NOW -> NowCard(
                  modifier = Modifier.fillMaxWidth(),
                  weatherData = weatherEntity,
                  startAnim = isAnimEnd
                )

                WeatherCardType.ALERT -> AlertCard(weatherEntity)
                WeatherCardType.MINUTELY -> MinutelyCard(
                  modifier = Modifier.fillMaxWidth(), weatherData = weatherEntity
                )

                WeatherCardType.HOURLY -> HourlyCard(
                  modifier = Modifier.fillMaxWidth(), weatherData = weatherEntity
                )

                WeatherCardType.DAILY -> DailyCard(data = weatherEntity)
                WeatherCardType.UV -> UVCard(
                  weatherEntity.forecast.today.uvIndex, isAnimEnd
                )

                WeatherCardType.FEEL_TEMP -> FeelTempCard(
                  feelTemp = weatherEntity.current.feelsLike, startAnim = isAnimEnd
                )

                WeatherCardType.PRECIPITATION -> PrecipitationCard(
                  precipitation = weatherEntity.current.precipitation.toInt(),
                  pop = weatherEntity.forecast.next24Hours?.firstOrNull()?.pop?.toInt() ?: 0,
                  startAnim = isAnimEnd
                )

                WeatherCardType.HUMIDITY -> HumidityCard(
                  humidity = weatherEntity.current.humidity, startAnim = isAnimEnd
                )

                WeatherCardType.SUNRISE -> SunriseCard(
                  weatherEntity = weatherEntity, startAnim = isAnimEnd
                )

                WeatherCardType.WIND -> WindCard(
                   startAnim = isAnimEnd,
                  weatherEntity = weatherEntity
                )

                WeatherCardType.AQI -> AqiCard(
                  airQualitySummary = weatherEntity.airQuality,
                  startAnim = isAnimEnd
                )
              }
            },
          )
        }
      }
    }
  }
}


@Composable
fun CardContainer(
  modifier: Modifier = Modifier,
  content: @Composable BoxScope.(isAnimEnd: Boolean) -> Unit,
  animationSpec: AnimationSpec<Float> = spring(
    dampingRatio = Spring.DampingRatioMediumBouncy,
    stiffness = Spring.StiffnessLow
  ),
  ratio: Float = 0F
) {

  val ratioModifier = if (ratio > 0f) {
    Modifier.aspectRatio(ratio)
  } else {
    Modifier
  }

  AnimateOnEnter(
    modifier = modifier
      .fillMaxWidth()
      .then(ratioModifier), animationSpec = animationSpec
  ) { progress, anim ->

    Box(
      modifier = Modifier
        .fillMaxSize()
        .graphicsLayer {
          this.alpha = progress
          scaleX = 0.25f + 0.75f * progress
          scaleY = 0.25f + 0.75f * progress
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


@Composable
private fun EmptyPage() {
  Box(
    modifier = Modifier, contentAlignment = Alignment.Center
  ) {
    Text(
      stringResource(R.string.empty_state_no_data),
      modifier = Modifier.align(Alignment.Center),
      style = MiuixTheme.textStyles.title2,
      color = COLOR_WHITE_100
    )
  }
}

@Composable
private fun Modifier.cardBackground() = this.background(
  color = MiuixTheme.colorScheme.surfaceContainer, shape = WIDGET_CARD_CORNER_SHAPE
)
