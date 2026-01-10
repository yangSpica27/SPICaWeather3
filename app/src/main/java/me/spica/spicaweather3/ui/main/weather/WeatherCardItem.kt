package me.spica.spicaweather3.ui.main.weather

import androidx.compose.animation.core.EaseOutCubic
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.graphicsLayer
import me.spica.spicaweather3.common.model.WeatherCardConfig
import me.spica.spicaweather3.data.remote.api.model.weather.AggregatedWeatherData
import sh.calvin.reorderable.ReorderableCollectionItemScope

/**
 * 单个天气卡片项
 * 
 * 包装卡片组件，提供拖拽、抖动动画、入场动画等功能。
 * 
 * @param cardConfig 卡片配置
 * @param weatherData 天气数据
 * @param delayMillis 入场动画延迟（毫秒）
 * @param durationMillis 入场动画持续时间（毫秒）
 * @param shakeOffset 抖动偏移量（拖拽时其他卡片会抖动）
 * @param onDragStart 开始拖拽回调
 * @param onDragStop 停止拖拽回调
 */
@Composable
fun ReorderableCollectionItemScope.WeatherCardItem(
  cardConfig: WeatherCardConfig,
  weatherData: AggregatedWeatherData,
  delayMillis: Int,
  durationMillis: Int,
  shakeOffset: Float,
  onDragStart: (startedPosition: Offset) -> Unit = {},
  onDragStop: () -> Unit
) {
  CardContainer(
    animationSpec = tween(
      durationMillis = durationMillis,
      delayMillis = delayMillis,
      easing = EaseOutCubic
    ),
    modifier = Modifier
      .graphicsLayer {
        if (shakeOffset != 0f) {
          rotationZ = shakeOffset * 0.5f  // 轻微旋转
          translationX = shakeOffset * 1.5f  // 轻微水平偏移
        }
      }
      .longPressDraggableHandle(
        enabled = true,
        onDragStarted = onDragStart,
        onDragStopped = onDragStop
      ),
    ratio = if (cardConfig.cardType.spanSize == 2) 0f else 1f,
    content = { isAnimEnd ->
      WeatherCardFactory.CreateCard(
        cardType = cardConfig.cardType,
        weatherData = weatherData,
        startAnim = isAnimEnd,
        modifier = Modifier.fillMaxWidth()
      )
    }
  )
}
