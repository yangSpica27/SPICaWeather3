package me.spica.spicaweather3.data.mapper

import me.spica.spicaweather3.data.local.db.entity.CityEntity
import me.spica.spicaweather3.data.remote.api.model.Location
import me.spica.spicaweather3.data.remote.api.model.weather.*
import me.spica.spicaweather3.domain.model.*
import me.spica.spicaweather3.domain.model.LocationInfo as DomainLocationInfo
import me.spica.spicaweather3.domain.model.WeatherData as DomainWeatherData

/**
 * 数据映射器 - 在 data 层和 domain 层之间转换数据
 */

// ============ CityEntity <-> City ============

fun CityEntity.toDomain(): City = City(
    id = id,
    name = name,
    latitude = lat,
    longitude = lon,
    administrativeArea1 = adm1,
    administrativeArea2 = adm2,
    sortOrder = sort,
    isUserLocation = isUserLoc,
    weather = weather?.toDomain()
)

fun City.toEntity(): CityEntity = CityEntity(
    id = id,
    name = name,
    lat = latitude,
    lon = longitude,
    adm1 = administrativeArea1,
    adm2 = administrativeArea2,
    sort = sortOrder,
    isUserLoc = isUserLocation,
    weather = weather?.toDataModel()
)

// ============ Location -> SearchLocation ============

fun Location.toSearchLocation(): SearchLocation = SearchLocation(
    id = id,
    name = name,
    latitude = lat,
    longitude = lon,
    administrativeArea1 = adm1,
    administrativeArea2 = adm2,
    country = country,
    type = type,
    rank = rank
)

fun SearchLocation.toCity(): City = City(
    id = id,
    name = name,
    latitude = latitude,
    longitude = longitude,
    administrativeArea1 = administrativeArea1,
    administrativeArea2 = administrativeArea2,
    sortOrder = System.currentTimeMillis(),
    isUserLocation = false,
    weather = null
)

// ============ AggregatedWeatherData <-> WeatherData ============

fun AggregatedWeatherData.toDomain(): DomainWeatherData = DomainWeatherData(
    generatedAt = generatedAt,
    location = location.toDomain(),
    current = current.toDomain(),
    forecast = forecast.toDomain(),
    minutelyPrecip = minutelyPrecip?.toDomain(),
    airQuality = airQuality.toDomain(),
    weatherAlerts = weatherAlerts?.map { it.toDomain() }
)

fun DomainWeatherData.toDataModel(): AggregatedWeatherData = AggregatedWeatherData(
    generatedAt = generatedAt,
    location = location.toDataModel(),
    current = current.toDataModel(),
    forecast = forecast.toDataModel(),
    minutelyPrecip = minutelyPrecip?.toDataModel(),
    airQuality = airQuality.toDataModel(),
    weatherAlerts = weatherAlerts?.map { it.toDataModel() }
)

// ============ LocationInfo ============

fun me.spica.spicaweather3.data.remote.api.model.weather.LocationInfo.toDomain(): DomainLocationInfo =
    DomainLocationInfo(
        name = name,
        latitude = latitude,
        longitude = longitude
    )

fun DomainLocationInfo.toDataModel(): me.spica.spicaweather3.data.remote.api.model.weather.LocationInfo =
    me.spica.spicaweather3.data.remote.api.model.weather.LocationInfo(
        name = name,
        latitude = latitude,
        longitude = longitude
    )

// ============ CurrentWeather ============

fun me.spica.spicaweather3.data.remote.api.model.weather.CurrentWeather.toDomain(): me.spica.spicaweather3.domain.model.CurrentWeather =
    me.spica.spicaweather3.domain.model.CurrentWeather(
        obsTime = obsTime,
        temperature = temperature,
        feelsLike = feelsLike,
        condition = condition,
        icon = icon,
        humidity = humidity,
        precipitation = precipitation,
        pressure = pressure,
        visibility = visibility,
        windDirection = windDirection,
        windDirectionText = windDirectionText,
        windScale = windScale,
        windSpeed = windSpeed,
        cloudCover = cloudCover
    )

fun me.spica.spicaweather3.domain.model.CurrentWeather.toDataModel(): me.spica.spicaweather3.data.remote.api.model.weather.CurrentWeather =
    me.spica.spicaweather3.data.remote.api.model.weather.CurrentWeather(
        obsTime = obsTime,
        temperature = temperature,
        feelsLike = feelsLike,
        condition = condition,
        icon = icon,
        humidity = humidity,
        precipitation = precipitation,
        pressure = pressure,
        visibility = visibility,
        windDirection = windDirection,
        windDirectionText = windDirectionText,
        windScale = windScale,
        windSpeed = windSpeed,
        cloudCover = cloudCover
    )

// ============ ForecastSummary <-> ForecastData ============

fun ForecastSummary.toDomain(): ForecastData = ForecastData(
    today = today.toDomain(),
    tomorrow = tomorrow.toDomain(),
    next7Days = next7Days.map { it.toDomain() },
    next24Hours = next24Hours?.map { it.toDomain() }
)

fun ForecastData.toDataModel(): ForecastSummary = ForecastSummary(
    today = today.toDataModel(),
    tomorrow = tomorrow.toDataModel(),
    next7Days = next7Days.map { it.toDataModel() },
    next24Hours = next24Hours?.map { it.toDataModel() }
)

// ============ DailyForecast ============

