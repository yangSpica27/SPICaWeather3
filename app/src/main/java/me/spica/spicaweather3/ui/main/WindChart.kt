package me.spica.spicaweather3.ui.main

import android.R.attr.end
import android.R.attr.strokeWidth
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import top.yukonga.miuix.kmp.theme.MiuixTheme


private val ITEM_WIDTH = 80.dp


private val LINE_HEIGHT = 80.dp

@Composable
fun WindChart(modifier: Modifier = Modifier, data: List<ItemWindData>) {

  val lineColor1 = MiuixTheme.colorScheme.secondaryContainer

  val lineColor2 = MiuixTheme.colorScheme.primary

  Box(
    modifier = modifier
      .fillMaxWidth()
      .horizontalScroll(state = rememberScrollState())
  ) {

    Row {
      data.forEach { item ->
        Column(
          modifier = modifier
            .width(ITEM_WIDTH),
          horizontalAlignment = Alignment.CenterHorizontally,
          verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
          Text(
            text = item.date,
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface
          )
          Icon(
            Icons.AutoMirrored.Default.ArrowBack,
            contentDescription = null,
            tint = MiuixTheme.colorScheme.onSurfaceContainer,
            modifier = Modifier.rotate(
              item.wind360 * 1f + 45f + 180f
            )
          )
          Canvas(
            modifier = Modifier
              .fillMaxWidth()
              .height(height = LINE_HEIGHT)
          ) {

            drawLine(
              color = lineColor1,
              start = Offset(size.width / 2, 0f),
              end = Offset(size.width / 2, size.height),
              strokeWidth = 3.dp.toPx()
            )

            val lineWidth = ITEM_WIDTH.toPx() / 4

            val startY =
              size.height - lineWidth / 2 - ((size.height - lineWidth / 2 * 2) * (item.windSpeed / 50f).coerceIn(
                0f,
                1f
              ))

            val endY = size.height - lineWidth / 2

            drawLine(
              color = lineColor2,
              start = Offset(
                size.width / 2,
                startY
              ),
              end = Offset(size.width / 2, endY),
              cap = StrokeCap.Round,
              strokeWidth = lineWidth
            )
          }

          Text(
            text = "${item.windSpeed}km/h",
            style = MiuixTheme.textStyles.footnote1,
            color = MiuixTheme.colorScheme.onSurface,
            fontWeight = FontWeight.W500
          )
        }


      }

    }

  }
}


data class ItemWindData(
  val date: String,
  val windDirection: String,
  val windSpeed: Int,
  val wind360: Int
)