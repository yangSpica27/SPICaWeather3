package me.spica.spicaweather3.ui.widget.rain

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent

/**
 * 雨滴动画组件（TextureView + OpenGL ES 3.0）
 *
 * 使用 GPU 渲染代替原有的 Canvas + JBox2D 方案，改进：
 *  - 每个雨滴绘制为渐变三角形（头部不透明 → 尾部透明），拖尾更自然
 *  - 前景 / 背景双层视差，近大远小增强立体感
 *  - 12° 斜向风角，模拟真实降雨斜度
 *  - 纯运动学模拟，无物理引擎开销，CPU 占用更低
 *
 * @param show 控制雨滴动画是否显示，默认为 true
 */
@Composable
fun RainView(show: Boolean = true) {
    ShowOnIdleContent(
        visible = show,
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context -> RainTextureView(context) },
            modifier = Modifier.fillMaxSize()
        )
    }
}
