package me.spica.spicaweather3.ui.widget.haze

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent

/**
 * 烟雾动画组件（TextureView + OpenGL ES 2.0）
 * 使用分层 Simplex 噪声着色器模拟自然流动的雾气效果
 *
 * @param show 控制烟雾动画是否显示
 * @param fogColor 雾气颜色，默认白色；可传入天气主题色进行轻微色调融合
 */
@Composable
fun HazeView(
  show: Boolean = true,
  fogColor: Color = Color.White,
) {
  ShowOnIdleContent(
    visible = show,
    modifier = Modifier.fillMaxSize(),
  ) {
    val lifecycleOwner = LocalLifecycleOwner.current
    val renderer = remember { HazeRenderer() }
    var hazeTextureView by remember { mutableStateOf<HazeTextureView?>(null) }

    SideEffect {
      renderer.updateFogColor(fogColor.red, fogColor.green, fogColor.blue)
    }

    DisposableEffect(lifecycleOwner) {
      val observer = object : DefaultLifecycleObserver {
        override fun onResume(owner: LifecycleOwner) {
          hazeTextureView?.onResume()
        }

        override fun onPause(owner: LifecycleOwner) {
          hazeTextureView?.onPause()
        }
      }
      lifecycleOwner.lifecycle.addObserver(observer)
      hazeTextureView?.onResume()
      onDispose {
        lifecycleOwner.lifecycle.removeObserver(observer)
        hazeTextureView?.onPause()
      }
    }

    AndroidView(
      modifier = Modifier.fillMaxSize(),
      factory = { context ->
        HazeTextureView(context, renderer).also { hazeTextureView = it }
      },
    )
  }
}