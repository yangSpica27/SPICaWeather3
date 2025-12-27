package me.spica.spicaweather3.network

import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import me.spica.spicaweather3.common.HefengConfig
import me.spica.spicaweather3.network.model.LookUpResponse
import me.spica.spicaweather3.network.model.TopCityResponse
import me.spica.spicaweather3.network.model.weather.WeatherResponse
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.Query

@Keep
@Suppress("unused")
interface ApiService {


  @Headers("${HefengConfig.HEADER}:${HefengConfig.APIKEY}")
  @GET("/geo/v2/city/lookup")
  suspend fun lookupCity(@Query("location") query: String): ApiResponse<LookUpResponse>

  @Headers("${HefengConfig.HEADER}:${HefengConfig.APIKEY}")
  @GET("/geo/v2/city/top")
  suspend fun topCity(): ApiResponse<TopCityResponse>


  @GET("http://106.54.25.152:4040/api/weather/all")
  suspend fun getWeather(@Query("location") location: String): ApiResponse<WeatherResponse>

}
