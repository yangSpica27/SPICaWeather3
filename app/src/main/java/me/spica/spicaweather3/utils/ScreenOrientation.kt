package me.spica.spicaweather3.utils

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalConfiguration

/**
 * 屏幕方向检测工具
 */

/**
 * 判断当前是否为横屏
 */
@Composable
fun isLandscape(): Boolean {
    val configuration = LocalConfiguration.current
    return remember {
        derivedStateOf {
            configuration.orientation == Configuration.ORIENTATION_LANDSCAPE
        }
    }.value
}

/**
 * 判断当前是否为竖屏
 */
@Composable
fun isPortrait(): Boolean {
    val configuration = LocalConfiguration.current
    return remember {
        derivedStateOf {
            configuration.orientation == Configuration.ORIENTATION_PORTRAIT
        }
    }.value
}
