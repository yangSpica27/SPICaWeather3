package me.spica.spicaweather3.ui.main.cards

import android.util.Log
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.remote.api.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.data.remote.api.model.weather.DailyForecast
import me.spica.spicaweather3.presentation.theme.COLOR_BLACK_10
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import me.spica.spicaweather3.utils.noRippleClickable
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.pressable

/**
 * 天级别天气信息卡片
 * 展示未来多天的天气预报，包括温度、天气状况、湿度、风速等信息
 * 点击每日项可展开查看详细信息
 *
 * @param data 天气数据，包含多日预报信息
 */
@Composable
fun DailyCard(data: AggregatedWeatherData) {
  // 计算所有天气数据中的最低和最高温度，用于温度进度条的比例计算
  val limitLow = remember(data) { data.forecast.next7Days.minOf { it.tempMin } }
  val limitHigh = remember(data) { data.forecast.next7Days.maxOf { it.tempMax } }

  Column(
    modifier = Modifier
      .fillMaxWidth()
      .padding(
        vertical = 12.dp,
        horizontal = 16.dp
      )
  ) {
    // 卡片标题栏
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_calendar_today),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurfaceContainer
      )

      Text(
        text = stringResource(R.string.daily_info_title),
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        style = WIDGET_CARD_TITLE_TEXT_STYLE()
      )
    }
    // 遍历展示每天的天气信息
    data.forecast.next7Days.forEach { item ->
      DailyItem(item, limitLow = limitLow, limitHigh = limitHigh)
    }
  }
}

/**
 * 单日天气信息项
 * 展示某一天的基本天气信息，点击可展开详细信息面板
 *
 * @param item 单日天气数据
 * @param limitLow 本周期内的最低温度，用于温度条比例计算
 * @param limitHigh 本周期内的最高温度，用于温度条比例计算
 */
@Composable
private fun DailyItem(item: DailyForecast, limitLow: Int, limitHigh: Int) {
  // 控制详细信息面板的展开/收起状态
  var showExtraPanel by rememberSaveable { mutableStateOf(false) }

  Column(
    modifier = Modifier.fillMaxWidth()
  ) {
    // 主信息行：星期、天气、温度、温度条
    Row(
      modifier = Modifier
        .fillMaxWidth()
        .pressable(null)
        .noRippleClickable {
          showExtraPanel = !showExtraPanel
        }
        .padding(
          vertical = 8.dp
        ),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      val dayLabel = if (item.isToday()) {
        stringResource(R.string.common_today)
      } else {
        item.getDayOfWeekLabel()
      }
      Text(
        dayLabel,
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        fontWeight = FontWeight.W800,
      )
      Text(
        item.dayCondition,
        style = MiuixTheme.textStyles.title4,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        modifier = Modifier.weight(1f),
        textAlign = TextAlign.Center
      )
      Text(
        "${item.tempMin}℃",
        style = MiuixTheme.textStyles.title4,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        modifier = Modifier.width(50.dp),
        fontWeight = FontWeight.ExtraBold
      )
      TempProgress(
        modifier = Modifier
          .width(55.dp)
          .height(12.dp),
        data = item,
        limitLow = limitLow,
        limitHigh = limitHigh
      )
      Text(
        "${item.tempMax}℃",
        style = MiuixTheme.textStyles.title4,
        color = MiuixTheme.colorScheme.onSurfaceContainer,
        modifier = Modifier.width(50.dp),
        fontWeight = FontWeight.ExtraBold
      )
    }
    // 详细信息面板（可展开/收起）
    AnimatedVisibility(
      showExtraPanel
    ) {
      Column(
        modifier = Modifier
          .fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
      ) {
        // 第一行：湿度、UV级别
        Row(
          modifier = Modifier
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ItemInfo(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.daily_info_humidity),
            value = stringResource(R.string.daily_humidity_value, item.humidity),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.material_symbols_outlined_humidity_mid),
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
            )
          }
          ItemInfo(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.daily_info_uv_level),
            value = "${item.uvIndex}",
          ) {
            Icon(
              painter = painterResource(id = R.drawable.material_symbols_outlined_smart_outlet),
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
            )
          }
        }
        // 第二行：能见度、降水量
        Row(
          modifier = Modifier
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ItemInfo(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.daily_info_visibility),
            value = stringResource(R.string.daily_visibility_value, item.vis),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.material_symbols_outlined_eye_tracking),
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
            )
          }
          ItemInfo(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.daily_info_precipitation),
            value = stringResource(R.string.daily_precipitation_value, item.precipitation),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.material_symbols_outlined_grain),
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
            )
          }
        }
        // 第三行：风速、风向
        Row(
          modifier = Modifier
            .fillMaxWidth(),
          horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
          ItemInfo(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.daily_info_wind_speed),
            value = stringResource(R.string.daily_wind_speed_value, item.windSpeedDay),
          ) {
            Icon(
              painter = painterResource(id = R.drawable.material_symbols_outlined_wind_power),
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
            )
          }
          ItemInfo(
            modifier = Modifier.weight(1f),
            title = stringResource(R.string.daily_info_wind_direction),
            value = item.windDirDay,
          ) {
            Icon(
              painter = painterResource(id = R.drawable.material_symbols_outlined_arrow_upward),
              contentDescription = null,
              tint = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
              modifier = Modifier.rotate(item.windDirDay.toFloatOrNull() ?: 0f)
            )
          }
        }


      }
    }
  }
}


