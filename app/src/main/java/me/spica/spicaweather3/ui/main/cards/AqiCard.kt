package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.theme.COLOR_BLACK_10
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt


@Composable
fun AqiCard(modifier: Modifier = Modifier, weatherData: WeatherData, startAnim: Boolean) {
  val animValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 250)
  ).value

  val animValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 370, 170)
  ).value

  val animValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f, spring(
      dampingRatio = 1.5f, stiffness = 700f
    )
  ).value

  Column(
    modifier = modifier.padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_check_in_out),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.graphicsLayer {
          scaleX = animValue1
          scaleY = animValue1
          alpha = animValue1
        }
      )
      Text(
        text = stringResource(R.string.air_quality_title),
        color = MiuixTheme.colorScheme.onSurface, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
        modifier = Modifier.graphicsLayer {
          alpha = animValue2
          translationY = -12.dp.toPx() * (1f - animValue2)
        }
      )
    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
      Box(
        modifier = Modifier
          .fillMaxHeight()
          .aspectRatio(1f)
          .padding(8.dp)
          .drawWithCache {

            val shader = Brush.horizontalGradient(
              colors = listOf(
                Color(0xff52c41a),
                Color(0xffbae637),
                Color(0xFFCDDC39),
                Color(0xFFFFEB3B),
                Color(0xFFFF9800),
              )
            )

            val padding = 10.dp.toPx()

            val drawSize = Size(
              width = size.width - padding * 2, height = size.height - padding * 2
            )


            onDrawBehind {
              translate(
                padding, padding
              ) {
                drawArc(
                  color = COLOR_BLACK_10,
                  startAngle = 45f,
                  sweepAngle = -270f,
                  size = drawSize,
                  useCenter = false,
                  style = Stroke(
                    width = 20.dp.toPx(), cap = StrokeCap.Round
                  )
                )
                drawArc(
                  color = COLOR_BLACK_10,
                  startAngle = 45f,
                  sweepAngle = -270f,
                  useCenter = false,
                  size = drawSize,
                  style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Square,
                    pathEffect = PathEffect.dashPathEffect(
                      floatArrayOf(
                        6.dp.toPx(), size.width / 10, size.width / 10
                      )
                    )
                  )
                )
                drawArc(
                  brush = shader,
                  startAngle = 135f,
                  size = drawSize,
                  sweepAngle = 270f * (weatherData.air.aqi / 500f).coerceIn(0f, 1f) * animValue3,
                  useCenter = false,
                  style = Stroke(
                    width = 12.dp.toPx(), cap = StrokeCap.Round
                  )
                )
              }
            }
          },
        contentAlignment = Alignment.BottomCenter,
      ) {
        Text(
          "${(weatherData.air.aqi * animValue3).roundToInt()}",
          modifier = Modifier.align(Alignment.Center),
          style = MiuixTheme.textStyles.title1,
          color = MiuixTheme.colorScheme.onSurface,
          fontWeight = FontWeight.W900
        )
        Text(
          weatherData.air.category,
          modifier = Modifier.align(Alignment.BottomCenter),
          style = MiuixTheme.textStyles.title3,
          color = MiuixTheme.colorScheme.onSurface,
          fontWeight = FontWeight.W900
        )
      }
      Column(
        modifier = Modifier
          .weight(1f)
          .padding(vertical = 12.dp),
        horizontalAlignment = Alignment.Start,
        verticalArrangement = Arrangement.Center
      ) {

        ItemAir(
          modifier = Modifier.weight(1f),
          title = stringResource(R.string.pollutant_carbon_monoxide), value = "${weatherData.air.co} μg/m³", progress = (weatherData.air.co/70.0).toFloat().coerceIn(0f,1f)
        )
        ItemAir(
          modifier = Modifier.weight(1f),
          title = stringResource(R.string.pollutant_sulfur_dioxide), value = "${weatherData.air.so2} μg/m³", progress = (weatherData.air.so2/74.0).toFloat().coerceIn(0f,1f)
        )
        ItemAir(
          modifier = Modifier.weight(1f),
          title = stringResource(R.string.pollutant_nitrogen_dioxide), value = "${weatherData.air.no2} μg/m³", progress = (weatherData.air.no2/64.0).toFloat().coerceIn(0f,1f)
        )
        ItemAir(
          modifier = Modifier.weight(1f),
          title = "PM2.5", value = "${weatherData.air.pm2p5} μg/m³", progress = (weatherData.air.pm2p5/74.0).toFloat().coerceIn(0f,1f)
        )

      }

    }
  }
}

@Composable
fun ItemAir(modifier: Modifier = Modifier, title: String, value: String, progress: Float) {

  val lineColor = MiuixTheme.colorScheme.onSurface.copy(alpha = .6f)

  val lineBackground = MiuixTheme.colorScheme.secondaryContainer


  Column(
    modifier = modifier,
    horizontalAlignment = Alignment.Start,
    verticalArrangement = Arrangement.spacedBy(8.dp)
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.SpaceBetween,
      verticalAlignment = Alignment.CenterVertically
    ) {
      Text(title, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurface)
      Text(value, style = MiuixTheme.textStyles.body2, color = MiuixTheme.colorScheme.onSurface)
    }
  }
  Canvas(
    modifier = Modifier
      .fillMaxWidth()
      .height(12.dp)
  ) {
    drawLine(
      strokeWidth = size.height/2,
      color = lineBackground,
      start = Offset(0f, size.height / 2),
      end = Offset(size.width, size.height / 2),
      cap = StrokeCap.Round
    )
    drawLine(
      strokeWidth = size.height/2,
      color = lineColor,
      start = Offset(0f, size.height / 2),
      end = Offset(size.width * progress, size.height / 2),
      cap = StrokeCap.Round
    )
  }
}