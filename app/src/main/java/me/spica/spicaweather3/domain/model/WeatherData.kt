package me.spica.spicaweather3.domain.model

/**
 * 领域模型 - 聚合的天气数据
 */
data class WeatherData(
    val generatedAt: String,
    val location: LocationInfo,
    val current: CurrentWeather,
    val forecast: ForecastData,
    val minutelyPrecip: MinutelyPrecipData?,
    val airQuality: AirQualityData,
    val weatherAlerts: List<WeatherAlert>?
)

/**
 * 位置信息
 */
data class LocationInfo(
    val name: String,
    val latitude: String,
    val longitude: String
)

/**
 * 实时天气
 */
data class CurrentWeather(
    val obsTime: String,
    val temperature: Int,
    val feelsLike: Int,
    val condition: String,
    val icon: String,
    val humidity: Int,
    val precipitation: Double,
    val pressure: Int,
    val visibility: Int,
    val windDirection: Int,
    val windDirectionText: String,
    val windScale: String,
    val windSpeed: Int,
    val cloudCover: Int
)

/**
 * 预报数据
 */
data class ForecastData(
    val today: DailyForecast,
    val tomorrow: DailyForecast,
    val next7Days: List<DailyForecast>,
    val next24Hours: List<HourlyForecast>?
)

/**
 * 每日预报
 */
data class DailyForecast(
    val date: String,
    val tempMax: Int,
    val tempMin: Int,
    val dayCondition: String,
    val dayIcon: String,
    val nightCondition: String,
    val nightIcon: String,
    val precipitation: Double,
    val humidity: Int,
    val uvIndex: Int,
    val sunrise: String,
    val sunset: String,
    val visibility: String,
    val cloud: String,
    val wind360Day: Double,
    val wind360Night: Double,
    val windDirDay: String,
    val windDirNight: String,
    val windSpeedDay: String,
    val windSpeedNight: String,
    val windScaleDay: String,
    val windScaleNight: String
)

/**
 * 小时预报
 */
data class HourlyForecast(
    val time: String,
    val temperature: Int,
    val condition: String,
    val icon: String,
    val precipProbability: Int,
    val precipitation: Double,
    val windDirection: String,
    val windScale: String,
    val humidity: Int,
    val wind360: Double,
    val pop: Double,
    val windSpeed: Double
)

/**
 * 分钟级降水预报
 */
data class MinutelyPrecipData(
    val summary: String,
    val isPrecipitating: Boolean,
    val precipType: String?,
    val currentIntensity: Double,
    val next2Hours: List<MinutelyPrecip>
)

/**
 * 分钟级降水数据点
 */
data class MinutelyPrecip(
    val time: String,
    val precipitation: Double,
    val type: String
)

/**
 * 空气质量
 */
data class AirQualityData(
    val aqi: Int,
    val level: Int,
    val category: String,
    val primaryPollutant: String,
    val primaryPollutantName: String,
    val healthEffect: String,
    val healthAdvice: String,
    val pm25: Double?,
    val pm10: Double?
)

/**
 * 天气预警
 */
data class WeatherAlert(
    val id: String,
    val headline: String,
    val eventType: String,
    val eventCode: String,
    val severity: String,
    val colorCode: String,
    val description: String,
    val instruction: String,
    val issuedTime: String,
    val effectiveTime: String,
    val expireTime: String
)
