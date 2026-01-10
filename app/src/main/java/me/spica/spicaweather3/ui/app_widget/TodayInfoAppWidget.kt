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
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import androidx.room.Room
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.data.local.db.AppDatabase
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_100
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_40


/**
 * 今日信息小组件
 * 显示当前天气、温度、城市信息等
 */
class TodayInfoAppWidget : GlanceAppWidget() {

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

    Log.e("TodayInfoAppWidget", "provideGlance: 更新小组件 $cityEntity $weatherData")

    provideContent {
      Box(
        modifier = GlanceModifier
          .fillMaxSize()
          .cornerRadius(16.dp)
          .padding(12.dp),
        contentAlignment = Alignment.Center
      ) {
        if (cityEntity != null && weatherData != null) {
          Row(
            modifier = GlanceModifier,
            verticalAlignment = Alignment.CenterVertically,
            horizontalAlignment = Alignment.Start
          ) {
            Column(
              modifier = GlanceModifier,
              horizontalAlignment = Alignment.CenterHorizontally
            ) {
              Text(
                text = cityEntity.name,
                modifier = GlanceModifier,
                TextStyle(
                  fontSize = 24.sp,
                  color = ColorProvider(COLOR_WHITE_100)
                )
              )
              Spacer(
                modifier = GlanceModifier
                  .height(4.dp)
              )
              Row {
                Text(
                  text = "${weatherData.current.temperature}",
                  modifier = GlanceModifier,
                  style = TextStyle(
                    fontSize = 54.sp,
                    color = ColorProvider(COLOR_WHITE_100)
                  )
                )
                Text(
                  text = "°C",
                  modifier = GlanceModifier,
                  style = TextStyle(
                    fontSize = 24.sp,
                    color = ColorProvider(COLOR_WHITE_100)
                  )
                )
              }
              Spacer(
                modifier = GlanceModifier
                  .height(4.dp)
              )
              Text(
                text = "${weatherData.current.condition} 风速${weatherData.current.windSpeed}公里/小时",
                modifier = GlanceModifier,
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
              fontSize = 32.sp,
              color = ColorProvider(COLOR_WHITE_100)
            )
          )
        }
      }
    }
  }

}



