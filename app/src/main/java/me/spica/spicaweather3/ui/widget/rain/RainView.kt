package me.spica.spicaweather3.ui.widget.rain

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.viewinterop.AndroidView
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent

/**
 * 雨滴动画组件（TextureView + OpenGL ES 3.0）
 *
 * @param show 控制雨滴动画是否显示
 * @param collisionRect 碰撞矩形（本地像素坐标），雨滴会与此矩形边缘碰撞溅落
 * @param collisionCornerRadiusPx 碰撞矩形的圆角半径（像素）
 * @param textCollisions 文本碰撞数据列表，各文本轮廓均参与物理碰撞
 */
@Composable
fun RainView(
    show: Boolean = true,
    collisionRect: Rect? = null,
    collisionCornerRadiusPx: Float = 0f,
    textCollisions: List<RainTextCollision> = emptyList(),
) {
    ShowOnIdleContent(
        visible = show,
        modifier = Modifier.fillMaxSize()
    ) {
        AndroidView(
            factory = { context -> RainTextureView(context) },
            update = { view ->
                view.setTextCollisions(textCollisions)
                if (collisionRect != null) {
                    view.setCollisionRect(
                        collisionRect.left,
                        collisionRect.top,
                        collisionRect.right,
                        collisionRect.bottom,
                        collisionCornerRadiusPx
                    )
                } else {
                    view.clearCollisionRect()
                }
            },
            modifier = Modifier.fillMaxSize()
        )
    }
}
