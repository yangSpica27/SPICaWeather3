package me.spica.spicaweather3.ui.main.cards

import androidx.annotation.StringRes
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.scale
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.kyant.backdrop.backdrops.layerBackdrop
import com.kyant.backdrop.backdrops.rememberLayerBackdrop
import com.kyant.backdrop.drawPlainBackdrop
import com.kyant.backdrop.effects.lens
import com.kyant.capsule.ContinuousRoundedRectangle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.theme.WIDGET_CARD_PADDING
import me.spica.spicaweather3.theme.WIDGET_CARD_TITLE_TEXT_STYLE
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun UVCard(uv: Int, startAnim: Boolean) {
  val backdrop = rememberLayerBackdrop()
  val uvDescriptionRes = remember(uv) { uv.toUvLevelRes() }
  val uvDescription = stringResource(id = uvDescriptionRes)
  val uvTipRes = remember(uv) { uv.toUvTipRes() }
  val uvExtraDesc = stringResource(id = uvTipRes)

  val glassColor = MiuixTheme.colorScheme.onSurface.copy(alpha = .04f)

  val progressAnimValue = animateFloatAsState(
    if (startAnim) 1f else 0f
  ).value

  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 450, 150)
  ).value

  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 550, 250)
  ).value

  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f,
    animationSpec = tween(durationMillis = 750, 350)
  ).value

  Column(
    modifier = Modifier.padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_background_blur_light),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface
      )
      Text(
        text = stringResource(R.string.uv_info_title),
        color = MiuixTheme.colorScheme.onSurface, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue1
          translationY = -12.dp.toPx() * (1f - textAnimValue1)
        }
      )
    }

    Text(
      text = uvDescription,
      color = MiuixTheme.colorScheme.onSurface,
      style = MiuixTheme.textStyles.main.copy(fontWeight = FontWeight.ExtraBold),
      modifier = Modifier.graphicsLayer {
        alpha = textAnimValue2
        translationY = -12.dp.toPx() * (1f - textAnimValue2)
      }
    )

    BoxWithConstraints(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f),
      contentAlignment = Alignment.BottomStart,
    ) {

      Box(
        modifier = Modifier
          .fillMaxWidth()
          .height(
            11.dp
          )
          .graphicsLayer {
            translationY = -5.5.dp.toPx() - 4.dp.toPx()
          }
          .layerBackdrop(backdrop)
          .background(
            brush = Brush.horizontalGradient(
              colors = listOf(
                Color(0xFF73d13d),
                Color(0xFF73d13d),
                Color(0xFFffe58f),
                Color(0xFFffd666),
                Color(0xFFFF5722),
                Color(0xFF915BF0)
              )
            ),
            shape = ContinuousRoundedRectangle(2.dp)
          )
          .border(
            1.dp,
            color = Color.White,
            ContinuousRoundedRectangle(2.dp)
          )
      )
      Box(
        modifier = Modifier
          .size(32.dp)
          .graphicsLayer {
            translationX = 0.dp.toPx() +
                (constraints.maxWidth - 32.dp.toPx()) * (((uv * 1f) / 10f).coerceIn(
              0f,
              1f
            )) * progressAnimValue
          }
          .clip(CircleShape)
          .border(
            width = 1.dp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = .1f),
            shape = CircleShape
          ),
        contentAlignment = Alignment.Center
      ) {
        Box(
          modifier = Modifier
            .fillMaxSize()
            .drawPlainBackdrop(
              backdrop = backdrop,
              shape = { CircleShape },
              onDrawFront = {
                drawRect(
                  color = glassColor,
                  size = this.size
                )
              },
              onDrawBackdrop = { draw ->
                scale(1.5f, 1.5f) {
                  draw()
                }
              },
              effects = {
                lens(
                  5f.dp.toPx(),
                  10f.dp.toPx(),
                  chromaticAberration = true
                )
              }
            )
        )
      }

    }
    Row(
      modifier = Modifier.fillMaxWidth(),
      verticalAlignment = Alignment.Bottom,
      horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
      Row {
        Text(
          modifier = Modifier
            .alignByBaseline()
            .graphicsLayer {
              alpha = textAnimValue3
              translationY = -12.dp.toPx() * (1f - textAnimValue3)
            },
          text = uvExtraDesc, color = MiuixTheme.colorScheme.onSurface,
          style = WIDGET_CARD_TITLE_TEXT_STYLE().copy(
            fontWeight = FontWeight.W500, fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = .6f)
          )
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Icon(
        modifier = Modifier.size(30.dp),
        painter = painterResource(R.drawable.material_symbols_outlined_sports_tennis),
        tint = MiuixTheme.colorScheme.onSurface,
        contentDescription = null
      )
    }
  }
}

@StringRes
private fun Int.toUvLevelRes(): Int = when {
  this < 3 -> R.string.uv_level_low
  this < 6 -> R.string.uv_level_medium
  this < 8 -> R.string.uv_level_high
  else -> R.string.uv_level_extreme
}

@StringRes
private fun Int.toUvTipRes(): Int = when {
  this < 3 -> R.string.uv_tip_low
  this < 6 -> R.string.uv_tip_medium
  this < 8 -> R.string.uv_tip_high
  else -> R.string.uv_tip_extreme
}

