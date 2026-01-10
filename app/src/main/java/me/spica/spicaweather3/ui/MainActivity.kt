package me.spica.spicaweather3.ui

import android.content.res.Configuration
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import me.jessyan.autosize.internal.CustomAdapt

class MainActivity : ComponentActivity(), CustomAdapt {

  /**
   * 是否为横屏模式
   */
  private val isLandscape: Boolean
    get() = resources.configuration.orientation == Configuration.ORIENTATION_LANDSCAPE

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent {
      AppMain()
    }
  }

  override fun isBaseOnWidth(): Boolean = true

  /**
   * 设计稿基准尺寸（dp）
   * 竖屏：375dp（手机设计稿）
   * 横屏：1024dp（平板/横屏设计稿）
   */
  override fun getSizeInDp(): Float = if (isLandscape) 1024f else 375f
}