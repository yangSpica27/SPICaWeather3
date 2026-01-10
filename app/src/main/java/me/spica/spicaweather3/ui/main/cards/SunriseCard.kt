package me.spica.spicaweather3.ui.main.cards

import android.util.Log
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.graphics.RectangleShape
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.remote.api.model.weather.AggregatedWeatherData
import me.spica.spicaweather3.presentation.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import java.util.*

/**
 * 日出日落信息卡片
 * 可视化展示当天的日出日落时间，并通过弧线路径动画展示当前时间的太阳位置
 * 特点：
 * - 贝塞尔曲线绘制太阳轨迹
 * - 实时计算太阳当前位置
 * - 每分钟自动更新位置
 * - 流畅的路径动画和发光效果
 * - 智能判断日出前/日落后状态
 *
 * @param weatherEntity 天气数据，包含日出日落时间
 * @param startAnim 是否开始播放动画
 */
@Composable
fun SunriseCard(weatherEntity: AggregatedWeatherData, startAnim: Boolean) {

  // ==================== 动画配置 ====================
  // 图标缩放动画（230ms）
  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 230)
  ).value

  // 标题文字动画（延迟170ms，持续320ms）
  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 320, 170)
  ).value

  // 备用动画值（延迟370ms，持续520ms）
  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 520, 370)
  ).value

  // ==================== 数据验证 ====================
  // 如果没有天气数据，显示空状态
  if (weatherEntity.forecast.next7Days.isEmpty()) {

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .height(140.dp)
    ) {
      Text(
        stringResource(R.string.sunrise_no_data),
        modifier = Modifier.align(Alignment.Center),
        style = MiuixTheme.textStyles.title2,
        color = MiuixTheme.colorScheme.onSurface.copy(alpha = .9f)
      )
    }

    return
  }

  // ==================== 时间计算 ====================
  // 解析日出时间（HH:mm 格式）
  val sunrise = remember(weatherEntity.forecast.today.sunrise) {
    Date().apply {
      hours = weatherEntity.forecast.today.sunrise.split(":")[0].toInt()
      minutes = weatherEntity.forecast.today.sunrise.split(":")[1].toInt()
    }
  }

  // 解析日落时间（HH:mm 格式）
  val sunset = remember(weatherEntity.forecast.today.sunset) {
    Date().apply {
      hours = weatherEntity.forecast.today.sunset.split(":")[0].toInt()
      minutes = weatherEntity.forecast.today.sunset.split(":")[1].toInt()
    }
  }

  // 太阳当前位置进度（0.0=日出, 1.0=日落）
  val progress = remember { Animatable(0f) }

  // 实时更新太阳位置
  LaunchedEffect(Unit) {
    while (isActive) {
      // 每分钟同步一次太阳位置
      val currentDate = Date()
      
      // 判断当前时间在日出和日落之间
      if (currentDate.after(sunrise) && currentDate.before(sunset)) {
        // 计算进度：(当前时间 - 日出时间) / (日落时间 - 日出时间)
        progress.animateTo(
          ((currentDate.time - sunrise.time) * 1f / (sunset.time - sunrise.time)).coerceIn(0f, 1f),
          animationSpec = tween(durationMillis = 850, delayMillis = 450)
        )
      } else
        if (currentDate.before(sunrise)) {
          // 日出前，太阳位置为 0
          Log.e("SunriseCard", "还没日出")
          progress.animateTo(0f)
        } else
          if (currentDate.after(sunset)) {
            // 日落后，太阳位置为 1
            Log.e("SunriseCard", "日落了")
            progress.animateTo(1f)
          }
      // 等待1分钟后再次更新
      delay(1000 * 60)
    }
  }


  // 路径背景颜色
  val pathBgColor = MiuixTheme.colorScheme.secondaryContainer

  // ==================== 主布局 ====================
  Column(
    modifier = Modifier
      .fillMaxSize()
      .aspectRatio(1f)
      .padding(
        horizontal = 16.dp,
        vertical = 12.dp
      ),
    verticalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    // 标题行（图标 + 文字）
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      // 太阳图标（缩放渐显动画）
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_sunny),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.graphicsLayer {
          scaleX = textAnimValue1
          scaleY = textAnimValue1
          alpha = textAnimValue1
        }
      )
      // 标题文字（渐显和向上平移动画）
      Text(
        text = stringResource(R.string.sunrise_title),
        color = MiuixTheme.colorScheme.onSurface, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue2
          translationY = -12.dp.toPx() * (1f - textAnimValue2)
        }
      )
    }

    // 太阳轨迹可视化区域
    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f)
        .drawWithCache {
          // 路径测量工具，用于计算路径上的点
          val pathMeasure = PathMeasure()

          // 创建太阳运动轨迹（使用三次贝塞尔曲线绘制弧线）
          val sunMovePath = Path().apply {
            moveTo(0f, size.height * 1f) // 起点：左下角
            cubicTo(
              .2f * size.width,  // 控制点1x: 左侧曲率
              size.height * .0f,  // 控制点1y: 顶部高度
              .8f * size.width,  // 控制点2x: 右侧曲率
              size.height * .0f,  // 控制点2y: 顶部高度
              size.width * 1f,   // 终点x: 右下角
              size.height * 1f   // 终点y: 底部
            )
          }

          // 设置路径并计算当前进度的路径段
          pathMeasure.setPath(sunMovePath, false)
          val movePath = Path()
          // 获取从起点到当前进度的路径段
          pathMeasure.getSegment(0f, pathMeasure.length * progress.value, movePath, true)

          onDrawWithContent {
            // 绘制背景轨迹线（灰色）
            drawPath(
              path = sunMovePath,
              color = pathBgColor,
              style = Stroke(
                width = 7.dp.toPx(),
                cap = StrokeCap.Round,
              ),
            )
            // 绘制已经过的轨迹线（橙色半透明）
            drawPath(
              path = movePath,
              color = Color(0xC1FFA940),
              style = Stroke(
                width = 7.dp.toPx(),
                cap = StrokeCap.Round,
              )
            )
            // 如果还没日落，绘制太阳位置指示点
            if (progress.value != 1.0f) {
              // 绘制内圈（实心太阳）
              drawCircle(
                color = Color(0xffffa940),
                center = pathMeasure.getPosition(
                  pathMeasure.length * progress.value
                ),
                radius = 10.dp.toPx()
              )
              // 绘制外圈（发光效果）
              drawCircle(
                color = Color(0x80FFA940),
                center = pathMeasure.getPosition(
                  pathMeasure.length * progress.value
                ),
                radius = 15.dp.toPx()
              )
            }
          }
        }
        .clip(RectangleShape)
    ) {

    }


    // 底部日出日落时间显示
    Column (
      modifier = Modifier.fillMaxWidth(),
      horizontalAlignment = Alignment.CenterHorizontally,
      verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
      // 左侧：日出时间
      Text(
        text = stringResource(
          R.string.sunrise_label,
          weatherEntity.forecast.today.sunrise
        ),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
      // 右侧：日落时间
      Text(
        text = stringResource(
          R.string.sunset_label,
          weatherEntity.forecast.today.sunset
        ),
        style = MiuixTheme.textStyles.footnote1,
        color = MiuixTheme.colorScheme.onSurface,
        fontWeight = FontWeight.Bold
      )
    }

  }

}
