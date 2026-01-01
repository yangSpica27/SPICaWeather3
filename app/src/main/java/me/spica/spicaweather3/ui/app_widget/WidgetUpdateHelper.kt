package me.spica.spicaweather3.ui.app_widget

import android.content.Context
import androidx.glance.appwidget.GlanceAppWidgetManager
import androidx.glance.appwidget.updateAll
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Widget 更新工具类
 * 提供手动刷新 Widget 的方法
 */
object WidgetUpdateHelper {

    /**
     * 更新所有的今日信息小组件
     * 通常在天气数据更新后调用
     */
    suspend fun updateTodayInfoWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                TodayInfoAppWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }
}
