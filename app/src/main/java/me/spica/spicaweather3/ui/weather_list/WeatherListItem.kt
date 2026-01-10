package me.spica.spicaweather3.ui.weather_list

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.ui.widget.particle.ThanosDisintegrateContainer
import me.spica.spicaweather3.utils.noRippleClickable
import sh.calvin.reorderable.ReorderableCollectionItemScope
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme
import kotlin.random.Random

/**
 * 天气列表项组件
 *
 * 展示单个城市的天气信息，支持拖拽排序、删除动画、抖动效果等。
 *
 * @param item 城市天气数据状态
 * @param index 在列表中的索引
 * @param isDrag 当前是否有任何项正在被拖拽
 * @param disintegratingCityId 正在消散的城市ID
 * @param onDragStart 开始拖拽回调
 * @param onDragStop 停止拖拽回调
 * @param onClick 点击回调（用于显示删除对话框）
 * @param onDisintegrationComplete 消散完成回调
 */
@Composable
fun ReorderableCollectionItemScope.WeatherListItem(
    item: WeatherPageState,
    index: Int,
    isDrag: Boolean,
    disintegratingCityId: Long?,
    onDragStart: () -> Unit,
    onDragStop: () -> Unit,
    onClick: () -> Unit,
    onDisintegrationComplete: () -> Unit
) {
    // 入场动画
    val appearAnim = remember(item.cityEntity.id) { androidx.compose.animation.core.Animatable(0f) }
    val delayMillis = remember(item.cityEntity.id) { Random.nextInt(0, 200) }
    val durationMillis = remember(item.cityEntity.id) { Random.nextInt(260, 520) }

    // 当前item是否正在拖拽
    var draging by remember { mutableStateOf(false) }

    LaunchedEffect(item.cityEntity.id) {
        appearAnim.snapTo(0f)
        appearAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = durationMillis,
                delayMillis = delayMillis
            )
        )
    }

    val progress = appearAnim.value

    with(LocalSharedTransitionScope.current) {
        // 拖拽时的阴影和缩放动画
        val elevation by animateDpAsState(if (draging) 1.dp else 2.dp, label = "elevation")
        val scale by animateFloatAsState(
            if (draging) 1.02f else 1.0f,
            label = "scale"
        )

        // iOS 风格的抖动动画
        val infiniteTransition = rememberInfiniteTransition(label = "shake")
        val shakeOffset by infiniteTransition.animateFloat(
            initialValue = -1f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "shake_offset"
        )

        // 判断是否应该显示抖动效果
        val shouldShake = isDrag && !draging

        // 判断当前卡片是否正在消散
        val isThisCardDisintegrating =
            disintegratingCityId == item.cityEntity.id.hashCode().toLong()

        // 消散完成后标记，用于隐藏卡片防止闪烁
        var hasDisintegrated by remember { mutableStateOf(false) }

        ThanosDisintegrateContainer(
            isDisintegrating = isThisCardDisintegrating,
            onDisintegrationComplete = {
                hasDisintegrated = true
                onDisintegrationComplete()
            },
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = scale * progress
                    scaleY = scale * progress
                    translationY = (1f - progress) * 36.dp.toPx()
                    alpha = if (hasDisintegrated) 0f else 1f

                    // 应用细微的抖动效果（首项不抖动）
                    if (shouldShake && index != 0) {
                        rotationZ = shakeOffset * 0.5f
                        translationX = shakeOffset * 1.5f
                    }
                }
                .padding(top = if (index != 0) 12.dp else 0.dp)
                .padding(horizontal = 22.dp)
                .then(if (hasDisintegrated) Modifier else Modifier)
        ) {
            WeatherItemContent(
                modifier = Modifier
                    .fillMaxSize()
                    .noRippleClickable(onClick = onClick)
                    .longPressDraggableHandle(
                        enabled = index != 0,
                        onDragStarted = {
                            onDragStart()
                            draging = true
                        },
                        onDragStopped = {
                            onDragStop()
                            draging = false
                        }
                    )
                    .shadow(
                        elevation = elevation,
                        shape = ContinuousRoundedRectangle(12.dp),
                    )
                    .clip(ContinuousRoundedRectangle(12.dp)),
                cityData = item
            )
        }
    }
}

/**
 * 天气卡片内容
 *
 * 展示城市名、地区和当前温度，背景色根据天气类型动画变化。
 *
 * @param modifier 修饰符
 * @param cityData 城市天气数据状态
 * @param initColor 初始背景颜色
 */
@Composable
private fun WeatherItemContent(
    modifier: Modifier = Modifier,
    cityData: WeatherPageState,
    initColor: Color = MiuixTheme.colorScheme.onSurface
) {
    // 卡片背景色，会根据天气类型动画变化
    val cardColor = remember { androidx.compose.animation.Animatable(initColor) }
    val cityEntity = remember(cityData) { cityData.cityEntity }
    val isUserLoc = remember(cityEntity) { cityEntity.isUserLoc }

    // 根据天气数据动画更新卡片背景色
    LaunchedEffect(cityData) {
        if (cityData is WeatherPageState.Data) {
            val iconId = cityEntity.weather?.current?.icon ?: "100"
            cardColor.animateTo(WeatherAnimType.getAnimType(iconId).topColor)
        } else {
            cardColor.animateTo(initColor)
        }
    }

    Row(
        modifier = modifier
            .background(cardColor.value)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            horizontalAlignment = Alignment.Start,
        ) {
            // 城市名称
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    cityEntity.name,
                    color = MiuixTheme.colorScheme.surface,
                    textAlign = TextAlign.Start,
                    style = MiuixTheme.textStyles.title2,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 22.sp
                )
                if (isUserLoc) {
                    Icon(
                        painter = painterResource(R.drawable.ic_location),
                        contentDescription = null,
                        tint = MiuixTheme.colorScheme.surface,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            // 省份和市区信息
            Text(
                "${cityEntity.adm1},${cityEntity.adm2}",
                color = MiuixTheme.colorScheme.surface,
                textAlign = TextAlign.Start,
                style = MiuixTheme.textStyles.body2,
                fontWeight = FontWeight.Normal,
                fontSize = 16.sp
            )
        }

        // 温度或加载动画
        AnimatedContent(
            targetState = cityData,
            label = "city_state",
            transitionSpec = { fadeIn() togetherWith fadeOut() }
        ) { state ->
            Box(
                modifier = Modifier.fillMaxHeight(),
                contentAlignment = Alignment.Center
            ) {
                when (state) {
                    is WeatherPageState.Data -> {
                        Text(
                            buildAnnotatedString {
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 40.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                ) {
                                    append("${state.cityEntity.weather?.current?.temperature}")
                                }
                                withStyle(
                                    style = SpanStyle(
                                        fontSize = 32.sp,
                                        fontWeight = FontWeight.Light
                                    )
                                ) {
                                    append("℃")
                                }
                            },
                            color = MiuixTheme.colorScheme.surface,
                            style = MiuixTheme.textStyles.main.copy(fontSize = 32.sp),
                            fontWeight = FontWeight.Light,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.fillMaxHeight()
                        )
                    }

                    else -> {
                        InfiniteProgressIndicator(
                            color = MiuixTheme.colorScheme.surface,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }
            }
        }
    }
}
