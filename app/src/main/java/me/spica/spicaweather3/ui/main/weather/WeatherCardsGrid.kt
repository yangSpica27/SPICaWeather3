package me.spica.spicaweather3.ui.main.weather

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.rememberLazyGridState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.common.model.WeatherCardConfig
import me.spica.spicaweather3.data.remote.api.model.weather.AggregatedWeatherData
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyGridState
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import kotlin.random.Random

/**
 * 天气卡片网格
 * 
 * 展示多个天气卡片的网格布局，支持拖拽排序、入场动画、抖动效果。
 * 
 * @param cards 要显示的卡片配置列表
 * @param weatherData 天气数据
 * @param onReorder 卡片重新排序回调，参数为重新排序后的完整列表
 * @param scrollBehavior 滚动行为
 * @param modifier 修饰符
 */
@Composable
fun WeatherCardsGrid(
  cards: List<WeatherCardConfig>,
  weatherData: AggregatedWeatherData,
  onReorder: (List<WeatherCardConfig>) -> Unit,
  scrollBehavior: ScrollBehavior,
  modifier: Modifier = Modifier
) {
  // 拖拽状态标记
  var isDragging by remember { mutableStateOf(false) }
  
  // 当前显示的卡片列表（用于拖拽过程中的实时更新）
  var displayCards by remember(cards) {
    mutableStateOf(cards)
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

  val listState = rememberLazyGridState()
  val hapticFeedback = LocalHapticFeedback.current

  // 拖拽排序状态
  val reorderableState = rememberReorderableLazyGridState(listState) { from, to ->
    // 实时更新显示列表
    val mutableList = displayCards.toMutableList()
    val item = mutableList.removeAt(from.index)
    mutableList.add(to.index, item)
    displayCards = mutableList
    
    // 触发触觉反馈
    hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
  }

  LazyVerticalGrid(
    modifier = modifier
      .fillMaxSize()
      .nestedScroll(scrollBehavior.nestedScrollConnection),
    columns = GridCells.Fixed(2),
    horizontalArrangement = Arrangement.spacedBy(12.dp),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    state = listState,
    contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
  ) {
    displayCards.forEach { cardConfig ->
      item(
        key = cardConfig.cardType.key,
        span = { GridItemSpan(cardConfig.cardType.spanSize) }
      ) {
        // 优化延迟和持续时间，创造更流畅的波浪式入场效果
        val delayMillis = remember(cardConfig.cardType.key) { Random.nextInt(0, 280) }
        val durationMillis = remember(cardConfig.cardType.key) { Random.nextInt(100, 700) }

        ReorderableItem(
          key = cardConfig.cardType.key,
          state = reorderableState
        ) { isDraggingThis ->
          val shouldShake = isDragging && !isDraggingThis

          WeatherCardItem(
            cardConfig = cardConfig,
            weatherData = weatherData,
            delayMillis = delayMillis,
            durationMillis = durationMillis,
            shakeOffset = if (shouldShake) shakeOffset else 0f,
            onDragStart = { isDragging = true },
            onDragStop = {
              isDragging = false
              // 拖拽结束后，通知外部更新真实数据
              onReorder(displayCards)
            }
          )
        }
      }
    }
  }
}
