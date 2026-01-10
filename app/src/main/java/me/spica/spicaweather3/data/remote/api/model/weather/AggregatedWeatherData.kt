package me.spica.spicaweather3.data.remote.api.model.weather

import androidx.compose.runtime.Stable
import kotlinx.serialization.Serializable
import java.time.LocalDateTime
import java.time.OffsetDateTime


/**
 * 批量天气查询结果
 */
@Serializable
data class BatchWeatherResult(
  val locationId: String,
  val success: Boolean,
  val data: AggregatedWeatherData,
  val error: String? = null
)


/**
 * 聚合的天气数据
 * 整合了实时天气、未来预报、分钟级降水、空气质量和预警信息
 */
@Stable
@Serializable
data class AggregatedWeatherData(
  /**
   * 数据生成时间
   * 2026-01-07T20:57:13.365641330
   */
  val generatedAt: String = LocalDateTime.now().toString(),

  /**
   * 位置信息（可选，从查询参数获取）
   */
  val location: LocationInfo,

  /**
   * 实时天气
   */
  val current: CurrentWeather,

  /**
   * 未来10天预报（简化版）
   */
  val forecast: ForecastSummary,

  /**
   * 分钟级降水预报
   */
  val minutelyPrecip: MinutelyPrecipSummary? = null,

  /**
   * 空气质量
   */
  val airQuality: AirQualitySummary,

  /**
   * 天气预警
   */
  val weatherAlerts: List<WeatherAlertSummary>? = null
) {
  /**
   * 获取数据生成时间的 LocalDateTime 对象
   */
  fun getGeneratedAtDateTime(): LocalDateTime {
    return LocalDateTime.parse(generatedAt)
  }
}

/**
 * 位置信息
 */
@Serializable
data class LocationInfo(
  val name: String,
  val latitude: String,
  val longitude: String
)

/**
 * 实时天气信息
 */
@Serializable
data class CurrentWeather(
  /**
   * 观测时间
   * 2026-01-07T20:48+08:00
   */
  val obsTime: String,

  /**
   * 温度（摄氏度）
   */
  val temperature: Int,

  /**
   * 体感温度（摄氏度）
   */
  val feelsLike: Int,

  /**
   * 天气状况文字
   */
  val condition: String,

  /**
   * 天气图标代码
   */
  val icon: String,

  /**
   * 相对湿度（百分比）
   */
  val humidity: Int,

  /**
   * 降水量（毫米）
   */
  val precipitation: Double,

  /**
   * 大气压强（百帕）
   */
  val pressure: Int,

  /**
   * 能见度（公里）
   */
  val visibility: Int,

  /**
   * 风向（角度）
   */
  val windDirection: Int,

  /**
   * 风向文字
   */
  val windDirectionText: String,

  /**
   * 风力等级
   */
  val windScale: String,

  /**
   * 风速（公里/小时）
   */
  val windSpeed: Int,

  /**
   * 云量（百分比）
   */
  val cloudCover: Int
) {

  /**
   * 获取观测时间的 LocalDateTime 对象
   */
  fun getObsDateTime(): LocalDateTime {
    return OffsetDateTime.parse(obsTime).toLocalDateTime()
  }

}

/**
 * 预报摘要
 */
@Serializable
data class ForecastSummary(
  /**
   * 今日预报
   */
  val today: DailyForecast,

  /**
   * 明日预报
   */
  val tomorrow: DailyForecast,

  /**
   * 未来7天预报（包含今天和明天）
   */
  val next7Days: List<DailyForecast>,

  /**
   * 未来24小时预报
   */
  val next24Hours: List<HourlyForecast>?
)

/**
 * 每日预报
 */
@Serializable
data class DailyForecast(
  /**
   * 预报日期
   */
  val date: String,

  /**
   * 最高温度（摄氏度）
   */
  val tempMax: Int,

  /**
   * 最低温度（摄氏度）
   */
  val tempMin: Int,

  /**
   * 白天天气
   */
  val dayCondition: String,

  /**
   * 白天天气图标
   */
  val dayIcon: String,

  /**
   * 夜间天气
   */
  val nightCondition: String,

  /**
   * 夜间天气图标
   */
  val nightIcon: String,

  /**
   * 降水概率（百分比，如果可用）
   */
  val precipitation: Double,

  /**
   * 湿度（百分比）
   */
  val humidity: Int,

  /**
   * 紫外线指数
   */
  val uvIndex: Int,

  /**
   * 日出时间
   */
  val sunrise: String,

  /**
   * 日落时间
   */
  val sunset: String,

  /**
   * 可见度
   */
  val vis: String,

  /**
   * 云量
   */
  val cloud: String,

  /**
   * 白天风向
   */
  val wind360Day: Double,

  /**
   * 夜间风向
   */
  val wind360Night: Double,


  /**
   * 白天风向
   */
  val windDirDay: String,

  /**
   * 晚上风向
   */
  val windDirNight: String,

  /**
   * 风速日间
   */
  val windSpeedDay: String,

  /**
   * 风速夜间
   */
  val windSpeedNight: String,

  /**
   * 风力等级
   */
  val windScaleDay: String,

  /**
   * 风力等级
   */
  val windScaleNight: String,
) {
  /**
   * 获取预报日期的 LocalDate 对象
   */
  fun getDateAsLocalDate(): LocalDateTime {
    return LocalDateTime.parse("${date}T00:00:00")
  }

  /**
   * 判断预报日期是否为今天
   */
  fun isToday(): Boolean {
    val today = LocalDateTime.now()
    val forecastDate = getDateAsLocalDate()
    return today.year == forecastDate.year &&
        today.monthValue == forecastDate.monthValue &&
        today.dayOfMonth == forecastDate.dayOfMonth
  }

  fun getDayOfWeekLabel(): String {
    val forecastDate = getDateAsLocalDate()
    return when (forecastDate.dayOfWeek.value) {
      1 -> "周一"
      2 -> "周二"
      3 -> "周三"
      4 -> "周四"
      5 -> "周五"
      6 -> "周六"
      7 -> "周日"
      else -> ""
    }
  }

  fun getCondition(): String {
    val isNight = LocalDateTime.now().hour !in 6..<18
    return if (isNight) nightCondition else dayCondition
  }

}

