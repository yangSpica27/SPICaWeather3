package me.spica.spicaweather3.ui.widget

import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.layer.drawLayer
import androidx.compose.ui.graphics.rememberGraphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch

/**
 * 为组件添加截图捕获和下拉菜单功能的 Modifier
 *
 * 这个 Modifier 会：
 * 1. 捕获组件的位置和尺寸
 * 2. 使用 GraphicsLayer 截取组件的图像
 * 3. 点击时显示全屏下拉菜单叠加层
 * 4. 在叠加层中显示组件截图和菜单项
 * 5. 背景模糊效果
 *
 * 使用示例：
 * ```
 * Box(
 *   modifier = Modifier
 *     .size(200.dp)
 *     .background(Color.Blue)
 *     .captureToDropdownMenu(
 *       items = listOf(
 *         DropdownMenuItem("编辑", Icons.Default.Edit) { /* 编辑逻辑 */ },
 *         DropdownMenuItem("分享", Icons.Default.Share) { /* 分享逻辑 */ },
 *         DropdownMenuItem("删除", Icons.Default.Delete) { /* 删除逻辑 */ }
 *       )
 *     )
 * ) {
 *   Text("点击显示菜单")
 * }
 * ```
 *
 * @param items 菜单项列表
 * @param onLongClick 是否使用长按触发（默认为 false，使用点击触发）
 * @param enabled 是否启用（默认为 true）
 */
@Composable
fun Modifier.captureToDropdownMenu(
  items: List<DropdownMenuItem>,
  onLongClick: Boolean = false,
  enabled: Boolean = true
): Modifier = composed {
  if (!enabled || items.isEmpty()) {
    return@composed this
  }

  val controller = LocalDropdownMenuController.current
  val coroutineScope = rememberCoroutineScope()
  val graphicsLayer = rememberGraphicsLayer()
  
  var targetOffset by remember { mutableStateOf(Offset.Zero) }
  var targetSize by remember { mutableStateOf(IntSize.Zero) }

  this
    // 使用 GraphicsLayer 以支持截图
    .drawWithContent {
      graphicsLayer.record {
        this@drawWithContent.drawContent()
      }
      drawLayer(graphicsLayer)
    }
    // 记录组件位置和尺寸
    .onGloballyPositioned { coordinates ->
      targetOffset = coordinates.positionInWindow()
      targetSize = coordinates.size
    }
    // 处理点击事件
    .pointerInput(items, onLongClick) {
      detectTapGestures(
        onTap = if (!onLongClick) {
          {
            coroutineScope.launch {
              // 捕获当前 GraphicsLayer 的图像
              val bitmap = graphicsLayer.toImageBitmap()
              
              // 显示下拉菜单
              controller.show(
                bitmap = bitmap,
                offset = targetOffset,
                size = targetSize,
                items = items
              )
            }
          }
        } else null,
        onLongPress = if (onLongClick) {
          {
            coroutineScope.launch {
              // 捕获当前 GraphicsLayer 的图像
              val bitmap = graphicsLayer.toImageBitmap()
              
              // 显示下拉菜单
              controller.show(
                bitmap = bitmap,
                offset = targetOffset,
                size = targetSize,
                items = items
              )
            }
          }
        } else null
      )
    }
}

/**
 * 为组件添加截图捕获和下拉菜单功能的 Modifier（使用 lambda 提供菜单项）
 *
 * 与 captureToDropdownMenu 类似，但菜单项通过 lambda 动态提供，
 * 这在需要根据当前状态动态生成菜单项时很有用。
 *
 * 使用示例：
 * ```
 * var isStarred by remember { mutableStateOf(false) }
 * 
 * Box(
 *   modifier = Modifier
 *     .size(200.dp)
 *     .captureToDropdownMenuDynamic(
 *       itemsProvider = {
 *         listOf(
 *           DropdownMenuItem(
 *             text = if (isStarred) "取消收藏" else "收藏",
 *             icon = if (isStarred) Icons.Default.Star else Icons.Default.StarBorder,
 *             onClick = { isStarred = !isStarred }
 *           ),
 *           DropdownMenuItem("删除", Icons.Default.Delete) { /* 删除 */ }
 *         )
 *       }
 *     )
 * )
 * ```
 *
 * @param itemsProvider 动态提供菜单项的 lambda 函数
 * @param onLongClick 是否使用长按触发（默认为 false）
 * @param enabled 是否启用（默认为 true）
 */
@Composable
fun Modifier.captureToDropdownMenuDynamic(
  itemsProvider: () -> List<DropdownMenuItem>,
  onLongClick: Boolean = false,
  enabled: Boolean = true
): Modifier = composed {
  if (!enabled) {
    return@composed this
  }

  val controller = LocalDropdownMenuController.current
  val coroutineScope = rememberCoroutineScope()
  val graphicsLayer = rememberGraphicsLayer()
  
  var targetOffset by remember { mutableStateOf(Offset.Zero) }
  var targetSize by remember { mutableStateOf(IntSize.Zero) }

  this
    .drawWithContent {
      graphicsLayer.record {
        this@drawWithContent.drawContent()
      }
      drawLayer(graphicsLayer)
    }
    .onGloballyPositioned { coordinates ->
      targetOffset = coordinates.positionInWindow()
      targetSize = coordinates.size
    }
    .pointerInput(onLongClick) {
      detectTapGestures(
        onTap = if (!onLongClick) {
          {
            val items = itemsProvider()
            if (items.isNotEmpty()) {
              coroutineScope.launch {
                val bitmap = graphicsLayer.toImageBitmap()
                controller.show(
                  bitmap = bitmap,
                  offset = targetOffset,
                  size = targetSize,
                  items = items
                )
              }
            }
          }
        } else null,
        onLongPress = if (onLongClick) {
          {
            val items = itemsProvider()
            if (items.isNotEmpty()) {
              coroutineScope.launch {
                val bitmap = graphicsLayer.toImageBitmap()
                controller.show(
                  bitmap = bitmap,
                  offset = targetOffset,
                  size = targetSize,
                  items = items
                )
              }
            }
          }
        } else null
      )
    }
}
