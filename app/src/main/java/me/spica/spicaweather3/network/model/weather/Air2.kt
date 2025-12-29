package me.spica.spicaweather3.network.model.weather

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Stable
data class Air2(
  val indexes: List<Indexe>,
  val metadata: Metadata,
  val pollutants: List<Pollutant>,
  val stations: List<Station>
) {
  @Immutable
  data class Indexe(
    val aqi: Int,
    val aqiDisplay: String,
    val category: String,
    val code: String,
    val color: Color,
    val health: Health,
    val level: String,
    val name: String,
    val primaryPollutant: PrimaryPollutant?
  ) {

    @Immutable
    data class PrimaryPollutant(
      val name: String,
      val fullName: String,
      val concentration: Concentration
    ){
      @Immutable
      data class Concentration(
        val unit: String,
        val value: Double
      )
    }

    @Immutable
    data class Color(
      val alpha: Int,
      val blue: Int,
      val green: Int,
      val red: Int
    )

    @Immutable
    data class Health(
      val advice: Advice,
      val effect: String
    ) {
      data class Advice(
        val generalPopulation: String,
        val sensitivePopulation: String
      )
    }
  }

  @Immutable
  data class Metadata(
    val tag: String
  )

  @Immutable
  data class Pollutant(
    val code: String,
    val concentration: Concentration,
    val fullName: String,
    val name: String,
    val subIndexes: List<SubIndexe>
  ) {
    @Immutable
    data class Concentration(
      val unit: String,
      val value: Double
    )

    @Immutable
    data class SubIndexe(
      val aqi: Int,
      val aqiDisplay: String,
      val code: String
    )
  }

  @Immutable
  data class Station(
    val id: String,
    val name: String
  )
}