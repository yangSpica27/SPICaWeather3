package me.spica.spicaweather3.ui.app_widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 散排天气小组件 BroadcastReceiver
 */
class ScatteredWeatherAppWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = ScatteredWeatherAppWidget()
}
