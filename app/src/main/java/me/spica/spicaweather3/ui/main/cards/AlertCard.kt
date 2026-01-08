package me.spica.spicaweather3.ui.main.cards

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import me.spica.spicaweather3.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun AlertCard(weatherData: AggregatedWeatherData) {
  val alerts = remember(weatherData.weatherAlerts) {
    weatherData.weatherAlerts ?: emptyList()
  }
  val headerLine = alerts.firstOrNull()?.headline
  val color = MiuixTheme.colorScheme.primary

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .background(
        color = color,
        shape = WIDGET_CARD_CORNER_SHAPE
      )
      .padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(12.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.CenterVertically,
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      Icon(
        painter = painterResource(R.drawable.material_symbols_outlined_brightness_alert),
        contentDescription = null,
        modifier = Modifier.size(28.dp),
        tint = COLOR_WHITE_100
      )
      Column(
        modifier = Modifier.weight(1f),
        verticalArrangement = Arrangement.spacedBy(4.dp),
        horizontalAlignment = Alignment.Start
      ) {
        Text(
          text = stringResource(R.string.alert_info_title),
          modifier = Modifier.fillMaxWidth(),
          maxLines = 1,
          style = MiuixTheme.textStyles.headline2,
          color = COLOR_WHITE_100,
          fontWeight = FontWeight.W600
        )
        Text(
          text = headerLine ?: stringResource(R.string.alert_placeholder),
          modifier = Modifier.fillMaxWidth(),
          maxLines = 1,
          overflow = TextOverflow.Ellipsis,
          style = MiuixTheme.textStyles.body1,
          color = COLOR_WHITE_100
        )
      }
    }
  }
}



