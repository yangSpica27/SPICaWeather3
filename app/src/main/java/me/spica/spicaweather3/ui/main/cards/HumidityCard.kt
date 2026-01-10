package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.annotation.StringRes
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastRoundToInt
import me.spica.spicaweather3.R
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 湿度信息卡片
 * 显示当前空气湿度百分比及健康提示
 * 包含数字递增动画、图标缩放动画和文字渐显效果
 *
 * @param modifier 修饰符
 * @param humidity 湿度值（0-100）
 * @param startAnim 是否开始播放动画
 */
@Composable
fun HumidityCard(modifier: Modifier = Modifier, humidity: Int = 55, startAnim: Boolean) {

  // ==================== 动画配置 ====================
  // 湿度数字递增动画（1050ms）
  val progressAnimValue = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 1050, 0)
  ).value

  // 标题和图标动画（持续140ms）
  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 140, 0)
  ).value

  // 湿度数字缩放渐显动画（持续230ms）
  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 230, 50)
  ).value

  // 百分号平移渐显动画（持续320ms）
  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 320, 70)
  ).value

  // 根据湿度值计算健康提示
  // 湿度区间：<45%=干燥, 45-65%=舒适, >65%=潮湿
  val humidityTipRes = remember(humidity) { humidity.toHumidityTipRes() }
  val humidityTip = stringResource(humidityTipRes)

  // ==================== 主布局 ====================
  Column(
    modifier = modifier.padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.Start
  ) {
    // 标题行（图标 + 文字，带缩放和平移动画）
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 湿度图标（缩放渐显动画）
      Icon(
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue1
          scaleX = textAnimValue1
          scaleY = textAnimValue1
        },
        painter = painterResource(id = R.drawable.material_symbols_outlined_humidity_helper),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface
      )

      // 标题文字（渐显和向上平移动画）
      Text(
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue1
          translationY = -12.dp.toPx() * (1f - textAnimValue1)
        },
        text = stringResource(R.string.humidity_info_title), color = MiuixTheme.colorScheme.onSurface, style = WIDGET_CARD_TITLE_TEXT_STYLE()
      )
    }

    // 湿度数值显示区域（占据剩余空间，底部对齐）
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f), contentAlignment = Alignment.BottomStart
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        // 湿度数字（从0递增到目标值，带缩放渐显动画）
        Text(
          modifier = Modifier
            .alignByBaseline()
            .graphicsLayer {
              scaleX = .8f + .2f * textAnimValue2
              scaleY = .8f + .2f * textAnimValue2
              alpha = textAnimValue2
            }, text = "${(humidity * progressAnimValue).fastRoundToInt()}",
          color = MiuixTheme.colorScheme.onSurfaceContainer,
          style = MiuixTheme.textStyles.headline1.copy(
            fontWeight = FontWeight.W900, fontSize = 56.sp
          )
        )
        // 百分号（从右向左平移渐显动画）
        Text(
          modifier = Modifier
            .alignByBaseline()
            .graphicsLayer {
              translationX = 12.dp.toPx() * (1f - textAnimValue3)
              alpha = textAnimValue3
            },
          text = "%",
          color = MiuixTheme.colorScheme.onSurfaceContainer,
          style = MiuixTheme.textStyles.headline2.copy(
            fontWeight = FontWeight.W900, fontSize = 22.sp
          )
        )
      }
    }
    // 底部健康提示区域
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row {
        // 健康提示文字（根据湿度值动态显示，带向上平移渐显动画）
        Text(
          modifier = Modifier
            .alignByBaseline()
            .graphicsLayer {
              translationY = -12.dp.toPx() * (1f - textAnimValue1)
              alpha = textAnimValue1
            }, text = humidityTip, color = MiuixTheme.colorScheme.onSurface,
          style = MiuixTheme.textStyles.body1.copy(
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = .9f)
          )
        )
      }
    }
  }
}

@StringRes
private fun Int.toHumidityTipRes(): Int = when {
  this < 45 -> R.string.humidity_tip_dry
  this < 65 -> R.string.humidity_tip_comfortable
  else -> R.string.humidity_tip_humid
}

