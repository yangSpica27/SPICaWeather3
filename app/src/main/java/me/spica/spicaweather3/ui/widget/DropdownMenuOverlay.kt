package me.spica.spicaweather3.ui.widget

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import kotlinx.coroutines.launch
import top.yukonga.miuix.kmp.basic.Card
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.roundToInt

/**
 * 下拉菜单项数据类
 *
 * @param text 菜单项文本
 * @param icon 菜单项图标（可选）
 * @param enabled 是否启用，默认为 true
 * @param onClick 点击事件回调
 */
data class DropdownMenuItem(
  val text: String,
  val icon: ImageVector? = null,
  val enabled: Boolean = true,
  val onClick: () -> Unit
)

/**
 * 下拉菜单状态数据类
 * @param targetBitmap 目标组件的截图
 * @param targetOffset 目标组件在窗口中的偏移位置
 * @param targetSize 目标组件的尺寸
 * @param menuItems 菜单项列表
 */
@Immutable
data class DropdownMenuState(
  val targetBitmap: ImageBitmap? = null,
  val targetOffset: Offset = Offset.Zero,
  val targetSize: IntSize = IntSize.Zero,
  val menuItems: List<DropdownMenuItem> = emptyList()
)

/**
 * 下拉菜单控制器
 * 用于管理全局的下拉菜单状态和显示/隐藏逻辑
 */
class DropdownMenuController {
  var state by mutableStateOf(DropdownMenuState())
    private set

  var isVisible by mutableStateOf(false)
    private set

  /**
   * 显示下拉菜单
   *
   * @param bitmap 目标组件的截图
   * @param offset 目标组件在窗口中的位置
   * @param size 目标组件的尺寸
   * @param items 菜单项列表
   */
  fun show(
    bitmap: ImageBitmap,
    offset: Offset,
    size: IntSize,
    items: List<DropdownMenuItem>
  ) {
    state = DropdownMenuState(
      targetBitmap = bitmap,
      targetOffset = offset,
      targetSize = size,
      menuItems = items
    )
    isVisible = true
  }

  /**
   * 隐藏下拉菜单
   */
  fun dismiss() {
    isVisible = false
  }
}

/**
 * 全局下拉菜单控制器的 CompositionLocal
 */
val LocalDropdownMenuController = compositionLocalOf {
  DropdownMenuController()
}



/**
 * 下拉菜单全屏叠加层
 * 显示模糊背景、目标组件截图和菜单
 */
@Composable
fun DropdownMenuOverlay() {
  val controller = LocalDropdownMenuController.current
  val state = controller.state
  val density = LocalDensity.current
  val isVisible = controller.isVisible

  BackHandler(isVisible) {
    controller.dismiss()
  }

  AnimatedVisibility(
    visible = isVisible,
    enter = fadeIn(),
    exit = fadeOut(animationSpec = tween(durationMillis = 50, delayMillis = 500))
  ) {
    Box(
      modifier = Modifier
        .fillMaxSize()
        .zIndex(1000f)
    ) {
      // 背景层
      Box(
        modifier = Modifier
          .fillMaxSize()
          .background(MiuixTheme.colorScheme.onSurface.copy(alpha = .08f))
          .pointerInput(Unit) {
            detectTapGestures {
              controller.dismiss()
            }
          }
      )

      // 目标组件截图（保持清晰）
      state.targetBitmap?.let { bitmap ->
        Image(
          bitmap = bitmap,
          contentDescription = "Target Component",
          modifier = Modifier
            .offset {
              IntOffset(
                state.targetOffset.x.roundToInt(),
                state.targetOffset.y.roundToInt()
              )
            }
            .size(
              width = with(density) { state.targetSize.width.toDp() },
              height = with(density) { state.targetSize.height.toDp() }
            )
            .zIndex(1001f)
        )
      }

      // 下拉菜单
      if (state.menuItems.isNotEmpty()) {
        // 计算菜单显示位置（在目标组件下方或上方）
        val menuOffset = calculateMenuOffset(
          targetOffset = state.targetOffset,
          targetSize = state.targetSize,
          density = density
        )

        Box(
          modifier = Modifier
            .offset { menuOffset }
            .zIndex(1002f)
        ) {
          ShowOnIdleContent (
            visible = isVisible,
            enter = materialSharedAxisYIn(true),
            exit = materialSharedAxisYOut(true)
          ) {
            DropdownMenuContent(
              items = state.menuItems,
              onItemClick = { item ->
                item.onClick()
                controller.dismiss()
              },
              onDismiss = { controller.dismiss() }
            )
          }
        }
      }
    }
  }
}