/**
 * 小时预报
 */
@Serializable
data class HourlyForecast(
  /**
   * 预报时间
   */
  val time: String,

  /**
   * 温度（摄氏度）
   */
  val temperature: Int,

  /**
   * 天气状况
   */
  val condition: String,

  /**
   * 天气图标
   */
  val icon: String,

  /**
   * 降水概率（百分比）
   */
  val precipProbability: Int,

  /**
   * 降水量（毫米）
   */
  val precipitation: Double,

  /**
   * 风向文字
   */
  val windDirection: String,

  /**
   * 风力等级
   */
  val windScale: String,

  /**
   * 湿度（百分比）
   */
  val humidity: Int,

  /**
   * 风向
   */
  val wind360: Double,

  /**
   * 降水概率
   */
  val pop: Double,

  /**
   * 风速
   */
  val windSpeed: Double,
) {
  /**
   * 获取预报时间的 LocalDateTime 对象
   */
  fun getTimeAsLocalDateTime(): LocalDateTime {
    return OffsetDateTime.parse(time).toLocalDateTime()
  }
}

/**
 * 分钟级降水预报摘要
 */
@Serializable
data class MinutelyPrecipSummary(
  /**
   * 降水摘要文字
   */
  val summary: String,

  /**
   * 是否正在降水
   */
  val isPrecipitating: Boolean,

  /**
   * 降水类型（rain/snow）
   */
  val precipType: String?,

  /**
   * 当前降水强度（毫米/小时）
   */
  val currentIntensity: Double,

  /**
   * 未来2小时降水强度
   */
  val next2Hours: List<MinutelyPrecip>
)

/**
 * 分钟级降水数据点
 */
@Serializable
data class MinutelyPrecip(
  /**
   * 2026-01-08T08:00+08:00
   */
  val time: String,
  val precipitation: Double,
  val type: String
) {
  /**
   * 获取时间的 LocalDateTime 对象
   */
  fun getTimeAsLocalDateTime(): LocalDateTime {
    return OffsetDateTime.parse(time).toLocalDateTime()
  }
}

/**
 * 空气质量摘要
 */
@Serializable
data class AirQualitySummary(
  /**
   * AQI 值（美国标准）
   */
  val aqi: Int,

  /**
   * 空气质量等级（1-优 2-良 3-轻度污染 4-中度污染 5-重度污染 6-严重污染）
   */
  val level: Int,

  /**
   * 空气质量类别文字
   */
  val category: String,

  /**
   * 主要污染物
   */
  val primaryPollutant: String,

  /**
   * 主要污染物全称
   */
  val primaryPollutantName: String,

  /**
   * 健康影响
   */
  val healthEffect: String,

  /**
   * 健康建议（一般人群）
   */
  val healthAdvice: String,

  /**
   * PM2.5 浓度（μg/m³）
   */
  val pm25: Double?,

  /**
   * PM10 浓度（μg/m³）
   */
  val pm10: Double?
)

/**
 * 天气预警摘要
 */
@Serializable
data class WeatherAlertSummary(
  /**
   * 预警ID
   */
  val id: String,

  /**
   * 预警标题
   */
  val headline: String,

  /**
   * 预警类型
   */
  val eventType: String,

  /**
   * 预警类型代码
   */
  val eventCode: String,

  /**
   * 严重程度
   */
  val severity: String,

  /**
   * 预警颜色代码
   */
  val colorCode: String,

  /**
   * 预警描述
   */
  val description: String,

  /**
   * 预警指引
   */
  val instruction: String,

  /**
   * 发布时间
   */
  val issuedTime: String,

  /**
   * 生效时间
   */
  val effectiveTime: String,

  /**
   * 过期时间
   */
  val expireTime: String
)
