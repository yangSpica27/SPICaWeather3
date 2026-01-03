package me.spica.spicaweather3.ui.main.cards

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
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


@Composable
fun PrecipitationCard(
  modifier: Modifier = Modifier,
  precipitation: Int,
  pop: Int,
  startAnim: Boolean = true
) {
  val progressAnimValue = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 1050, 0)
  ).value

  val textAnimValue1 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 140, 0)
  ).value

  val textAnimValue2 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 230, 50)
  ).value

  val textAnimValue3 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 320, 70)
  ).value

  val textAnimValue4 = animateFloatAsState(
    if (startAnim) 1f else 0f, animationSpec = tween(durationMillis = 520, 170)
  ).value

  Column(
    modifier = modifier.padding(WIDGET_CARD_PADDING),
    verticalArrangement = Arrangement.spacedBy(8.dp),
    horizontalAlignment = Alignment.Start
  ) {
    Row(
      modifier = Modifier.fillMaxWidth(),
      horizontalArrangement = Arrangement.spacedBy(12.dp),
      verticalAlignment = Alignment.CenterVertically
    ) {
      Icon(
        painter = painterResource(id = R.drawable.material_symbols_outlined_rainy_light),
        contentDescription = null,
        tint = MiuixTheme.colorScheme.onSurface,
        modifier = Modifier.graphicsLayer {
          scaleX = textAnimValue1
          scaleY = textAnimValue1
          alpha = textAnimValue1
        }
      )
      Text(
        text = stringResource(R.string.precipitation_info_title),
        color = MiuixTheme.colorScheme.onSurface, style = WIDGET_CARD_TITLE_TEXT_STYLE(),
        modifier = Modifier.graphicsLayer {
          alpha = textAnimValue2
          translationY = -12.dp.toPx() * (1f - textAnimValue1)
        }
      )
    }

    Box(
      modifier = Modifier
        .fillMaxWidth()
        .weight(1f), contentAlignment = Alignment.BottomStart
    ) {
      Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(
          modifier = Modifier.alignByBaseline().graphicsLayer{
            scaleX = textAnimValue1
            scaleY = textAnimValue1
            alpha = textAnimValue1
          },
          text = "${(precipitation * progressAnimValue).toInt()}",
          color = MiuixTheme.colorScheme.onSurface,
          style = MiuixTheme.textStyles.headline1.copy(
            fontWeight = FontWeight.W900, fontSize = 56.sp
          )
        )
        Text(
          modifier = Modifier.alignByBaseline().graphicsLayer{
            alpha = textAnimValue2
            translationY = -12.dp.toPx() * (1f - textAnimValue2)
          },
          text = "mm",
          color = MiuixTheme.colorScheme.onSurface,
          style = MiuixTheme.textStyles.headline2.copy(
            fontWeight = FontWeight.W900, fontSize = 22.sp
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
          modifier = Modifier.alignByBaseline().graphicsLayer{
            alpha = textAnimValue3
            translationY = -12.dp.toPx() * (1f - textAnimValue3)
          },
          text = stringResource(R.string.precipitation_probability_label), color = MiuixTheme.colorScheme.onSurface,
          style = WIDGET_CARD_TITLE_TEXT_STYLE().copy(
            fontWeight = FontWeight.W500, fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = .9f)
          )
        )
        Text(
          modifier = Modifier.alignByBaseline().graphicsLayer{
            alpha = textAnimValue3
            translationY = -12.dp.toPx() * (1f - textAnimValue3)
          },
          text = "${pop}%", color = MiuixTheme.colorScheme.onSurface,
          style = MiuixTheme.textStyles.body1.copy(
            fontWeight = FontWeight.W500,
            fontSize = 14.sp,
            color = MiuixTheme.colorScheme.onSurface.copy(alpha = .9f)
          )
        )
      }
      Spacer(modifier = Modifier.weight(1f))
      Icon(
        modifier = Modifier.size(30.dp).graphicsLayer{
          alpha = textAnimValue4
          scaleX = textAnimValue4
          scaleY = textAnimValue4
        },
        painter = painterResource(R.drawable.material_symbols_outlined_smart_outlet),
        tint = MiuixTheme.colorScheme.onSurface,
        contentDescription = null
      )
    }
  }
}