/**
 * 计算菜单显示位置
 * 优先在目标组件下方显示，如果空间不够则在上方显示
 */
private fun calculateMenuOffset(
  targetOffset: Offset,
  targetSize: IntSize,
  density: androidx.compose.ui.unit.Density
): IntOffset {
  with(density) {
    // 菜单显示在目标组件下方，稍微偏移一点
    val x = targetOffset.x.roundToInt()
    val y = (targetOffset.y + targetSize.height + 8.dp.toPx()).roundToInt()

    return IntOffset(x, y)
  }
}

/**
 * 下拉菜单内容组件
 */
@Composable
private fun DropdownMenuContent(
  items: List<DropdownMenuItem>,
  onItemClick: (DropdownMenuItem) -> Unit,
  onDismiss: () -> Unit
) {
  Card(
    modifier = Modifier
      .fillMaxWidth(0.5f)
      .padding(horizontal = 16.dp),
  ) {
    Column(
      modifier = Modifier.padding(vertical = 8.dp)
    ) {
      items.forEach { item ->
        DropdownMenuItemContent(
          item = item,
          onClick = { onItemClick(item) }
        )
      }
    }
  }
}

/**
 * 单个菜单项内容
 */
@Composable
private fun DropdownMenuItemContent(
  item: DropdownMenuItem,
  onClick: () -> Unit
) {
  val interactionSource = remember { MutableInteractionSource() }

  Row(
    modifier = Modifier
      .fillMaxWidth()
      .clickable(
        enabled = item.enabled,
        interactionSource = interactionSource,
        onClick = onClick
      )
      .padding(horizontal = 12.dp, vertical = 8.dp),
    verticalAlignment = Alignment.CenterVertically
  ) {
    // 图标
    item.icon?.let { icon ->
      Icon(
        imageVector = icon,
        contentDescription = null,
        modifier = Modifier.size(24.dp),
        tint = if (item.enabled) {
          MiuixTheme.colorScheme.onSurface
        } else {
          MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
        }
      )
      Spacer(modifier = Modifier.width(12.dp))
    }

    // 文本
    Text(
      text = item.text,
      style = MiuixTheme.textStyles.body1,
      color = if (item.enabled) {
        MiuixTheme.colorScheme.onSurface
      } else {
        MiuixTheme.colorScheme.onSurface.copy(alpha = 0.38f)
      }
    )
  }
}

/**
 * 用于组件捕获和显示下拉菜单的扩展 Modifier
 *
 * 使用示例：
 * ```
 * Box(
 *   modifier = Modifier
 *     .size(100.dp)
 *     .background(Color.Blue)
 *     .captureAndShowDropdown(
 *       items = {
 *         listOf(
 *           DropdownMenuItem("编辑", Icons.Default.Edit) { /* 处理编辑 */ },
 *           DropdownMenuItem("删除", Icons.Default.Delete) { /* 处理删除 */ }
 *         )
 *       }
 *     )
 * )
 * ```
 *
 * @param items 菜单项列表的提供函数
 * @param onLongClick 是否使用长按触发（默认为 false，使用点击触发）
 */
@Composable
fun Modifier.captureAndShowDropdown(
  items: () -> List<DropdownMenuItem>,
  onLongClick: Boolean = false
): Modifier {
  val controller = LocalDropdownMenuController.current
  val coroutineScope = rememberCoroutineScope()
  var targetOffset by remember { mutableStateOf(Offset.Zero) }
  var targetSize by remember { mutableStateOf(IntSize.Zero) }

  return this
    .onGloballyPositioned { coordinates ->
      // 记录组件在窗口中的位置和尺寸
      targetOffset = coordinates.positionInWindow()
      targetSize = coordinates.size
    }
    .pointerInput(Unit) {
      if (onLongClick) {
        // 长按触发
        detectTapGestures(
          onLongPress = {
            coroutineScope.launch {
              // 捕获当前组件的截图
              // 注意：实际截图需要使用 graphicsLayer 的 captureToImage
              // 这里简化处理，实际使用时需要配合 graphicsLayer
            }
          }
        )
      } else {
        // 点击触发
        detectTapGestures {
          coroutineScope.launch {
            // 捕获当前组件的截图
            // 注意：实际截图需要使用 graphicsLayer 的 captureToImage
            // 这里简化处理，实际使用时需要配合 graphicsLayer
          }
        }
      }
    }
}
