package me.spica.spicaweather3.network.model.weather

import androidx.annotation.Keep
import com.google.gson.annotations.SerializedName

@Keep
data class WeatherWarning(
  @SerializedName("id") val id: String? = null,
  @SerializedName("title") val title: String? = null,
  @SerializedName("text") val text: String? = null,
  @SerializedName("type") val type: String? = null,
  @SerializedName("typeName") val typeName: String? = null,
  @SerializedName("level") val level: String? = null,
  @SerializedName("levelName") val levelName: String? = null,
  @SerializedName("severity") val severity: String? = null,
  @SerializedName("pubTime") val pubTime: String? = null,
  @SerializedName("status") val status: String? = null,
  @SerializedName("source") val source: String? = null
)
