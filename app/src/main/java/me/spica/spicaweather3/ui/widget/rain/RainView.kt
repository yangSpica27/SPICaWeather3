package me.spica.spicaweather3.ui.widget.rain

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import kotlinx.coroutines.android.awaitFrame
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import java.util.concurrent.Executors


/**
 * 雨滴动画组件
 *
 * @param show 控制雨滴动画是否显示，默认为 true
 */
@Composable
fun RainView(show: Boolean = true) {
  // 仅在应用空闲时显示内容，用于优化渲染性能
  ShowOnIdleContent(
    visible = show,
    modifier = Modifier.fillMaxSize()
  ) {
    // 获取父容器的尺寸约束
    BoxWithConstraints(
      modifier = Modifier.fillMaxSize()
    ) {
      // 创建并缓存雨滴工具类实例，使用当前容器的最大宽高初始化
      val rainBoxUtils = remember {
        RainBoxUtils(
          width = constraints.maxWidth,
          height = constraints.maxHeight
        )
      }

      // 帧计数器，每帧递增以触发画布重绘
      var frameTick by remember { mutableIntStateOf(0) }

      // 创建专用的计算线程池，并在组件销毁时自动关闭
      val computeContext = remember { 
        Executors.newFixedThreadPool(1).asCoroutineDispatcher() 
      }
      
      // 确保在组件销毁时关闭线程池，避免内存泄漏
      DisposableEffect(Unit) {
        onDispose {
          computeContext.close()
        }
      }

      // 启动动画协程，在组件进入组合时开始，离开时自动取消
      LaunchedEffect(Unit) {
        // 初始化雨滴粒子系统
        rainBoxUtils.initRainBox()
        // 动画循环：在后台线程中持续更新粒子状态
        launch(computeContext) {
          while (isActive) {
            // 更新雨滴位置和状态
            rainBoxUtils.next()
            // 等待下一帧，确保动画流畅
            awaitFrame()
            // 递增帧计数，触发重组
            frameTick++
          }
        }
      }

      // 主容器，包含雨滴动画和视觉效果
      Box(
        modifier = Modifier
          .fillMaxSize()
          // 在内容绘制前绘制雨滴
          .drawBehind {
            // 读取 frameTick 确保每帧都执行绘制操作
            frameTick
            // 获取原生 Canvas 进行绘制
            drawIntoCanvas { canvas ->
              // 绘制所有雨滴粒子到画布上
              rainBoxUtils.drawPoints(canvas.nativeCanvas)
            }
          }
      )
    }
  }
}
