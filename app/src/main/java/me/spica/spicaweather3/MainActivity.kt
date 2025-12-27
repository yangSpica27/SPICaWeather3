package me.spica.spicaweather3

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.jessyan.autosize.internal.CustomAdapt
import me.spica.spicaweather3.ui.AppMain

class MainActivity : ComponentActivity(), CustomAdapt {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppMain()
    }
  }

  override fun isBaseOnWidth(): Boolean = true

  override fun getSizeInDp(): Float = 375f
}
