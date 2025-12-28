package me.spica.spicaweather3.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import me.spica.spicaweather3.common.WeatherCardConfig
import me.spica.spicaweather3.common.WeatherCardType

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

    // 天气卡片配置（JSON 字符串）
    val KEY_WEATHER_CARDS_CONFIG = stringPreferencesKey("weather_cards_config")

    private val gson = com.google.gson.Gson()

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

  // ==================== 天气卡片配置相关方法 ====================

  /**
   * 保存天气卡片配置列表
   * @param configs 卡片配置列表
   */
  suspend fun setWeatherCardsConfig(configs: List<WeatherCardConfig>) =
    withContext(Dispatchers.IO) {
      context.dataStore.edit { preferences ->
        val jsonString = gson.toJson(configs)
        preferences[KEY_WEATHER_CARDS_CONFIG] = jsonString
      }
    }

  /**
   * 获取天气卡片配置列表
   * 如果未保存过配置，返回默认配置
   */
  fun getWeatherCardsConfig(): Flow<List<WeatherCardConfig>> =
    context.dataStore.data
      .map { preferences ->
        val jsonString = preferences[KEY_WEATHER_CARDS_CONFIG]
        if (jsonString.isNullOrEmpty()) {
          // 返回默认配置
          getDefaultWeatherCardsConfig()
        } else {

          try {
            val list = gson.fromJson(
              jsonString,
              Array<WeatherCardConfig>::class.java
            ).toList()
            if (list.isEmpty()) {
              return@map getDefaultWeatherCardsConfig()
            } else {
              return@map list
            }
          } catch (e: Exception) {
            // 解析失败，返回默认配置
            e.printStackTrace()
            return@map getDefaultWeatherCardsConfig()
          }
        }
      }
      .distinctUntilChanged()


  /**
   * 更新单个卡片的显示状态
   * @param cardType 卡片类型
   * @param isVisible 是否可见
   */
  suspend fun updateCardVisibility(cardType: WeatherCardType, isVisible: Boolean) =
    withContext(Dispatchers.IO) {
      context.dataStore.edit { preferences ->
        val jsonString = preferences[KEY_WEATHER_CARDS_CONFIG]
        val currentConfigs = if (jsonString.isNullOrEmpty()) {
          getDefaultWeatherCardsConfig()
        } else {
          try {
            gson.fromJson(jsonString, Array<WeatherCardConfig>::class.java).toList()
          } catch (e: Exception) {
            getDefaultWeatherCardsConfig()
          }
        }

        // 更新指定卡片的可见性
        val updatedConfigs = currentConfigs.map { config ->
          if (config.cardType == cardType) {
            config.copy(isVisible = isVisible)
          } else {
            config
          }
        }

        preferences[KEY_WEATHER_CARDS_CONFIG] = gson.toJson(updatedConfigs)
      }
    }

  /**
   * 更新卡片顺序
   * @param reorderedCards 重新排序后的卡片列表
   */
  suspend fun updateCardsOrder(reorderedCards: List<WeatherCardConfig>) =
    withContext(Dispatchers.IO) {
      context.dataStore.edit { preferences ->
        // 重新分配 order 值
        val updatedConfigs = reorderedCards.mapIndexed { index, config ->
          config.copy(order = index)
        }
        preferences[KEY_WEATHER_CARDS_CONFIG] = gson.toJson(updatedConfigs)
      }
    }

  /**
   * 重置卡片配置为默认值
   */
  suspend fun resetWeatherCardsConfig() =
    withContext(Dispatchers.IO) {
      context.dataStore.edit { preferences ->
        preferences.remove(KEY_WEATHER_CARDS_CONFIG)
      }
    }

  /**
   * 获取默认的卡片配置列表
   * 所有卡片默认可见且按照 defaultOrder 排序
   */
  private fun getDefaultWeatherCardsConfig(): List<WeatherCardConfig> {
    return WeatherCardType.entries.map { cardType ->
      WeatherCardConfig(
        cardType = cardType,
        isVisible = true,
        order = cardType.defaultOrder
      )
    }
  }

}
