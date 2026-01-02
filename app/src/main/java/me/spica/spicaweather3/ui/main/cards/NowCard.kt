package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.capsule.ContinuousRoundedRectangle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.WeatherAnimType
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.ui.widget.WeatherBackground
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 当前天气卡片
 * 展示当前实时天气信息的核心卡片组件，包含动态天气背景和主要天气指标
 * 特点：
 * - 动态天气背景动画（晴天、雨天、雪天等不同场景）
 * - 大号温度显示
 * - 体感温度和湿度信息
 * - 文字渐显和平移动画
 * - 毛玻璃模糊效果
 *
 * @param modifier 修饰符
 * @param weatherData 当前天气数据
 * @param startAnim 是否开始播放动画
 */
@Composable
fun NowCard(modifier: Modifier = Modifier, weatherData: WeatherData, startAnim: Boolean) {


  // 根据天气图标ID计算对应的天气动画类型（晴天、雨天、雪天等）
  val currentWeatherAnimType = remember(weatherData) {
    WeatherAnimType.getAnimType(
      weatherData.todayWeather.iconId.toString(),
    )
  }

  // ==================== 动画配置 ====================
  // 温度数字动画（无延迟，持续450ms）
  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 250, 0)
  ).value

  // 天气状况和体感温度动画（延迟50ms，持续550ms）
  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 350, 50)
  ).value

  // 湿度信息动画（延迟150ms，持续750ms）
  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 550, 150)
  ).value

  // ==================== 主布局 ====================
  // 使用 Box 叠加布局：底层为天气背景动画，上层为天气信息文字
  Box(
    modifier = modifier
      .aspectRatio(1.21f) // 宽高比 1.21:1，保持卡片比例
  ) {
    // 动态天气背景（晴天、雨天、雪天等不同动画效果）
    WeatherBackground(
      currentWeatherType = currentWeatherAnimType,
      collapsedFraction = 0f, // 0=完全展开，1=完全折叠
    )

    // 天气信息文字层
    Column(
      modifier = Modifier
        .padding(
          12.dp
        )
        .padding(
          horizontal = 12.dp,
          vertical = 12.dp
        ),
      verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      // 当前温度（大号显示，带渐显和向上平移动画）
      Text(
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue1
          translationY = -12.dp.toPx() * (1f - textAnimValue1)
        },
        text = buildAnnotatedString {
          // 温度数字（94sp 超大字体）
          withStyle(
            style = SpanStyle(
              color = MiuixTheme.colorScheme.surface,
              fontSize = 94.sp,
              fontWeight = FontWeight.W800
            )
          ) {
            append(weatherData.todayWeather.temp.toString())
          }
          // 温度单位（45sp 较小字体）
          withStyle(
            style = SpanStyle(
              color = MiuixTheme.colorScheme.surface,
              fontSize = 45.sp,
              fontWeight = FontWeight.W800
            )
          ) {
            append("°C")
          }
        },
      )

      // 天气状况和体感温度（带延迟渐显和向上平移动画）
      Text(
        text = stringResource(
          R.string.now_card_condition,
          weatherData.todayWeather.weatherName,
          weatherData.todayWeather.feelTemp
        ),
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.surface,
        fontWeight = FontWeight.W600,
        modifier = Modifier
          .graphicsLayer {
            alpha = textAnimValue2
            translationY = -12.dp.toPx() * (1f - textAnimValue2)
          }
          .padding(start = 12.dp)
      )
      
      // 湿度信息（带毛玻璃背景、延迟渐显和向上平移动画）
      Text(
        text = stringResource(
          R.string.now_card_humidity,
          weatherData.todayWeather.water
        ),
        style = MiuixTheme.textStyles.body2,
        color = MiuixTheme.colorScheme.surface,
        fontWeight = FontWeight.W600,
        modifier = Modifier
          .graphicsLayer {
            alpha = textAnimValue3
            translationY = -12.dp.toPx() * (1f - textAnimValue3)
          }
          .padding(start = 12.dp)
          .clip(
            ContinuousRoundedRectangle(12.dp)
          )
          // 应用 Cupertino 风格超薄毛玻璃效果
          .background(
            MiuixTheme.colorScheme.surface.copy(alpha = 0.2f),
            CircleShape
          )
          .padding(
            horizontal = 8.dp,
            vertical = 4.dp
          )
      )
    }
  }
}