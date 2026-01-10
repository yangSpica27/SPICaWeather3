package me.spica.spicaweather3.ui.weather_list

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.materials.HazeMaterials
import me.spica.spicaweather3.R
import me.spica.spicaweather3.ui.LocalAnimatedContentScope
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.widget.MainTopBar
import me.spica.spicaweather3.utils.noRippleClickable
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme

/**
 * 天气列表顶栏组件
 * 
 * 包含返回按钮、标题和搜索栏，支持毛玻璃效果和共享元素转场动画。
 * 
 * @param scrollBehavior 滚动行为控制器
 * @param hazeState 毛玻璃效果状态
 * @param onNavigateBack 返回按钮点击回调
 * @param onSearchClick 搜索框点击回调
 */
@Composable
fun WeatherListTopBar(
  scrollBehavior: ScrollBehavior,
  hazeState: HazeState,
  onNavigateBack: () -> Unit,
  onSearchClick: () -> Unit
) {
  MainTopBar(
    modifier = Modifier.hazeEffect(
      state = hazeState,
      style = HazeMaterials.ultraThin(MiuixTheme.colorScheme.surface)
    ) {
      progressive = HazeProgressive.verticalGradient(startIntensity = 1f, endIntensity = 0f)
    },
    scrollBehavior = scrollBehavior,
    title = {
      Text(
        stringResource(R.string.weather_list_title),
        style = MiuixTheme.textStyles.headline2,
        color = MiuixTheme.colorScheme.onSurface,
        fontWeight = FontWeight.W600
      )
    },
    largeTitle = {
      Box(
        modifier = Modifier
          .fillMaxWidth()
          .padding(top = 48.dp)
      ) {
        with(LocalSharedTransitionScope.current) {
          Row(
            modifier = Modifier
              .fillMaxWidth()
              // 共享元素动画，与城市选择页面的搜索框联动
              .sharedBounds(
                animatedVisibilityScope = LocalAnimatedContentScope.current,
                sharedContentState = rememberSharedContentState("search_bar"),
              )
              .background(
                MiuixTheme.colorScheme.onSurface.copy(alpha = .1f),
                ContinuousRoundedRectangle(12.dp)
              )
              .noRippleClickable(onClick = onSearchClick)
              .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
          ) {
            Icon(
              Icons.Default.Search,
              contentDescription = stringResource(R.string.cd_search),
              tint = MiuixTheme.colorScheme.onSurface
            )

            Text(
              stringResource(R.string.weather_list_search_placeholder),
              style = MiuixTheme.textStyles.subtitle,
              color = MiuixTheme.colorScheme.onSurface,
              fontSize = 17.sp
            )
          }
        }
      }
    },
    navigationIcon = {
      IconButton(onClick = onNavigateBack) {
        Icon(
          Icons.AutoMirrored.Default.ArrowBack,
          contentDescription = stringResource(R.string.cd_back)
        )
      }
    }
  )
}
