package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.LinearGradientShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.toArgb
import androidx.annotation.StringRes
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 体感温度卡片
 * 显示当前体感温度及其描述，带有精致的动画效果和温度可视化
 * 包含温度数字动画、文字淡入动画和右侧温度条渐变显示
 *
 * @param feelTemp 体感温度值（摄氏度）
 * @param startAnim 是否开始播放动画
 */
@Composable
fun FeelTempCard(feelTemp: Int, startAnim: Boolean) {

  // 根据体感温度计算描述文字
  // 温度区间：>30℃=炎热, 25-30℃=热, 20-25℃=温暖, 15-20℃=舒适,
  //           10-15℃=凉爽, 5-10℃=寒冷, ≤5℃=极寒
  val feelTempDescRes = remember(feelTemp) { feelTemp.toFeelTempDescRes() }
  val feelTempDesc = stringResource(id = feelTempDescRes)

  // ==================== 动画配置 ====================
  // 温度条进度动画（1050ms，用于温度条高度渐变）
  val progressAnimValue = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 1050, 0)
  ).value

  // 标题文字淡入动画（延迟130ms，持续140ms）
  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 140, 130)
  ).value

  // 温度单位淡入动画（延迟150ms，持续230ms）
  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 230, 150)
  ).value

  // 温度描述淡入动画（延迟170ms，持续320ms）
  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 320, 170)
  ).value

  // 温度数字递增动画（延迟170ms，持续1720ms）
  val textAnimValue4 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 1720, 170)
  ).value

  // ==================== 主布局 ====================
  Row(
    modifier = Modifier
      .fillMaxWidth()
      .padding(WIDGET_CARD_PADDING)
  ) {
    // 左侧：温度信息区域
    Column(
      verticalArrangement = Arrangement.spacedBy(8.dp),
      modifier = Modifier.weight(1f)
    ) {
      // 标题行（图标 + 文字）
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Icon(
          painter = painterResource(id = R.drawable.material_symbols_outlined_accessibility_new),
          contentDescription = null,
          tint = MiuixTheme.colorScheme.onSurface
        )
        Text(
          text = stringResource(R.string.feel_temp_title),
          color = MiuixTheme.colorScheme.onSurface, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
          modifier = Modifier.graphicsLayer {
            alpha = textAnimValue1
            translationY = -12.dp.toPx() * (1f - textAnimValue1)
          }
        )
      }
      // 温度数值行（大号数字 + 单位）
      Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
      ) {
        // 温度数字（带递增动画和渐变色）
        Text(
          "${(feelTemp * textAnimValue4).toInt()}",
          color = MiuixTheme.colorScheme.onSurface,
          style = MiuixTheme.textStyles.title1.copy(
            brush = Brush.linearGradient(
              colors = listOf(
                MiuixTheme.colorScheme.onSurface,
                MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
                MiuixTheme.colorScheme.onSurface.copy(alpha = .5f),
              )
            )
          ),
          fontWeight = FontWeight.W900,
          modifier = Modifier.alignByBaseline(),
          fontSize = 48.sp,
        )
        // 温度单位（带淡入和位移动画）
        Text(
          "℃", color = MiuixTheme.colorScheme.onSecondaryContainer, style = MiuixTheme.textStyles.title1.copy(
            brush = Brush.linearGradient(
              colors = listOf(
                MiuixTheme.colorScheme.onSurface,
                MiuixTheme.colorScheme.onSurface.copy(alpha = .8f),
                MiuixTheme.colorScheme.onSurface.copy(alpha = .5f),
              )
            )
          ),
          fontWeight = FontWeight.W900, modifier = Modifier
            .alignByBaseline()
            .graphicsLayer {
              alpha = textAnimValue2
              translationY = -12.dp.toPx() * (1f - textAnimValue2)
            },
          fontSize = 22.sp
        )
      }
      // 占位符，推动描述文字到底部
      Spacer(
        modifier = Modifier.weight(1f)
      )
      // 温度描述文字（炎热/热/温暖等，带淡入动画）
      Text(
        feelTempDesc, color = MiuixTheme.colorScheme.onSecondaryContainer, style = MiuixTheme.textStyles.body1,
        fontWeight = FontWeight.W600,
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue3
          translationY = -12.dp.toPx() * (1f - textAnimValue3)
        }
      )
    }

    // ==================== 右侧温度可视化条 ====================
    val borderColor = MiuixTheme.colorScheme.secondaryContainer.toArgb()

    // 缓存 Paint 对象，避免在 drawWithCache 内重复创建
    val linePaint = remember {
      Paint().asFrameworkPaint().apply {
        strokeWidth = 8.dp.value
        strokeCap = android.graphics.Paint.Cap.ROUND
      }
    }
    val pointPaint = remember {
      Paint().asFrameworkPaint().apply {
        strokeWidth = 8.dp.value
        strokeCap = android.graphics.Paint.Cap.ROUND
      }
    }

    Box(
      modifier = Modifier
        .width(24.dp)
        .fillMaxHeight()
        .padding(vertical = 12.dp)
        .drawWithCache {

          // 温度条颜色定义：橙色(高温) -> 绿色(中温) -> 蓝色(低温)
          val highTempColor = Color(0xffffc069) // 高温-橙色
          val midTempColor = Color(0xff22c55e)  // 中温-绿色
          val lowTempColor = Color(0xff91caff)  // 低温-蓝色

          // 更新 Paint 属性
          linePaint.strokeWidth = 8.dp.toPx()
          pointPaint.strokeWidth = 8.dp.toPx()

          // 为温度条应用垂直渐变着色器
          linePaint.shader = LinearGradientShader(
            from = Offset(0f, 20.dp.toPx()), to = Offset(0f, size.height), colors = listOf(
              highTempColor,
              highTempColor,
              midTempColor,
              lowTempColor,
              lowTempColor,
            )
          )

          // 计算温度指示点位置
          // 温度范围：-20℃ 到 40℃，从下到上线性映射
          val offset = Offset(
            size.width / 2f,
            (size.height - 10.dp.toPx()) - (size.height - 20.dp.toPx()) * (((feelTemp - -20f) / (40 - -20)).coerceIn(
              0f,
              1f
            )) * progressAnimValue
          )

          onDrawWithContent {
            drawIntoCanvas { canvas ->
              // 绘制背景渐变温度条（垂直线）
              canvas.nativeCanvas.drawLine(
                size.width / 2f,
                20.dp.toPx(),
                size.width / 2f,
                size.height - linePaint.strokeWidth,
                linePaint
              )
              // 绘制温度指示点外圈（边框色）
              pointPaint.color = borderColor
              canvas.nativeCanvas.drawCircle(
                offset.x, offset.y, 10.dp.toPx() + 2.dp.toPx(), pointPaint
              )
              // 绘制温度指示点内圈（渐变色）
              canvas.nativeCanvas.drawCircle(
                offset.x, offset.y, 10.dp.toPx(), linePaint
              )
            }
          }
        }
    )

  }

}

@StringRes
private fun Int.toFeelTempDescRes(): Int = when {
  this > 30 -> R.string.feel_temp_extremely_hot
  this > 25 -> R.string.feel_temp_hot
  this > 20 -> R.string.feel_temp_warm
  this > 15 -> R.string.feel_temp_comfortable
  this > 10 -> R.string.feel_temp_cool
  this > 5 -> R.string.feel_temp_cold
  else -> R.string.feel_temp_freezing
}