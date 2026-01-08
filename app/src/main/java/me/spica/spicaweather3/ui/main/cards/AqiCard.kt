package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Fill
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.AirQualitySummary
import me.spica.spicaweather3.theme.COLOR_BLACK_100
import me.spica.spicaweather3.theme.COLOR_BLACK_5
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 空气质量卡片
 * 使用 air2 数据显示详细的 AQI 信息、等级、健康建议和主要污染物
 */
@Composable
fun AqiCard(airQualitySummary: AirQualitySummary?, startAnim: Boolean) {
  // 从 air2 获取数据，使用第一个 index（通常是综合 AQI）
  val airIndex = airQualitySummary ?: return
  val aqi = airIndex.aqi
  val healthAdvice = airIndex.healthAdvice


  // 动画配置
  val progressAnimValue = animateFloatAsState(
    if (startAnim) 1f else 0f, label = "progress"
  ).value

  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 450, 150), label = "text1"
  ).value

  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 550, 250), label = "text2"
  ).value

  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 750, 350), label = "text3"
  ).value

  Column(
    modifier = Modifier.padding(WIDGET_CARD_PADDING),
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
        painter = painterResource(id = R.drawable.icon_aqi),
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
        })
    }

    // AQI 数值和等级
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {

      Box(
        modifier = Modifier
          .width(100.dp)
          .height(100.dp)
          .drawBehind {
            drawCircle(
              brush = Brush.sweepGradient(
                colors = listOf(
                  Color(0XFFb37feb),
                  Color(0XFFf759ab),
                  Color(0XFFffc53d),
                  Color(0XFFfff566),
                  Color(0XFF73d13d),
                  Color(0XFFbae637),
                  Color(0XFFb37feb),
                  Color(0XFFb37feb),
                ), center = center
              ), center = center, radius = size.minDimension / 2 - 6.dp.toPx()
            )
            for (i in 0 until 5) {
              drawCircle(
                style = Fill,
                color = Color.White.copy(alpha = 0.3f),
                center = center,
                radius = 12.dp.toPx() + i * ((size.minDimension / 2 - 6.dp.toPx()) / 5f)
              )
            }
            drawArc(
              color = COLOR_BLACK_5,
              startAngle = -135f,
              sweepAngle = -270f * progressAnimValue,
              useCenter = false,
              style = androidx.compose.ui.graphics.drawscope.Stroke(
                7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round
              )
            )
            drawArc(
              brush = Brush.sweepGradient(
                colors = listOf(
                  Color(0XFFb37feb),
                  Color(0XFFf759ab),
                  Color(0XFFffc53d),
                  Color(0XFFfff566),
                  Color(0XFF73d13d),
                  Color(0XFFbae637),
                  Color(0XFFb37feb),
                  Color(0XFFb37feb),
                ), center = center
              ),
              startAngle = -135f,
              sweepAngle = -270f * progressAnimValue * (aqi.coerceIn(0, 500) / 500f),
              useCenter = false,
              style = androidx.compose.ui.graphics.drawscope.Stroke(
                7.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round
              )
            )
          }, contentAlignment = Alignment.Center
      ) {
        Text(
          modifier = Modifier.graphicsLayer {
            alpha = textAnimValue2
            translationY = -12.dp.toPx() * (1f - textAnimValue2) + 2.dp.toPx()
          },
          text = aqi.toString(),
          color = COLOR_BLACK_100,
          style = WIDGET_CARD_TITLE_TEXT_STYLE().copy(
            fontSize = 36.sp, fontWeight = FontWeight.ExtraBold
          )
        )
      }
      // 污染物
      Column(
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start,
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue2
          translationY = -12.dp.toPx() * (1f - textAnimValue2)
        }) {

      }
      // 健康建议
      Text(
        modifier = Modifier
          .fillMaxWidth()
          .graphicsLayer {
            alpha = textAnimValue3
            translationY = -12.dp.toPx() * (1f - textAnimValue3)
          },
        text = healthAdvice,
        color = MiuixTheme.colorScheme.onSurfaceContainer.copy(alpha = .7f),
        style = MiuixTheme.textStyles.body1,
      )
    }
  }

}

