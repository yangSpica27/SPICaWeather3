package me.spica.spicaweather3.ui.main

import android.graphics.CornerPathEffect
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asAndroidPath
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.common.WeatherAnimType
import top.yukonga.miuix.kmp.theme.MiuixTheme


private val ITEM_WIDTH = 80.dp


private val LINE_HEIGHT = 80.dp

@Composable
fun DailyTempLineView(
  data: List<TempLineItem>,
  modifier: Modifier = Modifier,
) {

  Box(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(state = rememberScrollState())
  ) {

    var height by remember { mutableStateOf(0.dp) }

    Column(
      horizontalAlignment = Alignment.CenterHorizontally,
      modifier = Modifier
        .padding(vertical = 12.dp),
      verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {

      Row(
        modifier = Modifier
          .onGloballyPositioned {
            height = it.size.height.dp
          }
      ) {
        data.forEachIndexed { _, item ->
          Column(
            modifier = modifier
              .width(ITEM_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = item.date,
              color = MiuixTheme.colorScheme.onSurface.copy(alpha = .6f),
              style = MiuixTheme.textStyles.body2
            )
            Icon(
              painter = painterResource(WeatherAnimType.getWeatherIconRes(item.iconId)),
              contentDescription = null,
              tint = Color.Unspecified,
              modifier = Modifier
                .width(34.dp)
                .height(34.dp)
            )
            Text(
              text = "${item.maxTemp}℃",
              color = MiuixTheme.colorScheme.onSurface,
              style = MiuixTheme.textStyles.footnote2,
              fontWeight = FontWeight.W600
            )
          }
        }
      }

      val tintColor = MiuixTheme.colorScheme.onSurfaceContainer

      // 缓存 Paint 对象，避免在 drawWithCache 内重复创建
      val linePaint = remember {
        Paint().asFrameworkPaint().apply {
          isAntiAlias = true
          strokeCap = android.graphics.Paint.Cap.ROUND
          style = android.graphics.Paint.Style.STROKE
        }
      }

      Box(
        modifier = modifier
          .width(ITEM_WIDTH * data.size)
          .height(LINE_HEIGHT)
          .drawWithCache {

            val maxTempValue = data.maxOf { it.maxTemp }
            val minTempValue = data.minOf { it.minTemp }
            val tempRange = (maxTempValue - minTempValue).toFloat()

            val maxTempPath = Path().asAndroidPath()
            val minTempPath = Path().asAndroidPath()

            val maxTempShader = LinearGradientShader(
              from = Offset(0f, 0f),
              to = Offset(size.width, 0f),
              listOf(
                Color(0xFFbae637),
                Color(0xFFbae637),
                Color(0xFFfff1b8),
                Color(0xffffe58f),
                Color(0xffffe58f),
                Color(0xFFffd591),
                Color(0xffff9c6e),
              )
            )

            val minTempShader = LinearGradientShader(
              from = Offset(0f, 0f),
              to = Offset(size.width, size.height),
              listOf(
                Color(0xFFbae637),
                Color(0xFFbae637),
                Color(0xFFfff1b8),
                Color(0xffffe58f),
                Color(0xffffe58f),
                Color(0xffffe58f),
                Color(0xffffd666),
              )
            )

            // 更新 Paint 属性（复用对象）
            linePaint.strokeWidth = 3.dp.toPx()
            linePaint.color = tintColor.toArgb()
            linePaint.pathEffect = CornerPathEffect(22.dp.toPx())

            onDrawWithContent {


              data.forEachIndexed { index, item ->
                val x = (index * ITEM_WIDTH.toPx()) + ITEM_WIDTH.toPx() / 2

                val maxTempY =
                  size.height * 0.8f * (1 - ((item.maxTemp - minTempValue) / tempRange).toFloat()) + size.height * 0.1f
                val minTempY =
                  size.height * 0.8f * (1 - ((item.minTemp - minTempValue) / tempRange).toFloat()) + size.height * 0.1f

                if (index == 0) {
                  maxTempPath.moveTo(x, maxTempY)
                  minTempPath.moveTo(x, minTempY)
                } else {
                  maxTempPath.lineTo(x, maxTempY)
                  minTempPath.lineTo(x, minTempY)
                }

              }


              drawIntoCanvas { canvas ->
                linePaint.shader = maxTempShader
                canvas.nativeCanvas.drawPath(
                  maxTempPath,
                  linePaint
                )
              }
              drawIntoCanvas { canvas ->
                linePaint.shader = minTempShader
                canvas.nativeCanvas.drawPath(
                  minTempPath,
                  linePaint
                )
              }
            }
          },
      )
      Row {
        data.forEachIndexed { _, item ->
          Column(
            modifier = modifier
              .width(ITEM_WIDTH),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
          ) {
            Text(
              text = "${item.minTemp}℃",
              color = MiuixTheme.colorScheme.onSurface,
              style = MiuixTheme.textStyles.footnote2,
              fontWeight = FontWeight.W600
            )
            Text(
              text = "${item.probabilityOfPrecipitation}%",
              color = MiuixTheme.colorScheme.onSurface.copy(alpha = .7f),
              style = MiuixTheme.textStyles.footnote1
            )
            Icon(
              Icons.AutoMirrored.Default.ArrowBack,
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurfaceContainer,
              modifier = Modifier.rotate(
                item.wind360 * 1f + 45f + 180f
              )
            )
            Text(
              text = item.windDirection,
              color = MiuixTheme.colorScheme.onSurface,
              style = MiuixTheme.textStyles.footnote2
            )
          }
        }
      }
    }
  }

}


data class TempLineItem(
  val maxTemp: Double,
  val minTemp: Double,
  val date: String = "",// 日期
  val probabilityOfPrecipitation: Double = 0.0,// 降水概率
  val weatherType: String = "",
  val wind360: Int = 0,
  val windDirection: String = "",
  val iconId: String
)
