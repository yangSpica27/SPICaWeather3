package me.spica.spicaweather3.ui.app_widget

import android.annotation.SuppressLint
import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
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
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextAlign
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.data.local.db.AppDatabase
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_10
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_100
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_40
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_50
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_60
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_70
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_80
import me.spica.spicaweather3.presentation.theme.COLOR_WHITE_90

/**
 * 散排天气小组件
 */
class ScatteredWeatherAppWidget : GlanceAppWidget() {

    @SuppressLint("RestrictedApi")
    override suspend fun provideGlance(
        context: Context,
        id: GlanceId
    ) = withContext(Dispatchers.IO) {
        val database = AppDatabase.build(context)

        val cityEntity = database.cityDao().getUserLoc()
        val weatherData = cityEntity?.weather

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .cornerRadius(20.dp)
                    .background(Color(0xCC1B2838))
            ) {
                if (cityEntity != null && weatherData != null) {
                    val temp = weatherData.current.temperature
                    val condition = weatherData.current.condition
                    val humidity = weatherData.current.humidity
                    val windScale = weatherData.current.windScale
                    val windDirText = weatherData.current.windDirectionText
                    val feelsLike = weatherData.current.feelsLike
                    val tempMax = weatherData.forecast.today.tempMax
                    val tempMin = weatherData.forecast.today.tempMin
                    val sunrise = weatherData.forecast.today.sunrise
                    val sunset = weatherData.forecast.today.sunset
                    val aqiCategory = weatherData.airQuality.category
                    val cityName = cityEntity.name

                    // 层1：幽灵背景大字（仅 10% 不透明度）——营造视觉纵深
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.BottomEnd
                    ) {
                        Text(
                            text = "$temp",
                            modifier = GlanceModifier.padding(end = 4.dp, bottom = 2.dp),
                            style = TextStyle(
                                fontSize = 112.sp,
                                fontWeight = FontWeight.Bold,
                                color = ColorProvider(COLOR_WHITE_10)
                            )
                        )
                    }

                    // 层2：主要内容
                    Column(
                        modifier = GlanceModifier
                            .fillMaxSize()
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.Top,
                        horizontalAlignment = Alignment.Start
                    ) {
                        // ── 第一行：城市名（左） + 温度区间（右） ──
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalAlignment = Alignment.Start
                        ) {
                            Text(
                                text = cityName,
                                style = TextStyle(
                                    fontSize = 14.sp,
                                    color = ColorProvider(COLOR_WHITE_70)
                                )
                            )
                            Spacer(GlanceModifier.defaultWeight())
                            Text(
                                text = "${tempMax}°~${tempMin}°",
                                style = TextStyle(
                                    fontSize = 12.sp,
                                    color = ColorProvider(COLOR_WHITE_50)
                                )
                            )
                        }

                        Spacer(GlanceModifier.height(6.dp))

                        // ── 第二行：大温度（左下对齐） + 条件详情（右） ──
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalAlignment = Alignment.Start
                        ) {
                            // 左侧：超大温度数字
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = "$temp",
                                    style = TextStyle(
                                        fontSize = 64.sp,
                                        fontWeight = FontWeight.Bold,
                                        color = ColorProvider(COLOR_WHITE_100)
                                    )
                                )
                                Text(
                                    text = "°",
                                    modifier = GlanceModifier.padding(bottom = 10.dp),
                                    style = TextStyle(
                                        fontSize = 22.sp,
                                        color = ColorProvider(COLOR_WHITE_80)
                                    )
                                )
                            }

                            Spacer(GlanceModifier.defaultWeight())

                            // 右侧：天气条件 + 细节（底部对齐）
                            Column(
                                horizontalAlignment = Alignment.End
                            ) {
                                Text(
                                    text = condition,
                                    style = TextStyle(
                                        fontSize = 22.sp,
                                        fontWeight = FontWeight.Medium,
                                        color = ColorProvider(COLOR_WHITE_90),
                                        textAlign = TextAlign.End
                                    )
                                )
                                Spacer(GlanceModifier.height(4.dp))
                                Text(
                                    text = "湿度 $humidity%",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = ColorProvider(COLOR_WHITE_50),
                                        textAlign = TextAlign.End
                                    )
                                )
                                Text(
                                    text = "$windDirText ${windScale}级",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = ColorProvider(COLOR_WHITE_50),
                                        textAlign = TextAlign.End
                                    )
                                )
                                Text(
                                    text = "空气 $aqiCategory",
                                    style = TextStyle(
                                        fontSize = 11.sp,
                                        color = ColorProvider(COLOR_WHITE_50),
                                        textAlign = TextAlign.End
                                    )
                                )
                            }
                        }

                        Spacer(GlanceModifier.height(6.dp))

                        // ── 第三行：日出日落（左） + 体感（右） ──
                        Row(
                            modifier = GlanceModifier.fillMaxWidth(),
                            verticalAlignment = Alignment.Bottom,
                            horizontalAlignment = Alignment.Start
                        ) {
                            // 日出日落两行
                            Column {
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "日出",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = ColorProvider(COLOR_WHITE_40)
                                        )
                                    )
                                    Spacer(GlanceModifier.width(4.dp))
                                    Text(
                                        text = sunrise,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = ColorProvider(COLOR_WHITE_60)
                                        )
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = "日落",
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = ColorProvider(COLOR_WHITE_40)
                                        )
                                    )
                                    Spacer(GlanceModifier.width(4.dp))
                                    Text(
                                        text = sunset,
                                        style = TextStyle(
                                            fontSize = 10.sp,
                                            color = ColorProvider(COLOR_WHITE_60)
                                        )
                                    )
                                }
                            }

                            Spacer(GlanceModifier.defaultWeight())

                            Text(
                                text = "体感 ${feelsLike}°",
                                style = TextStyle(
                                    fontSize = 10.sp,
                                    color = ColorProvider(COLOR_WHITE_40)
                                )
                            )
                        }
                    }
                } else {
                    // 无数据占位
                    Box(
                        modifier = GlanceModifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "没有数据 0_0",
                            style = TextStyle(
                                fontSize = 20.sp,
                                color = ColorProvider(COLOR_WHITE_60)
                            )
                        )
                    }
                }
            }
        }
    }
}