fun me.spica.spicaweather3.data.remote.api.model.weather.DailyForecast.toDomain(): me.spica.spicaweather3.domain.model.DailyForecast =
    me.spica.spicaweather3.domain.model.DailyForecast(
        date = date,
        tempMax = tempMax,
        tempMin = tempMin,
        dayCondition = dayCondition,
        dayIcon = dayIcon,
        nightCondition = nightCondition,
        nightIcon = nightIcon,
        precipitation = precipitation,
        humidity = humidity,
        uvIndex = uvIndex,
        sunrise = sunrise,
        sunset = sunset,
        visibility = vis,
        cloud = cloud,
        wind360Day = wind360Day,
        wind360Night = wind360Night,
        windDirDay = windDirDay,
        windDirNight = windDirNight,
        windSpeedDay = windSpeedDay,
        windSpeedNight = windSpeedNight,
        windScaleDay = windScaleDay,
        windScaleNight = windScaleNight
    )

fun me.spica.spicaweather3.domain.model.DailyForecast.toDataModel(): me.spica.spicaweather3.data.remote.api.model.weather.DailyForecast =
    me.spica.spicaweather3.data.remote.api.model.weather.DailyForecast(
        date = date,
        tempMax = tempMax,
        tempMin = tempMin,
        dayCondition = dayCondition,
        dayIcon = dayIcon,
        nightCondition = nightCondition,
        nightIcon = nightIcon,
        precipitation = precipitation,
        humidity = humidity,
        uvIndex = uvIndex,
        sunrise = sunrise,
        sunset = sunset,
        vis = visibility,
        cloud = cloud,
        wind360Day = wind360Day,
        wind360Night = wind360Night,
        windDirDay = windDirDay,
        windDirNight = windDirNight,
        windSpeedDay = windSpeedDay,
        windSpeedNight = windSpeedNight,
        windScaleDay = windScaleDay,
        windScaleNight = windScaleNight
    )

// ============ HourlyForecast ============

fun me.spica.spicaweather3.data.remote.api.model.weather.HourlyForecast.toDomain(): me.spica.spicaweather3.domain.model.HourlyForecast =
    me.spica.spicaweather3.domain.model.HourlyForecast(
        time = time,
        temperature = temperature,
        condition = condition,
        icon = icon,
        precipProbability = precipProbability,
        precipitation = precipitation,
        windDirection = windDirection,
        windScale = windScale,
        humidity = humidity,
        wind360 = wind360,
        pop = pop,
        windSpeed = windSpeed
    )

fun me.spica.spicaweather3.domain.model.HourlyForecast.toDataModel(): me.spica.spicaweather3.data.remote.api.model.weather.HourlyForecast =
    me.spica.spicaweather3.data.remote.api.model.weather.HourlyForecast(
        time = time,
        temperature = temperature,
        condition = condition,
        icon = icon,
        precipProbability = precipProbability,
        precipitation = precipitation,
        windDirection = windDirection,
        windScale = windScale,
        humidity = humidity,
        wind360 = wind360,
        pop = pop,
        windSpeed = windSpeed
    )

// ============ MinutelyPrecipSummary <-> MinutelyPrecipData ============

fun MinutelyPrecipSummary.toDomain(): MinutelyPrecipData = MinutelyPrecipData(
    summary = summary,
    isPrecipitating = isPrecipitating,
    precipType = precipType,
    currentIntensity = currentIntensity,
    next2Hours = next2Hours.map { it.toDomain() }
)

fun MinutelyPrecipData.toDataModel(): MinutelyPrecipSummary = MinutelyPrecipSummary(
    summary = summary,
    isPrecipitating = isPrecipitating,
    precipType = precipType,
    currentIntensity = currentIntensity,
    next2Hours = next2Hours.map { it.toDataModel() }
)

// ============ MinutelyPrecip ============

fun me.spica.spicaweather3.data.remote.api.model.weather.MinutelyPrecip.toDomain(): me.spica.spicaweather3.domain.model.MinutelyPrecip =
    me.spica.spicaweather3.domain.model.MinutelyPrecip(
        time = time,
        precipitation = precipitation,
        type = type
    )

fun me.spica.spicaweather3.domain.model.MinutelyPrecip.toDataModel(): me.spica.spicaweather3.data.remote.api.model.weather.MinutelyPrecip =
    me.spica.spicaweather3.data.remote.api.model.weather.MinutelyPrecip(
        time = time,
        precipitation = precipitation,
        type = type
    )

// ============ AirQualitySummary <-> AirQualityData ============

fun AirQualitySummary.toDomain(): AirQualityData = AirQualityData(
    aqi = aqi,
    level = level,
    category = category,
    primaryPollutant = primaryPollutant,
    primaryPollutantName = primaryPollutantName,
    healthEffect = healthEffect,
    healthAdvice = healthAdvice,
    pm25 = pm25,
    pm10 = pm10
)

fun AirQualityData.toDataModel(): AirQualitySummary = AirQualitySummary(
    aqi = aqi,
    level = level,
    category = category,
    primaryPollutant = primaryPollutant,
    primaryPollutantName = primaryPollutantName,
    healthEffect = healthEffect,
    healthAdvice = healthAdvice,
    pm25 = pm25,
    pm10 = pm10
)

// ============ WeatherAlertSummary <-> WeatherAlert ============

fun WeatherAlertSummary.toDomain(): WeatherAlert = WeatherAlert(
    id = id,
    headline = headline,
    eventType = eventType,
    eventCode = eventCode,
    severity = severity,
    colorCode = colorCode,
    description = description,
    instruction = instruction,
    issuedTime = issuedTime,
    effectiveTime = effectiveTime,
    expireTime = expireTime
)

fun WeatherAlert.toDataModel(): WeatherAlertSummary = WeatherAlertSummary(
    id = id,
    headline = headline,
    eventType = eventType,
    eventCode = eventCode,
    severity = severity,
    colorCode = colorCode,
    description = description,
    instruction = instruction,
    issuedTime = issuedTime,
    effectiveTime = effectiveTime,
    expireTime = expireTime
)
