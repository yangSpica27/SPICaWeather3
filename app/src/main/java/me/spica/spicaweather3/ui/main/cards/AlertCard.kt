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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.network.model.weather.WeatherWarning
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import me.spica.spicaweather3.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeParseException
import java.util.*

private const val MAX_WARNING_ITEMS = 2
private val warningTimeFormatter by lazy {
  DateTimeFormatter.ofPattern("MM-dd HH:mm", Locale.getDefault())
}
private val AlertBadgeDefaultColor = Color(0xFF7C66FF)

@Composable
fun AlertCard(weatherData: WeatherData) {
  val warnings = remember(weatherData.warnings) {
    weatherData.warnings.filter { it.hasDisplayableContent() }
  }
  val headerLine = warnings.firstOrNull()?.headerLine()
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



private fun WeatherWarning.hasDisplayableContent(): Boolean =
  !title.isNullOrBlank() || !text.isNullOrBlank() || !typeName.isNullOrBlank()

private fun WeatherWarning.headerLine(): String? {
  val parts = listOfNotNull(
    typeName?.takeIf { it.isNotBlank() },
    levelName?.takeIf { it.isNotBlank() }
  )
  if (parts.isNotEmpty()) return parts.joinToString(" · ")
  return title?.takeIf { it.isNotBlank() } ?: severity?.takeIf { it.isNotBlank() }
}

private fun WeatherWarning.primaryTitle(): String? =
  title?.takeIf { it.isNotBlank() }
    ?: typeName?.takeIf { it.isNotBlank() }
    ?: levelName?.takeIf { it.isNotBlank() }

private fun WeatherWarning.badgeLabel(): String? =
  levelName?.takeIf { it.isNotBlank() }
    ?: level?.takeIf { it.isNotBlank() }
    ?: severity?.takeIf { it.isNotBlank() }

private fun WeatherWarning.metaInfo(): String? {
  val publish = formattedPublishTime()
  val sourceInfo = source?.takeIf { it.isNotBlank() }
  val statusInfo = status?.takeIf { it.isNotBlank() }
  val segments = listOfNotNull(sourceInfo, publish, statusInfo)
  return segments.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

private fun WeatherWarning.formattedPublishTime(): String? {
  val raw = pubTime?.takeIf { it.isNotBlank() } ?: return null
  return try {
    OffsetDateTime.parse(raw).format(warningTimeFormatter)
  } catch (_: DateTimeParseException) {
    raw.replace("T", " ")
  }
}

private fun WeatherWarning.levelColor(): Color {
  val keyword = buildString {
    level?.let { append(it).append(' ') }
    levelName?.let { append(it).append(' ') }
    severity?.let { append(it).append(' ') }
  }.lowercase(Locale.getDefault())
  return when {
    keyword.contains("red") || keyword.contains("红") -> Color(0xFFa8071a)
    keyword.contains("orange") || keyword.contains("橙") -> Color(0xFFF97A24)
    keyword.contains("yellow") || keyword.contains("黄") -> Color(0xFFFADB14)
    keyword.contains("blue") || keyword.contains("蓝") -> Color(0xFF2F54EB)
    keyword.contains("green") || keyword.contains("绿") || keyword.contains("minor") -> Color(0xFF52C41A)
    keyword.contains("white") || keyword.contains("白") -> Color(0xFFBFBFBF)
    keyword.contains("black") || keyword.contains("黑") -> Color(0xFF595959)
    keyword.contains("severe") -> Color(0xFFFF4D4F)
    keyword.contains("moderate") -> Color(0xFFFFA940)
    else -> AlertBadgeDefaultColor
  }
}