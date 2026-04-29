package me.spica.spicaweather3.ui.widget.rain

import android.graphics.Bitmap
import androidx.compose.runtime.Immutable

@Immutable
data class RainTextCollision(
    val bitmap: Bitmap,
    val left: Float,
    val top: Float,
)
