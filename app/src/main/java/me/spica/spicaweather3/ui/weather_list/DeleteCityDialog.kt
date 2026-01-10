package me.spica.spicaweather3.ui.weather_list

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import me.spica.spicaweather3.R
import me.spica.spicaweather3.data.local.db.entity.CityEntity
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog

/**
 * 删除城市确认对话框
 * 
 * 用于确认删除操作，显示城市名称并提供确认/取消选项。
 * 
 * @param cityName 要删除的城市名称
 * @param show 对话框显示状态
 * @param onConfirm 确认删除回调
 * @param onDismiss 取消/关闭回调
 */
@Composable
fun DeleteCityDialog(
  cityName: String,
  show: MutableState<Boolean>,
  onConfirm: () -> Unit,
  onDismiss: () -> Unit
) {
  SuperDialog(
    title = stringResource(R.string.dialog_title_notice),
    summary = stringResource(R.string.weather_list_delete_city_message, cityName),
    show = show,
    onDismissRequest = onDismiss
  ) {
    TextButton(
      text = stringResource(R.string.action_confirm),
      onClick = {
        onConfirm()
        show.value = false
      },
      colors = ButtonDefaults.textButtonColorsPrimary(),
      modifier = Modifier.fillMaxWidth()
    )
  }
}
