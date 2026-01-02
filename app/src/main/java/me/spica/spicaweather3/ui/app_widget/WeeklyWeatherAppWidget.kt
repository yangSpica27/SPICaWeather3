package me.spica.spicaweather3.ui.app_widget

import android.annotation.SuppressLint
import android.content.Context
import android.util.Log
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Row
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.layout.width
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.db.AppDatabase
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import me.spica.spicaweather3.theme.COLOR_WHITE_40
import me.spica.spicaweather3.theme.COLOR_WHITE_60

/**
 * 一周天气小组件
 * 显示未来7天的天气预报
 */
class WeeklyWeatherAppWidget : GlanceAppWidget() {

  @SuppressLint("RestrictedApi")
  override suspend fun provideGlance(
    context: Context,
    id: GlanceId
  ) = withContext(Dispatchers.IO) {
    // 使用与应用相同的数据库名称和配置
    val database = Room.databaseBuilder(
      context.applicationContext,
      AppDatabase::class.java,
      "spica_weather.db"
    )
      .fallbackToDestructiveMigration(false)
      .build()

    // 获取用户位置的天气数据
    val cityEntity = database.cityDao().getUserLoc()
    val weatherData = cityEntity?.weather

    Log.e("WeeklyWeatherAppWidget", "provideGlance: 更新一周天气小组件 $cityEntity")

    provideContent {
      Box(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(16.dp)
          .padding(16.dp),
        contentAlignment = Alignment.TopStart
      ) {
        if (cityEntity != null && weatherData != null) {
          Column(
            modifier = GlanceModifier.fillMaxSize(),
            verticalAlignment = Alignment.Top,
            horizontalAlignment = Alignment.Start
          ) {
            // 标题：城市名称 + 当前温度
            Row(
              modifier = GlanceModifier.fillMaxWidth(),
              verticalAlignment = Alignment.CenterVertically,
              horizontalAlignment = Alignment.Start
            ) {
              Column {
                Text(
                  text = cityEntity.name,
                  style = TextStyle(
                    fontSize = 18.sp,
                    color = ColorProvider(COLOR_WHITE_100)
                  )
                )
                Spacer(modifier = GlanceModifier.height(2.dp))
                Row {
                  Text(
                    text = "${weatherData.todayWeather.temp}",
                    style = TextStyle(
                      fontSize = 32.sp,
                      color = ColorProvider(COLOR_WHITE_100)
                    )
                  )
                  Text(
                    text = "°C",
                    style = TextStyle(
                      fontSize = 16.sp,
                      color = ColorProvider(COLOR_WHITE_100)
                    )
                  )
                }
              }
            }

            Spacer(modifier = GlanceModifier.height(12.dp))

            // 一周天气列表
            val weeklyWeather = weatherData.dailyWeather.take(7)
            
            if (weeklyWeather.isNotEmpty()) {
              Column(
                modifier = GlanceModifier.fillMaxWidth(),
                verticalAlignment = Alignment.Top
              ) {
                weeklyWeather.forEach { day ->
                  Row(
                    modifier = GlanceModifier
                      .fillMaxWidth()
                      .padding(vertical = 3.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalAlignment = Alignment.Start
                  ) {
                    // 日期标签（周几）- 固定宽度
                    val dayLabel = try {
                      if (day.isToday()) "今天" else day.getDayOfWeekLabel()
                    } catch (e: Exception) {
                      day.fxTime.substring(5, 10)
                    }
                    
                    Box(
                      modifier = GlanceModifier.width(50.dp),
                      contentAlignment = Alignment.CenterStart
                    ) {
                      Text(
                        text = dayLabel,
                        style = TextStyle(
                          fontSize = 12.sp,
                          color = ColorProvider(COLOR_WHITE_60)
                        )
                      )
                    }

                    // 天气描述 - 固定宽度
                    Box(
                      modifier = GlanceModifier.width(80.dp),
                      contentAlignment = Alignment.CenterStart
                    ) {
                      Text(
                        text = day.getWeatherText(),
                        style = TextStyle(
                          fontSize = 12.sp,
                          color = ColorProvider(COLOR_WHITE_40)
                        )
                      )
                    }

                    // 温度范围 - 右对齐
                    Box(
                      modifier = GlanceModifier.defaultWeight(),
                      contentAlignment = Alignment.CenterEnd
                    ) {
                      Text(
                        text = "${day.maxTemp}° / ${day.minTemp}°",
                        style = TextStyle(
                          fontSize = 12.sp,
                          color = ColorProvider(COLOR_WHITE_100)
                        )
                      )
                    }
                  }
                }
              }
            } else {
              Text(
                text = "暂无一周天气数据",
                style = TextStyle(
                  fontSize = 14.sp,
                  color = ColorProvider(COLOR_WHITE_40)
                )
              )
            }
          }
        } else {
          Text(
            text = "没有数据 0_0",
            modifier = GlanceModifier,
            style = TextStyle(
              fontSize = 20.sp,
              color = ColorProvider(COLOR_WHITE_100)
            )
          )
        }
      }
    }
  }
}
