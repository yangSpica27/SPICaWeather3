package me.spica.spicaweather3.data.remote.api

import androidx.annotation.Keep
import com.skydoves.sandwich.ApiResponse
import me.spica.spicaweather3.common.config.HefengConfig
import me.spica.spicaweather3.data.remote.api.model.BatchWeatherRequest
import me.spica.spicaweather3.data.remote.api.model.LookUpResponse
import me.spica.spicaweather3.data.remote.api.model.TopCityResponse
import me.spica.spicaweather3.data.remote.api.model.weather.BaseResponse
import me.spica.spicaweather3.data.remote.api.model.weather.BatchWeatherList
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Headers
import retrofit2.http.POST
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


  @POST("http://106.54.25.152:4141/api/weather/batch")
  suspend fun getWeather(@Body req: BatchWeatherRequest): ApiResponse<BaseResponse<BatchWeatherList>>

}
