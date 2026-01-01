package me.spica.spicaweather3.ui.app_widget

import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.GlanceAppWidgetReceiver

/**
 * 今日信息小组件接收器
 * 用于接收和处理 Widget 的系统广播
 */
class TodayInfoAppWidgetReceiver : GlanceAppWidgetReceiver() {
    
    override val glanceAppWidget: GlanceAppWidget
        get() = TodayInfoAppWidget()
}
