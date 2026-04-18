package me.spica.spicaweather3.ui.app_widget

import android.content.Context
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

    /**
     * 更新散排天气小组件
     */
    suspend fun updateScatteredWeatherWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                ScatteredWeatherAppWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新一周天气小组件
     */
    suspend fun updateWeeklyWeatherWidgets(context: Context) {
        withContext(Dispatchers.IO) {
            try {
                WeeklyWeatherAppWidget().updateAll(context)
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 更新所有小组件（天气数据刷新后统一调用）
     */
    suspend fun updateAllWidgets(context: Context) {
        updateTodayInfoWidgets(context)
        updateWeeklyWeatherWidgets(context)
        updateScatteredWeatherWidgets(context)
    }
}
