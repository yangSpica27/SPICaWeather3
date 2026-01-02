package me.spica.spicaweather3.ui.app_widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 一周天气小组件接收器
 * 用于接收系统的小组件更新广播
 */
class WeeklyWeatherAppWidgetReceiver : GlanceAppWidgetReceiver() {
  override val glanceAppWidget: GlanceAppWidget = WeeklyWeatherAppWidget()
}
