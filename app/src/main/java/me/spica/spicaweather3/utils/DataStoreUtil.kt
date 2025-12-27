package me.spica.spicaweather3.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext

// 字典工具类
class DataStoreUtil(
  private val context: Context,
) {
  companion object {
    private val Context.dataStore: DataStore<Preferences> by preferencesDataStore("settings")

    // 用户是否同意隐私政策
    val AGREE_PRIVACY = booleanPreferencesKey("agree_privacy")

    // 是否是第一次启动
    val KEY_IS_FIRST_LAUNCH = booleanPreferencesKey("is_first_launch")

  }

  suspend fun setIsFirstLaunch(value: Boolean) =
    withContext(Dispatchers.IO) {
      context.dataStore.edit {
        it[KEY_IS_FIRST_LAUNCH] = value
      }
    }

  fun getIsFirstLaunch(): Flow<Boolean> =
    context.dataStore.data
      .map { preferences -> preferences[KEY_IS_FIRST_LAUNCH] ?: false }
      .conflate()
      .distinctUntilChanged()

  suspend fun setAgreePrivacy(value: Boolean) =
    withContext(Dispatchers.IO) {
      context.dataStore.edit {
        it[AGREE_PRIVACY] = value
      }
    }

  fun getAgreePrivacy(): Flow<Boolean> =
    context.dataStore.data
      .map {
        it[AGREE_PRIVACY] ?: false
      }
      .distinctUntilChanged()


}
