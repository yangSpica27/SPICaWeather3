package me.spica.spicaweather3.ui.widget

import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.kyant.backdrop.BackdropEffectScope
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.colorControls
import com.kyant.backdrop.effects.vibrancy

/**
 * 预定义的毛玻璃效果配置
 * 用于统一应用内的视觉风格
 */
object BackdropEffects {
  
  /**
   * 标准玻璃效果
   * 适用于顶栏、按钮等需要毛玻璃效果的组件
   * 
   * @param blurRadius 模糊半径，默认 8dp
   * @param saturation 饱和度，默认 1.6f
   * @param brightness 亮度，默认 0.3f
   */
  fun BackdropEffectScope.standardGlassEffect(
    blurRadius: Dp = 8.dp,
    saturation: Float = 1.6f,
    brightness: Float = 0.3f
  ) {
    vibrancy()
    blur(blurRadius.toPx())
    colorControls(
      saturation = saturation,
      brightness = brightness
    )
  }
}
