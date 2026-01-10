package me.spica.spicaweather3.core.constants

import androidx.compose.ui.unit.dp

/**
 * 主屏幕相关常量
 */
object MainScreenConstants {
  
  /**
   * 下拉刷新容器的顶部内边距
   * 用于避免内容被顶栏遮挡
   */
  val PULL_REFRESH_TOP_PADDING = 120.dp
  
  /**
   * 分页器预加载页面数量
   * 0 = 只加载当前页，优化内存占用
   * 1 = 预加载前后各一页，优化滑动体验
   */
  const val PAGER_BEYOND_VIEWPORT_PAGE_COUNT = 0
  
  /**
   * 刷新间隔阈值（毫秒）
   * 防止频繁刷新导致的性能问题
   */
  const val REFRESH_INTERVAL_MS = 3000L
}
