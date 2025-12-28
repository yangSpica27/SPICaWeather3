package me.spica.spicaweather3.ui.main.cards

import android.graphics.Color.argb
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.ExperimentalHazeApi
import dev.chrisbanes.haze.HazeDefaults
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.CupertinoMaterials
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 空气质量卡片
 * 使用 air2 数据显示详细的 AQI 信息、等级、健康建议和主要污染物
 */
@OptIn(ExperimentalHazeApi::class)
@Composable
fun AqiCard(weatherData: WeatherData, startAnim: Boolean) {
  val hazeState = HazeState()
  
  // 从 air2 获取数据，使用第一个 index（通常是综合 AQI）
  val airIndex = weatherData.air2.indexes.firstOrNull() ?: return
  val aqi = airIndex.aqi
  val category = airIndex.category
  val level = airIndex.level
  val healthAdvice = airIndex.health.advice.generalPopulation
  val primaryPollutant = airIndex.primaryPollutant.toString()
  
  // 从 ARGB 构建颜色
  val aqiColor = remember(airIndex.color) {
    Color(
      red = airIndex.color.red,
      green = airIndex.color.green,
      blue = airIndex.color.blue,
      alpha = 255
    )
  }
  
  // 优化后的显示颜色，确保在浅色背景下可读
  val displayColor = remember(aqiColor) {
    // 转换到 HSL 色彩空间进行调整
    val hsl = FloatArray(3)
    android.graphics.Color.colorToHSV(aqiColor.toArgb(), hsl)
    
    // 降低饱和度和亮度，提高对比度
    hsl[1] = (hsl[1] * 0.7f).coerceIn(0.3f, 1f) // 饱和度降低到70%，最低保持30%
    hsl[2] = (hsl[2] * 0.8f).coerceIn(0.3f, 0.7f) // 亮度控制在30%-70%之间
    
    Color(android.graphics.Color.HSVToColor(hsl))
  }

  // 动画配置
  val progressAnimValue = animateFloatAsState(
    if (startAnim) 1f else 0f,
    label = "progress"
  ).value

  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 450, 150),
    label = "text1"
  ).value

  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 550, 250),
    label = "text2"
  ).value

  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 750, 350),
    label = "text3"
  ).value

  Column(
    modifier = Modifier.padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.Start
  ) {
    // 标题行
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_air),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface
      )
      Text(
        text = stringResource(R.string.aqi_title),
        color = MiuixTheme.colorScheme.onSurface,
        style = WIDGET_CARD_TITLE_TEXT_STYLE(),
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue1
          translationY = -12.dp.toPx() * (1f - textAnimValue1)
        }
      )
    }

    // AQI 数值和等级
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.Bottom
    ) {
      Text(
        text = aqi.toString(),
        color = displayColor,
        style = MiuixTheme.textStyles.main.copy(
          fontWeight = FontWeight.ExtraBold,
          fontSize = 38.sp
        ),
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue2
          translationY = -12.dp.toPx() * (1f - textAnimValue2)
        }
      )
      Text(
        text = category,
        color = MiuixTheme.colorScheme.onSurface,
        style = MiuixTheme.textStyles.main.copy(
          fontWeight = FontWeight.SemiBold,
          fontSize = 16.sp
        ),
        modifier = Modifier
          .alignByBaseline()
          .graphicsLayer {
            alpha = textAnimValue2
            translationY = -12.dp.toPx() * (1f - textAnimValue2)
          }
      )
    }

    // AQI 可视化指示器
    BoxWithConstraints(
      modifier = Modifier
        .height(32.dp)
        .background(
          color = MiuixTheme.colorScheme.surfaceContainer,
        )
        ,
      contentAlignment = Alignment.BottomCenter,
    ) {
      // 渐变背景条
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(11.dp)
          .graphicsLayer {
            translationY = -5.5.dp.toPx() - 4.dp.toPx()
          }
          .hazeSource(state = hazeState)
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF00E400), // 优 (0-50)
                Color(0xFFFFFF00), // 良 (51-100)
                Color(0xFFFF7E00), // 轻度污染 (101-150)
                Color(0xFFFF0000), // 中度污染 (151-200)
                Color(0xFF99004C), // 重度污染 (201-300)
                Color(0xFF7E0023)  // 严重污染 (300+)
              )
            ),
            shape = ContinuousRoundedRectangle(2.dp)
          )
          .border(
            1.dp,
            color = Color.White.copy(alpha = 0.3f),
            ContinuousRoundedRectangle(2.dp)
          )
      )
      
      // 指示器圆圈
      Box(
        modifier = Modifier
          .size(32.dp)
          .graphicsLayer {
            // AQI 最大值通常是500，映射到0-1范围
            translationX = (constraints.maxWidth - 32.dp.toPx()) * 
              ((aqi.toFloat() / 500f).coerceIn(0f, 1f)) * progressAnimValue
          }
          .clip(CircleShape)
          .background(
            color = MiuixTheme.colorScheme.surfaceContainer,
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
              scaleY = 1.2f
            }
            .hazeEffect(
              state = hazeState,
              style = CupertinoMaterials.ultraThin(
                containerColor = MiuixTheme.colorScheme.surfaceContainerHigh
              )
            )
        )
      }
    }

    // 底部健康建议
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Top,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Text(
        modifier = Modifier
          .weight(1f)
          .graphicsLayer {
            alpha = textAnimValue3
            translationY = -12.dp.toPx() * (1f - textAnimValue3)
          },
        text = healthAdvice,
        color = MiuixTheme.colorScheme.onSurface.copy(alpha = .6f),
        style = WIDGET_CARD_TITLE_TEXT_STYLE().copy(
          fontWeight = FontWeight.W500,
          fontSize = 13.sp
        ),
        maxLines = 2
      )
      Icon(
        modifier = Modifier.size(28.dp),
        painter = painterResource(R.drawable.material_symbols_outlined_masks),
        tint = MiuixTheme.colorScheme.onSurface,
        contentDescription = null
      )
    }
  }
}

