package me.spica.spicaweather3.ui.widget.particle

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import me.spica.spicaweather3.utils.thanos_effect.ThanosEffect

/** 灭霸消散效果容器，通过页面的粒子叠加层显示动画。 */
@Composable
fun ThanosDisintegrateContainer(
    isDisintegrating: Boolean,
    onDisintegrationComplete: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    ThanosEffect(
        isDisintegrating = isDisintegrating,
        onDisintegrationComplete = onDisintegrationComplete,
        modifier = modifier,
        content = content,
    )
}
