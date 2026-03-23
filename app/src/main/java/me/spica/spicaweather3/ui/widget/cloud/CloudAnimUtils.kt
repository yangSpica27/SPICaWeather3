package me.spica.spicaweather3.ui.widget.cloud

import kotlin.math.PI
import kotlin.math.cos

/**
 * 平滑正弦波，输出 [0, 1] 之间的来回振荡值。
 * 使用 (1 - cos(2πt)) / 2，等效于 EaseInOutSine，
 * 在波峰和波谷处平滑减速，避免三角波的速度突变（生硬感）。
 */
internal fun smoothWave(elapsed: Long, period: Long): Float {
    val t = (elapsed % period).toFloat() / period
    return (1f - cos(t * 2f * PI.toFloat())) / 2f
}

/**
 * 三角波函数，适用于已经通过 sin() 二次处理的场合（如呼吸动画）。
 */
internal fun triangleWave(elapsed: Long, period: Long): Float {
    val t = (elapsed % period).toFloat() / period
    return if (t < 0.5f) t * 2f else (1f - t) * 2f
}
