package me.spica.spicaweather3.ui.main.weather

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.spica.spicaweather3.common.model.WeatherCardType
import me.spica.spicaweather3.data.remote.api.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.ui.main.cards.AlertCard
import me.spica.spicaweather3.ui.main.cards.AqiCard
import me.spica.spicaweather3.ui.main.cards.DailyCard
import me.spica.spicaweather3.ui.main.cards.FeelTempCard
import me.spica.spicaweather3.ui.main.cards.HourlyCard
import me.spica.spicaweather3.ui.main.cards.HumidityCard
import me.spica.spicaweather3.ui.main.cards.MinutelyCard
import me.spica.spicaweather3.ui.main.cards.NowCard
import me.spica.spicaweather3.ui.main.cards.PrecipitationCard
import me.spica.spicaweather3.ui.main.cards.SunriseCard
import me.spica.spicaweather3.ui.main.cards.UVCard
import me.spica.spicaweather3.ui.main.cards.WindCard

/**
 * 天气卡片工厂
 */
object WeatherCardFactory {
  
  /**
   * 根据卡片类型创建对应的天气卡片组件
   * 
   * @param cardType 卡片类型
   * @param weatherData 天气数据
   * @param startAnim 是否开始播放入场动画
   * @param modifier 修饰符
   */
  @Composable
  fun CreateCard(
    cardType: WeatherCardType,
    weatherData: AggregatedWeatherData,
    startAnim: Boolean,
    modifier: Modifier = Modifier
  ) {
    when (cardType) {
      WeatherCardType.NOW -> NowCard(
        modifier = modifier,
        weatherData = weatherData,
        startAnim = startAnim
      )
      
      WeatherCardType.ALERT -> AlertCard(weatherData)
      
      WeatherCardType.MINUTELY -> MinutelyCard(
        modifier = modifier,
        weatherData = weatherData
      )
      
      WeatherCardType.HOURLY -> HourlyCard(
        modifier = modifier,
        weatherData = weatherData
      )
      
      WeatherCardType.DAILY -> DailyCard(data = weatherData)
      
      WeatherCardType.UV -> UVCard(
        weatherData.forecast.today.uvIndex,
        startAnim
      )
      
      WeatherCardType.FEEL_TEMP -> FeelTempCard(
        feelTemp = weatherData.current.feelsLike,
        startAnim = startAnim
      )
      
      WeatherCardType.PRECIPITATION -> PrecipitationCard(
        precipitation = weatherData.current.precipitation.toInt(),
        pop = weatherData.forecast.next24Hours?.firstOrNull()?.pop?.toInt() ?: 0,
        startAnim = startAnim
      )
      
      WeatherCardType.HUMIDITY -> HumidityCard(
        humidity = weatherData.current.humidity,
        startAnim = startAnim
      )
      
      WeatherCardType.SUNRISE -> SunriseCard(
        weatherEntity = weatherData,
        startAnim = startAnim
      )
      
      WeatherCardType.WIND -> WindCard(
        startAnim = startAnim,
        weatherEntity = weatherData
      )
      
      WeatherCardType.AQI -> AqiCard(
        airQualitySummary = weatherData.airQuality,
        startAnim = startAnim
      )
    }
  }
}
