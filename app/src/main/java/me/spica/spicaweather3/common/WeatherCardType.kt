package me.spica.spicaweather3.common

import kotlinx.serialization.Serializable

/**
 * 天气页面卡片类型枚举
 * 用于拖拽排序和动态显示/隐藏功能
 */
@Serializable
enum class WeatherCardType(
  val key: String,
  val displayName: String,
  val description: String,
  val isRemovable: Boolean = true,
  val defaultOrder: Int,
  val spanSize: Int = 1,
) {

  /**
   * 当前天气卡片 - 不可移除
   */
  NOW(
    key = "now",
    displayName = "当前天气",
    description = "显示当前温度、天气状况",
    isRemovable = false,
    defaultOrder = 0,
    spanSize = 2
  ),

  /**
   * 预警卡片 - 动态显示（有预警时才显示）
   */
  ALERT(
    key = "alert",
    displayName = "天气预警",
    description = "显示气象预警信息",
    isRemovable = false,
    defaultOrder = 1,
    spanSize = 2
  ),

  /**
   * 分钟级降水卡片
   */
  MINUTELY(
    key = "minutely",
    displayName = "分钟级降水",
    description = "未来2小时降水预报",
    isRemovable = true,
    defaultOrder = 2,
    spanSize = 2
  ),

  /**
   * 逐小时预报卡片
   */
  HOURLY(
    key = "hourly",
    displayName = "逐小时预报",
    description = "24小时天气趋势",
    isRemovable = true,
    defaultOrder = 3,
    spanSize = 2
  ),

  /**
   * 逐日预报卡片
   */
  DAILY(
    key = "daily",
    displayName = "逐日预报",
    description = "未来15天天气趋势",
    isRemovable = true,
    defaultOrder = 4,
    spanSize = 2
  ),

  /**
   * 紫外线指数卡片
   */
  UV(
    key = "uv",
    displayName = "紫外线指数",
    description = "当日紫外线强度",
    isRemovable = true,
    defaultOrder = 5,
    spanSize = 1
  ),

  /**
   * 体感温度卡片
   */
  FEEL_TEMP(
    key = "feel_temp",
    displayName = "体感温度",
    description = "实际体感温度",
    isRemovable = true,
    defaultOrder = 6,
    spanSize = 1
  ),

  /**
   * 降水量卡片
   */
  PRECIPITATION(
    key = "precipitation",
    displayName = "降水量",
    description = "降水量和降水概率",
    isRemovable = true,
    defaultOrder = 7,
    spanSize = 1
  ),

  /**
   * 湿度卡片
   */
  HUMIDITY(
    key = "humidity",
    displayName = "湿度",
    description = "当前空气湿度",
    isRemovable = true,
    defaultOrder = 8,
    spanSize = 1
  ),

  /**
   * 日出日落卡片
   */
  SUNRISE(
    key = "sunrise",
    displayName = "日出日落",
    description = "今日日出日落时间",
    isRemovable = true,
    defaultOrder = 9,
    spanSize = 2
  ),

  /**
   * 空气质量卡片
   */
  AQI(
    key = "aqi",
    displayName = "空气质量",
    description = "空气质量指数详情",
    isRemovable = true,
    defaultOrder = 10,
    spanSize = 2
  );

  companion object {
    /**
     * 从 key 获取卡片类型
     */
    fun fromKey(key: String): WeatherCardType? {
      return entries.find { it.key == key }
    }

    /**
     * 获取默认显示的卡片列表（按默认顺序）
     */
    fun getDefaultCards(): List<WeatherCardType> {
      return entries.sortedBy { it.defaultOrder }
    }

    /**
     * 获取可移除的卡片列表
     */
    fun getRemovableCards(): List<WeatherCardType> {
      return entries.filter { it.isRemovable }
    }
  }
}

/**
 * 天气卡片配置数据类
 * 用于保存用户的卡片显示和排序配置
 */
@Serializable
data class WeatherCardConfig(
  val cardType: WeatherCardType,
  val isVisible: Boolean = true,
  val order: Int = cardType.defaultOrder
)

/**
 * 卡片配置列表的扩展函数
 */
fun List<WeatherCardConfig>.sortedByOrder(): List<WeatherCardConfig> {
  return this.sortedBy { it.order }
}

fun List<WeatherCardConfig>.getVisibleCards(): List<WeatherCardConfig> {
  return this.filter { it.isVisible }.sortedByOrder()
}

fun List<WeatherCardConfig>.getHiddenCards(): List<WeatherCardConfig> {
  return this.filter { !it.isVisible }.sortedBy { it.cardType.displayName }
}
