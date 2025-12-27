package me.spica.spicaweather3.ui.widget.galaxy

import androidx.annotation.ColorInt
import java.util.*

class Star(
  var centerX: Float,
  var centerY: Float,
  radius: Float,
  @field:ColorInt @param:ColorInt var color: Int,
  var duration: Long,
) {
  var radius: Float
  var alpha = 0f
  var progress: Float = 0f


  init {
    this.radius = (radius * (0.7 + 0.3 * Random().nextFloat())).toFloat()
  }

  fun shine(interval: Long) {
    progress = (interval % duration) *1f / duration
    alpha = if (progress < 0.5) {
      progress * 2f
    } else {
      (1 - progress) * 2f
    }
  }

}