package me.spica.spicaweather3.ui.main.cards

import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableStringBuilder
import android.text.StaticLayout
import android.text.TextPaint
import android.text.style.AbsoluteSizeSpan
import android.text.style.StyleSpan
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.boundsInRoot
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.domain.model.WeatherData
import me.spica.spicaweather3.ui.widget.WeatherBackground
import me.spica.spicaweather3.ui.widget.rain.RainTextCollision
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.math.ceil

/**
 * 当前天气卡片
 * 展示当前实时天气信息的核心卡片组件，包含动态天气背景和主要天气指标
 * 特点：
 * - 动态天气背景动画（晴天、雨天、雪天等不同场景）
 * - 大号温度显示
 * - 体感温度和湿度信息
 * - 文字渐显和平移动画
 * - 毛玻璃模糊效果
 *
 * @param modifier 修饰符
 * @param weatherData 当前天气数据
 * @param startAnim 是否开始播放动画
 */
@Composable
fun NowCard(modifier: Modifier = Modifier, weatherData: WeatherData, startAnim: Boolean) {


    // 根据天气图标ID计算对应的天气动画类型（晴天、雨天、雪天等）
    val currentWeatherAnimType = remember(weatherData) {
        WeatherAnimType.getAnimType(
            weatherData.current.icon,
        )
    }

    // ==================== 动画配置 ====================
    // 温度数字动画（无延迟，持续450ms）
    val textAnimValue1 = animateFloatAsState(
        if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 250, 0)
    ).value

    // 天气状况和体感温度动画（延迟50ms，持续550ms）
    val textAnimValue2 = animateFloatAsState(
        if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 350, 50)
    ).value

    // 湿度信息动画（延迟150ms，持续750ms）
    val textAnimValue3 = animateFloatAsState(
        if (startAnim) 1f else 0f,
        animationSpec = tween(durationMillis = 550, 150)
    ).value

    // ==================== 温度文本碰撞追踪 ====================
    var boxRectInRoot by remember { mutableStateOf(Rect.Zero) }
    var temperatureRectInRoot by remember { mutableStateOf(Rect.Zero) }
    val density = LocalDensity.current
    val tempTextSizePx = with(density) { 94.sp.toPx() }.toInt()
    val unitTextSizePx = with(density) { 45.sp.toPx() }.toInt()
    val textCollision = remember(
        weatherData.current.temperature,
        boxRectInRoot,
        temperatureRectInRoot,
        tempTextSizePx,
        unitTextSizePx,
    ) {
        if (boxRectInRoot == Rect.Zero || temperatureRectInRoot == Rect.Zero) {
            null
        } else {
            RainTextCollision(
                bitmap = createTemperatureCollisionBitmap(
                    temperature = weatherData.current.temperature,
                    widthPx = ceil(temperatureRectInRoot.width).toInt().coerceAtLeast(1),
                    heightPx = ceil(temperatureRectInRoot.height).toInt().coerceAtLeast(1),
                    temperatureTextSizePx = tempTextSizePx,
                    unitTextSizePx = unitTextSizePx,
                ),
                left = temperatureRectInRoot.left - boxRectInRoot.left,
                top = temperatureRectInRoot.top - boxRectInRoot.top,
            )
        }
    }

    // ==================== 主布局 ====================
    // 使用 Box 叠加布局：底层为天气背景动画，上层为天气信息文字
    Box(
        modifier = modifier
            .aspectRatio(1.21f) // 宽高比 1.21:1，保持卡片比例
            .onGloballyPositioned { boxRectInRoot = it.boundsInRoot() }
    ) {
        // 动态天气背景（晴天、雨天、雪天等不同动画效果）
        WeatherBackground(
            currentWeatherType = WeatherAnimType.RainLight,
            collapsedFraction = 0f, // 0=完全展开，1=完全折叠
            textCollision = textCollision,
            weatherData = weatherData
        )

        // 天气信息文字层
        Column(
            modifier = Modifier
                .padding(
                    12.dp
                )
                .padding(
                    horizontal = 12.dp,
                    vertical = 12.dp
                ),
            verticalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            // 当前温度（大号显示，带渐显和向上平移动画）
            Text(
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAnimValue1
                        translationY = -12.dp.toPx() * (1f - textAnimValue1)
                    }
                    .onGloballyPositioned { temperatureRectInRoot = it.boundsInRoot() },
                text = buildAnnotatedString {
                    // 温度数字（94sp 超大字体）
                    withStyle(
                        style = SpanStyle(
                            color = MiuixTheme.colorScheme.surface,
                            fontSize = 94.sp,
                            fontWeight = FontWeight.W800
                        )
                    ) {
                        append(weatherData.current.temperature.toString())
                    }
                    // 温度单位（45sp 较小字体）
                    withStyle(
                        style = SpanStyle(
                            color = MiuixTheme.colorScheme.surface,
                            fontSize = 45.sp,
                            fontWeight = FontWeight.W800
                        )
                    ) {
                        append("°C")
                    }
                },
            )

            // 天气状况和体感温度（带延迟渐显和向上平移动画）
            Text(
                text = stringResource(
                    R.string.now_card_condition,
                    weatherData.current.condition,
                    weatherData.current.feelsLike
                ),
                style = MiuixTheme.textStyles.body1,
                color = MiuixTheme.colorScheme.surface,
                fontWeight = FontWeight.W600,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAnimValue2
                        translationY = -12.dp.toPx() * (1f - textAnimValue2)
                    }
                    .padding(start = 12.dp)
            )

            // 湿度信息（带毛玻璃背景、延迟渐显和向上平移动画）
            Text(
                text = stringResource(
                    R.string.now_card_humidity,
                    weatherData.current.humidity
                ),
                style = MiuixTheme.textStyles.body2,
                color = MiuixTheme.colorScheme.surface,
                fontWeight = FontWeight.W600,
                modifier = Modifier
                    .graphicsLayer {
                        alpha = textAnimValue3
                        translationY = -12.dp.toPx() * (1f - textAnimValue3)
                    }
                    .padding(start = 12.dp)
                    .clip(
                        RoundedCornerShape(12.dp)
                    )
                    // 应用 Cupertino 风格超薄毛玻璃效果
                    .background(
                        MiuixTheme.colorScheme.surface.copy(alpha = 0.2f),
                        CircleShape
                    )
                    .padding(
                        horizontal = 8.dp,
                        vertical = 4.dp
                    )
            )
        }
    }
}

private fun createTemperatureCollisionBitmap(
    temperature: Int,
    widthPx: Int,
    heightPx: Int,
    temperatureTextSizePx: Int,
    unitTextSizePx: Int,
): Bitmap {
    val text = SpannableStringBuilder("${temperature}°C").apply {
        val tempEnd = temperature.toString().length
        setSpan(AbsoluteSizeSpan(temperatureTextSizePx), 0, tempEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), 0, tempEnd, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(AbsoluteSizeSpan(unitTextSizePx), tempEnd, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        setSpan(StyleSpan(Typeface.BOLD), tempEnd, length, android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
    }
    val paint = TextPaint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        typeface = Typeface.DEFAULT_BOLD
    }
    val layout = StaticLayout.Builder
        .obtain(text, 0, text.length, paint, widthPx)
        .setAlignment(Layout.Alignment.ALIGN_NORMAL)
        .setIncludePad(false)
        .build()

    return Bitmap.createBitmap(widthPx, heightPx, Bitmap.Config.ARGB_8888).also { bitmap ->
        Canvas(bitmap).apply {
            if (layout.height < heightPx) {
                translate(0f, ((heightPx - layout.height) * 0.5f))
            }
            layout.draw(this)
        }
    }
}
