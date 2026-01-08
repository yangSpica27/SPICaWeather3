package me.spica.spicaweather3.ui.landscape

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 左栏 - 城市列表
 */
@Composable
fun CityListPane(
  cities: List<WeatherPageState>,
  selectedIndex: Int,
  onCitySelected: (Int) -> Unit,
  modifier: Modifier = Modifier
) {
  Column(
    modifier = modifier
      .background(MiuixTheme.colorScheme.surfaceContainer)
      .statusBarsPadding()
      .navigationBarsPadding()
    ,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {
    // 标题栏
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
    ) {
      Text(
        text = "城市列表",
        fontSize = 20.sp,
        fontWeight = FontWeight.Bold,
        color = MiuixTheme.colorScheme.onSurface
      )
    }

    // 城市列表
    LazyColumn(
      modifier = Modifier
        .fillMaxHeight()
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      itemsIndexed(
        items = cities,
        key = { _, state -> state.cityEntity.id }
      ) { index, state ->
        CityListItem(
          cityState = state,
          isSelected = index == selectedIndex,
          onClick = { onCitySelected(index) }
        )
      }
    }
  }
}

/**
 * 城市列表项
 */
@Composable
private fun CityListItem(
  cityState: WeatherPageState,
  isSelected: Boolean,
  onClick: () -> Unit
) {
  val city = cityState.cityEntity
  val weather = city.weather

  // 根据选中状态计算颜色
  val backgroundColor = if (isSelected) {
    MiuixTheme.colorScheme.primary.copy(alpha = 0.12f)
  } else {
    MiuixTheme.colorScheme.surfaceContainer
  }

  val contentColor = if (isSelected) {
    MiuixTheme.colorScheme.primary
  } else {
    MiuixTheme.colorScheme.onSurface
  }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(horizontal = 8.dp)
      .clip(RoundedCornerShape(12.dp))
      .background(backgroundColor)
      .clickable(onClick = onClick)
      .padding(horizontal = 12.dp, vertical = 14.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 定位图标（如果是用户位置）
    if (city.isUserLoc) {
      Box(
        modifier = Modifier
          .size(32.dp)
          .clip(CircleShape)
          .background(MiuixTheme.colorScheme.primary.copy(alpha = 0.15f)),
        contentAlignment = Alignment.Center
      ) {
        Icon(
          imageVector = Icons.Default.LocationOn,
          contentDescription = "定位",
          tint = MiuixTheme.colorScheme.primary,
          modifier = Modifier.size(18.dp)
        )
      }
      Spacer(modifier = Modifier.width(12.dp))
    }

    // 城市信息
    Column(
      modifier = Modifier.weight(1f)
    ) {
      Text(
        text = city.name,
        fontSize = 16.sp,
        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
        color = contentColor
      )

      if (city.adm1.isNotEmpty() && city.adm1 != city.name) {
        Spacer(modifier = Modifier.height(2.dp))
        Text(
          text = "${city.adm1} · ${city.adm2}",
          fontSize = 12.sp,
          color = MiuixTheme.colorScheme.onSurfaceVariantSummary
        )
      }
    }

    // 温度显示
    weather?.current?.let { todayWeather ->
      Text(
        text = "${todayWeather.temperature}°",
        fontSize = 24.sp,
        fontWeight = FontWeight.Light,
        color = contentColor
      )
    }
  }
}