/**
 * 天气信息项组件
 * 用于展示单个天气属性的卡片，包含标题、数值和图标
 *
 * @param modifier 修饰符
 * @param title 信息标题（如"湿度"、"风速"等）
 * @param value 信息数值（如"80%"、"3米/秒"等）
 * @param icon 图标组件
 */
@Composable
fun ItemInfo(
  modifier: Modifier = Modifier,
  title: String,
  value: String,
  icon: @Composable () -> Unit
) {
  Row(
    modifier = modifier
      .background(MiuixTheme.colorScheme.secondaryContainer, WIDGET_CARD_CORNER_SHAPE)
      .padding(12.dp),
    horizontalArrangement = Arrangement.spacedBy(8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 左侧：标题和数值
    Column(
      modifier = Modifier.weight(1f),
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      Text(
        text = title,
        style = MiuixTheme.textStyles.title4,
        color = MiuixTheme.colorScheme.onSurface.copy(alpha = .8f)
      )
      Text(
        text = value,
        style = MiuixTheme.textStyles.body1,
        color = MiuixTheme.colorScheme.onSurface,
        fontWeight = FontWeight.W800
      )
    }
    icon()
  }
}


/**
 * 温度进度条组件
 * 使用渐变色线条可视化展示当天的温度范围
 * 颜色根据温度自动映射（冷色调表示低温，暖色调表示高温）
 *
 * @param modifier 修饰符
 * @param data 单日天气数据
 * @param limitLow 本周期内的最低温度
 * @param limitHigh 本周期内的最高温度
 */
@Composable
fun TempProgress(
  modifier: Modifier = Modifier,
  data: DailyForecast,
  limitLow: Int,
  limitHigh: Int
) {

  Log.d("TempProgress", "limitLow: $limitLow, limitHigh: $limitHigh")

  // 缓存 Paint 对象，避免在 drawWithCache 内重复创建
  val paint = remember {
    Paint().apply {
      isAntiAlias = true
      strokeCap = StrokeCap.Round
    }
  }

  Box(
    modifier = modifier
      .drawWithCache {
        // 创建温度渐变着色器（从低温色到高温色）
        val shader = LinearGradientShader(
          from = Offset.Zero,
          to = Offset(size.width, size.height),
          colors = listOf(
            limitLow.tempToColor(),
            limitHigh.tempToColor()
          )
        )
        // 更新画笔属性
        paint.shader = shader
        paint.strokeWidth = size.height * 0.7f

        // 计算当天最低温度在进度条上的起始位置
        val startOffset =
          Offset(
            size.width * ((data.tempMin - limitLow) * 1f / (limitHigh - limitLow)),
            size.height / 2
          )
        // 计算当天最高温度在进度条上的结束位置
        val endOffset =
          Offset(
            size.width * ((data.tempMax - limitLow) * 1f / (limitHigh - limitLow)),
            size.height / 2
          )


        onDrawWithContent {
          // 绘制背景底线（灰色）
          drawLine(
            COLOR_BLACK_10,
            Offset(0f, size.height / 2),
            Offset(size.width, size.height / 2),
            cap = StrokeCap.Round,
            strokeWidth = size.height
          )
          // 绘制温度范围渐变线（从最低温到最高温）
          drawIntoCanvas { canvas ->
            canvas.drawLine(
              startOffset,
              endOffset,
              paint
            )
          }
        }
      },
  )
}

/**
 * 温度转颜色扩展函数
 * 根据温度值返回对应的颜色
 * 颜色映射：
 * - 低于-10℃：深蓝色
 * - -10℃至0℃：浅蓝色
 * - 0℃至10℃：天蓝色
 * - 10℃至20℃：绿色
 * - 20℃至30℃：黄色
 * - 30℃以上：橙色
 */
private fun Int.tempToColor(): Color {
  if (this < -10) return Color(0xff4096ff)
  if (this < 0) return Color(0xffb5f5ec)
  if (this < 10) return Color(0xFF91caff)
  if (this < 20) return Color(0xffa0d911)
  if (this < 30) return Color(0xffffc53d)
  return Color(0xFFfa8c16)
}