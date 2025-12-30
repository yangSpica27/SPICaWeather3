package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.innerShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInParent
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import me.spica.spicaweather3.ui.main.DailyTempLineView
import me.spica.spicaweather3.ui.main.ItemWindData
import me.spica.spicaweather3.ui.main.TempLineItem
import me.spica.spicaweather3.ui.main.WindChart
import me.spica.spicaweather3.ui.widget.materialSharedAxisZIn
import me.spica.spicaweather3.ui.widget.materialSharedAxisZOut
import me.spica.spicaweather3.utils.noRippleClickable
import top.yukonga.miuix.kmp.basic.TabRowWithContour
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.pressable

@Composable
fun HourlyCard(modifier: Modifier = Modifier, weatherData: WeatherData) {
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
        painter = painterResource(id = R.drawable.material_symbols_outlined_view_day),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurfaceContainer
      )
      Text(
        text = stringResource(R.string.hourly_info_title),
        color = MiuixTheme.colorScheme.onSurfaceContainer, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
      )
    }

    val tabTitles = listOf(
      stringResource(R.string.hourly_tab_temperature),
      stringResource(R.string.hourly_tab_wind)
    )

    var selectIndex by rememberSaveable { mutableIntStateOf(0) }

    TabRowWithContour(
      titles = tabTitles,
      onTabSelected = {
        selectIndex = it
      }
    )

    AnimatedContent(
      targetState = selectIndex,
      modifier = Modifier
        .fillMaxWidth()
        .clip(WIDGET_CARD_CORNER_SHAPE),
      transitionSpec = {
        materialSharedAxisZIn(true) togetherWith materialSharedAxisZOut(true)
      },
      contentKey = { it }
    ) { index ->

      if (index == 0) {
        val sampleData = remember(weatherData) {
          weatherData.hourlyWeather.map { hourlyWeather ->
            TempLineItem(
              maxTemp = hourlyWeather.temp.toDouble(),
              minTemp = hourlyWeather.temp.toDouble(),
              date = "${hourlyWeather.fxTime().hours}:00",
              weatherType = hourlyWeather.weatherName,
              wind360 = hourlyWeather.wind360.toIntOrNull() ?: 0,
              windDirection = hourlyWeather.windDir,
              probabilityOfPrecipitation = hourlyWeather.pop.toDouble(),
              iconId = hourlyWeather.iconId.toString()
            )
          }
        }
        DailyTempLineView(data = sampleData)
      } else {
        val sampleData = remember(weatherData) {
          weatherData.hourlyWeather.map { hourlyWeather ->
            ItemWindData(
              date = "${hourlyWeather.fxTime().hours}:00",
              windDirection = hourlyWeather.windDir,
              windSpeed = hourlyWeather.windSpeed,
              wind360 = hourlyWeather.wind360.toIntOrNull() ?: 0,
            )
          }
        }
        WindChart(data = sampleData, modifier = Modifier.fillMaxWidth())
      }
    }

  }
}


@Composable
private fun TabRowWithContour(titles: List<String>, onTabSelected: (Int) -> Unit) {

  var selectIndex by rememberSaveable { mutableIntStateOf(0) }
  val tabPositions = remember { mutableStateMapOf<Int, Dp>() }
  val tabWidths = remember { mutableStateMapOf<Int, Dp>() }
  val tabHeight = remember { mutableStateMapOf<Int, Dp>() }
  val density = LocalDensity.current

  val radius = remember { ContinuousRoundedRectangle(12.dp) }

  val indicatorOffset by animateDpAsState(
    targetValue = tabPositions.getOrElse(selectIndex) { 0.dp }, label = ""
  )
  val indicatorWidth by animateDpAsState(
    targetValue = tabWidths.getOrElse(selectIndex) { 0.dp }, label = ""
  )
  val indicatorHeight by animateDpAsState(
    targetValue = tabHeight.getOrElse(selectIndex) { 0.dp }, label = ""
  )

  Box(
    modifier = Modifier
      .background(
        MiuixTheme.colorScheme.secondaryContainer,
        radius
      )
      .innerShadow(
        shape = radius,
        shadow = Shadow(
          radius = 4.dp,
          color = MiuixTheme.colorScheme.secondaryContainer,
          alpha = .2f,
        )
      )
      .padding(4.dp)
  ) {


    if (indicatorWidth > 0.dp && indicatorHeight > 0.dp) {
      Box(
        modifier = Modifier
          .offset(x = indicatorOffset)
          .width(indicatorWidth)
          .height(indicatorHeight)
          .background(
            MiuixTheme.colorScheme.surfaceContainer,
            radius
          )
      )
    }

    Row(
      modifier = Modifier,
      horizontalArrangement = Arrangement.spacedBy(8.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      titles.forEachIndexed { index, string ->
        TabItem(
          modifier = Modifier.onGloballyPositioned {
            tabPositions[index] = with(density) { it.positionInParent().x.toDp() }
            tabWidths[index] = with(density) { it.size.width.toDp() }
            tabHeight[index] = with(density) { it.size.height.toDp() }
          },
          title = string,
          selected = selectIndex == index,
          onClick = {
            selectIndex = index
            onTabSelected(index)
          }
        )
      }
    }


  }
}


@Composable
fun TabItem(
  modifier: Modifier = Modifier,
  title: String,
  selected: Boolean,
  onClick: () -> Unit
) {

  val textColor by animateColorAsState(
    targetValue = if (selected) MiuixTheme.colorScheme.onSurfaceContainer else MiuixTheme.colorScheme.onSurfaceContainer.copy(
      alpha = 0.6f
    ), label = ""
  )

  val fontWeight = if (selected) {
    FontWeight.W700
  } else {
    FontWeight.W500
  }

  Box(
    modifier = modifier
      .clip(
        ContinuousRoundedRectangle(12.dp)
      )
      .noRippleClickable {
        onClick()
      }
      .pressable(null)
      .padding(horizontal = 12.dp, vertical = 6.dp),
    contentAlignment = Alignment.Center
  ) {
    Text(
      title,
      color = textColor,
      fontWeight = fontWeight,
      style = MiuixTheme.textStyles.button.copy(fontSize = 14.5.sp)
    )
  }
}
