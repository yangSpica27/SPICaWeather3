package me.spica.spicaweather3.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.kyant.capsule.ContinuousRoundedRectangle
import top.yukonga.miuix.kmp.theme.MiuixTheme


// 主页 新增按钮尺寸
val MAIN_PLUS_BUTTON_SIZE = 64.dp

// 卡片组件的Padding
val WIDGET_CARD_PADDING = 12.dp

// 卡片组件的圆角
val WIDGET_CARD_CORNER_SHAPE = ContinuousRoundedRectangle(12.dp)

// 卡片组件的标题

@Composable
fun WIDGET_CARD_TITLE_TEXT_STYLE(): TextStyle {
  return TextStyle(fontWeight = FontWeight.W700, color = MiuixTheme.colorScheme.onSurface)
}