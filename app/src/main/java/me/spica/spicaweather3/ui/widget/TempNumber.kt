package me.spica.spicaweather3.ui.widget

import androidx.compose.animation.core.Animatable
import androidx.compose.foundation.layout.Row
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.theme.MiuixTheme


@Composable
fun TempNumber(modifier: Modifier = Modifier, temp: Int, unit: String) {

  val tempNumberAnim = remember { Animatable(temp.toFloat()) }

  LaunchedEffect(temp) {
    tempNumberAnim.animateTo(temp.toFloat())
  }

  Row(
    modifier = modifier,
    verticalAlignment = Alignment.Bottom,
  ) {
    Text(
      "${tempNumberAnim.value.toInt()}",
      modifier = Modifier.alignByBaseline(),
      style = MiuixTheme.textStyles.title1
        .copy(
          fontSize = 100.sp, fontWeight = FontWeight.Bold,
          color = Color.White.copy(alpha = 0.8f),
        ),
    )
    Text(
      unit,
      modifier = Modifier.alignByBaseline(),
      style = MiuixTheme.textStyles.title1
        .copy(
          fontSize = 50.sp, fontWeight = FontWeight.Bold,
          color = Color.White.copy(alpha = 0.8f),
        ),
    )
  }
}
