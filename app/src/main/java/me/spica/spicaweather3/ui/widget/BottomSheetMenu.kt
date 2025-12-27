package me.spica.spicaweather3.ui.widget

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.flow.collectLatest
import top.yukonga.miuix.kmp.theme.MiuixTheme

val LocalMenuState = compositionLocalOf { MenuState() }

@Stable
class MenuState(
  isVisible: Boolean = false,
  content: @Composable ColumnScope.() -> Unit = {},
) {
  var isVisible by mutableStateOf(isVisible)

  var content by mutableStateOf(content)

  /**
   * Sheet 展开进度 (0f = 完全隐藏, 1f = 完全展开)
   * 可用于驱动背景模糊、遮罩透明度等效果
   */
  var expandProgress by mutableFloatStateOf(0f)
    private set

  internal fun updateProgress(progress: Float) {
    expandProgress = progress.coerceIn(0f, 1f)
  }

  @OptIn(ExperimentalMaterial3Api::class)
  fun show(content: @Composable ColumnScope.() -> Unit) {
    isVisible = true
    this.content = content
  }

  @OptIn(ExperimentalMaterial3Api::class)
  fun dismiss() {
    isVisible = false
  }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BottomSheetMenu(
  modifier: Modifier = Modifier,
  state: MenuState,
  background: Color = MiuixTheme.colorScheme.secondaryContainer,
) {
  val focusManager = LocalFocusManager.current
  val hapticFeedback = LocalHapticFeedback.current

  val sheetState = rememberModalBottomSheetState(
    skipPartiallyExpanded = false,
  )

  LaunchedEffect(state.isVisible) {
    if (state.isVisible) {
      hapticFeedback.performHapticFeedback(hapticFeedbackType = HapticFeedbackType.SegmentFrequentTick)
    } else {
      // 隐藏时重置进度
      state.updateProgress(0f)
    }
  }

  if (state.isVisible) {
    // 使用 BoxWithConstraints 获取容器高度
    BoxWithConstraints {
      val containerHeightPx = with(LocalDensity.current) { maxHeight.toPx() }

      // 实时监听 offset 变化计算进度
      LaunchedEffect(sheetState, containerHeightPx) {
        snapshotFlow {
          try {
            sheetState.requireOffset()
          } catch (e: IllegalStateException) {
            // offset 尚未初始化
            containerHeightPx
          }
        }.collectLatest { offset ->
          // offset = 0 表示完全展开, offset = containerHeight 表示完全隐藏
          val progress = if (containerHeightPx > 0f) {
            1f - (offset / containerHeightPx).coerceIn(0f, 1f)
          } else {
            0f
          }
          state.updateProgress(progress)
        }
      }

      ModalBottomSheet(
        onDismissRequest = {
          focusManager.clearFocus()
          state.isVisible = false
        },
        sheetState = sheetState,
        containerColor = background,
        contentColor = MaterialTheme.colorScheme.onSecondaryContainer,
        dragHandle = {
          Box(
            modifier =
              Modifier
                .padding(vertical = 12.dp)
                .size(width = 40.dp, height = 4.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(MiuixTheme.colorScheme.onSurface.copy(alpha = 0.4f)),
          )
        },
        modifier =
          Modifier
            .fillMaxHeight()
            .statusBarsPadding(),
      ) {
        Column(
          modifier =
            modifier
              .fillMaxWidth()
              .padding(horizontal = 20.dp),
        ) {
          state.content(this)
        }
      }
    }
  }
}
