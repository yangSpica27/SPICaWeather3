package me.spica.spicaweather3.ui.main.cards

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.center
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastCoerceAtLeast
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.remote.api.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun MinutelyCard(
  weatherData: AggregatedWeatherData,
  modifier: Modifier = Modifier
) {

  Column(
    modifier = modifier
      .padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.Start,
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_rainy_light),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurfaceContainer
      )
      Text(
        text = stringResource(R.string.minutely_info_title),
        color = MiuixTheme.colorScheme.onSurfaceContainer, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
      )
    }

    val lineColor = MiuixTheme.colorScheme.onSecondaryContainer.copy(alpha = .31f)

    val rainLineColor = MiuixTheme.colorScheme.primary

    val rainData = weatherData.minutelyPrecip ?: return

    // 缓存 Paint 对象，避免重复创建
    val linePaint = remember {
      android.graphics.Paint().apply {
        strokeCap = android.graphics.Paint.Cap.SQUARE
        style = android.graphics.Paint.Style.STROKE
      }
    }

    val rainPaint = remember {
      android.graphics.Paint().apply {
        strokeCap = android.graphics.Paint.Cap.ROUND
        style = android.graphics.Paint.Style.STROKE
      }
    }

    val textPaint = remember {
      android.graphics.Paint().apply {
        isAntiAlias = true
        textAlign = android.graphics.Paint.Align.LEFT
      }
    }

    val dashLinePaint = remember {
      android.graphics.Paint().apply {
        style = android.graphics.Paint.Style.STROKE
        strokeWidth = 1f
      }
    }

    // 获取文字资源
    val oneHourText = stringResource(R.string.one_hour)
    val lightRainText = stringResource(R.string.rain_light)
    val heavyRainText = stringResource(R.string.rain_heavy)

    // 将颜色转换为 ARGB 以便在 drawWithCache 中使用
    val lineColorArgb = lineColor.toArgb()
    val rainLineColorArgb = rainLineColor.toArgb()
    val textColor = MiuixTheme.colorScheme.onSurfaceContainer.toArgb()
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(2.78f)
        .drawWithCache {
          // 更新 Paint 属性（需要尺寸信息）
          val strokeWidth = 4.dp.toPx()
          linePaint.strokeWidth = strokeWidth
          linePaint.color = lineColorArgb

          // 设置文字画笔属性
          val textSize = 10.dp.toPx()
          textPaint.textSize = textSize
          textPaint.color = textColor

          // 设置虚线画笔属性
          val dashWidth = 4.dp.toPx()
          val dashGap = 4.dp.toPx()
          dashLinePaint.pathEffect =
            android.graphics.DashPathEffect(floatArrayOf(dashWidth, dashGap), 0f)
          dashLinePaint.color = lineColorArgb
          dashLinePaint.strokeWidth = 1.dp.toPx()

          rainPaint.strokeWidth = strokeWidth
          rainPaint.shader = LinearGradientShader(
            from = size.center.copy(y = size.height),
            to = size.center.copy(y = 0f),
            colors = listOf(
              rainLineColor.copy(alpha = .5f),
              rainLineColor.copy(alpha = .7f),
              rainLineColor.copy(alpha = .9f)
            )
          )

          // 计算每个数据点的宽度
          val dataSize = rainData.next2Hours.size.fastCoerceAtLeast(1)
          val itemWidth = size.width / dataSize
          val oneHourX = itemWidth * 6f
          onDrawWithContent {
            drawIntoCanvas { canvas ->
              val nativeCanvas = canvas.nativeCanvas
              val textMargin = 8.dp.toPx()

              // 绘制 1/3 高度处的虚线（大雨线，从底部往上 2/3 的位置）
              val heavyRainY = size.height / 3f
              nativeCanvas.drawLine(
                0f,
                heavyRainY,
                size.width,
                heavyRainY,
                dashLinePaint
              )
              // 在虚线右侧绘制"大雨"文字
              val heavyRainTextWidth = textPaint.measureText(heavyRainText)
              nativeCanvas.drawText(
                heavyRainText,
                size.width - heavyRainTextWidth - textMargin,
                heavyRainY - textMargin,
                textPaint
              )

              // 绘制 2/3 高度处的虚线（小雨线，从底部往上 1/3 的位置）
              val lightRainY = size.height * 2f / 3f
              nativeCanvas.drawLine(
                0f,
                lightRainY,
                size.width,
                lightRainY,
                dashLinePaint
              )
              // 在虚线右侧绘制"小雨"文字
              val lightRainTextWidth = textPaint.measureText(lightRainText)
              nativeCanvas.drawText(
                lightRainText,
                size.width - lightRainTextWidth - textMargin,
                lightRainY - textMargin,
                textPaint
              )

              // 绘制一小时参考下
              nativeCanvas.drawLine(
                oneHourX,
                0f,
                oneHourX,
                size.height,
                linePaint
              )

              // 在分割线顶部右侧绘制"一小时"文字
              nativeCanvas.drawText(
                oneHourText,
                oneHourX + textMargin,
                textSize + textMargin,
                textPaint
              )

              // 绘制降雨数据线条
              for ((index, minutely) in rainData.next2Hours.withIndex()) {
                val precip = (minutely.precipitation.toFloat()).coerceIn(0f, 1f)
                val x = itemWidth * index + itemWidth / 2f
                nativeCanvas.drawLine(
                  x,
                  size.height,
                  x,
                  size.height - size.height * precip,
                  rainPaint
                )
              }
            }
          }
        }
    )
  }
}