package me.spica.spicaweather3.common.config

import me.spica.spicaweather3.BuildConfig

/**
 * 和风天气 API 配置
 *
 */
object HefengConfig {

  const val APIKEY: String = BuildConfig.HEFENG_API_KEY

  const val BASE_URL = "https://n85egdbbrr.re.qweatherapi.com/"

  const val HEADER = "X-QW-Api-Key"

}