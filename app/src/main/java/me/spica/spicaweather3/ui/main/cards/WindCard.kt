package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 风力卡片
 * 显示当前风力信息、风向、风速，以及未来24小时风力趋势图
 */
@Composable
fun WindCard(weatherEntity: WeatherData, startAnim: Boolean) {
  val todayWeather = weatherEntity.todayWeather
  val hourlyWeather = weatherEntity.hourlyWeather.take(24)

  // 获取当前风向描述
  val currentWindDir = remember(hourlyWeather) {
    if (hourlyWeather.isNotEmpty()) hourlyWeather.first().windDir else "北风"
  }

  // 获取当前风力角度
  val currentWind360 = remember(hourlyWeather) {
    if (hourlyWeather.isNotEmpty()) hourlyWeather.first().wind360.toFloatOrNull() ?: 0f else 0f
  }

  // 动画配置
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

  val iconRotationAnim = animateFloatAsState(
    if (startAnim) currentWind360 + 45f + 180f else 0f,
    animationSpec = tween(durationMillis = 800),
    label = "iconRotation"
  ).value

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .aspectRatio(1f)
      .padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.Start
  ) {
    // 标题行
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_wind_power),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface
      )
      Text(
        text = stringResource(R.string.hourly_tab_wind),
        color = MiuixTheme.colorScheme.onSurface,
        style = WIDGET_CARD_TITLE_TEXT_STYLE(),
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue1
          translationY = -12.dp.toPx() * (1f - textAnimValue1)
        }
      )
    }

    // 当前风力信息
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .graphicsLayer {
          alpha = textAnimValue2
          translationY = -12.dp.toPx() * (1f - textAnimValue2)
        },
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 风向指示器
      Box(
        modifier = Modifier
          .size(50.dp)
          .background(
            brush = Brush.radialGradient(
              colors = listOf(
                MiuixTheme.colorScheme.primary.copy(alpha = 0.2f),
                MiuixTheme.colorScheme.primary.copy(alpha = 0.05f),
                Color.Transparent
              )
            ),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          Icons.AutoMirrored.Default.ArrowBack,
          contentDescription = null,
          tint = MiuixTheme.colorScheme.primary,
          modifier = Modifier
            .size(40.dp)
            .graphicsLayer{
              transformOrigin = TransformOrigin(0.5f, 0.5f)
              rotationZ = iconRotationAnim
            }
        )
      }

      // 风速和风向信息
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp)
      ) {
        Text(
          text = currentWindDir,
          color = MiuixTheme.colorScheme.onSurface,
          style = MiuixTheme.textStyles.main.copy(fontWeight = FontWeight.Bold)
        )

        Row(
          verticalAlignment = Alignment.Bottom,
          horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
          Text(
            modifier = Modifier.alignByBaseline(),
            text = "${todayWeather.windSpeed}",
            color = MiuixTheme.colorScheme.primary,
            style = MiuixTheme.textStyles.title1.copy(fontWeight = FontWeight.ExtraBold)
          )
          Text(
            modifier = Modifier.alignByBaseline(),
            text = "km/h",
            color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
            style = MiuixTheme.textStyles.footnote1
          )
        }
      }
    }
    // 风力等级描述
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .graphicsLayer {
          alpha = textAnimValue3
          translationY = -12.dp.toPx() * (1f - textAnimValue3)
        },
      contentAlignment = Alignment.Center
    ) {
      Text(
        modifier = Modifier.fillMaxWidth(),
        text = getWindLevelDescription(todayWeather.windSpeed),
        color = MiuixTheme.colorScheme.onSurfaceVariantSummary,
        style = MiuixTheme.textStyles.body2,
        textAlign = TextAlign.Start
      )
    }

  }
}

/**
 * 根据风速获取风力等级描述
 */
private fun getWindLevelDescription(windSpeed: Int): String {
  return when {
    windSpeed < 1 -> "无风"
    windSpeed < 6 -> "1级 软风"
    windSpeed < 12 -> "2级 轻风"
    windSpeed < 20 -> "3级 微风"
    windSpeed < 29 -> "4级 和风"
    windSpeed < 39 -> "5级 清劲风"
    windSpeed < 50 -> "6级 强风"
    windSpeed < 62 -> "7级 疾风"
    windSpeed < 75 -> "8级 大风"
    windSpeed < 89 -> "9级 烈风"
    windSpeed < 103 -> "10级 狂风"
    windSpeed < 118 -> "11级 暴风"
    windSpeed < 134 -> "12级 飓风"
    windSpeed < 150 -> "13级 台风"
    windSpeed < 167 -> "14级 强台风"
    windSpeed < 184 -> "15级 强台风"
    windSpeed < 202 -> "16级 超强台风"
    else -> "17级 超强台风"
  }
}