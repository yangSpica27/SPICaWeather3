package me.spica.spicaweather3.ui.main.weather

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SharedTransitionScope
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.util.fastRoundToInt
import com.kyant.capsule.ContinuousRoundedRectangle
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.SharedContentKey
import me.spica.spicaweather3.common.WeatherAnimType
import me.spica.spicaweather3.network.model.weather.WeatherData
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.theme.COLOR_BLACK_100
import me.spica.spicaweather3.theme.COLOR_WHITE_100
import me.spica.spicaweather3.theme.WIDGET_CARD_CORNER_SHAPE
import me.spica.spicaweather3.ui.LocalAnimatedContentScope
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.air_quality.AirQualityScreen
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.main.cards.AlertCard
import me.spica.spicaweather3.ui.main.cards.AqiCard
import me.spica.spicaweather3.ui.main.cards.DailyCard
import me.spica.spicaweather3.ui.main.cards.FeelTempCard
import me.spica.spicaweather3.ui.main.cards.HourlyCard
import me.spica.spicaweather3.ui.main.cards.HumidityCard
import me.spica.spicaweather3.ui.main.cards.MinutelyCard
import me.spica.spicaweather3.ui.main.cards.NowCard
import me.spica.spicaweather3.ui.main.cards.PrecipitationCard
import me.spica.spicaweather3.ui.main.cards.SunriseCard
import me.spica.spicaweather3.ui.main.cards.UVCard
import me.spica.spicaweather3.ui.widget.AnimateOnEnter
import me.spica.spicaweather3.ui.widget.LocalMenuState
import me.spica.spicaweather3.ui.widget.RainDropContent
import me.spica.spicaweather3.ui.widget.ShowOnIdleContent
import me.spica.spicaweather3.utils.noRippleClickable
import org.koin.compose.viewmodel.koinActivityViewModel
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.ScrollBehavior
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.icon.MiuixIcons
import top.yukonga.miuix.kmp.icon.icons.other.GitHub
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.pressable
import top.yukonga.miuix.kmp.utils.scrollEndHaptic


@Composable
fun WeatherPage(
  weatherState: WeatherPageState, modifier: Modifier = Modifier, scrollBehavior: ScrollBehavior
) {

  AnimatedContent(
    weatherState,
    modifier = modifier,
    label = "WeatherPage",
    contentKey = { it },
  ) { state ->
    when (state) {

      is WeatherPageState.Data -> {
        DataPage(weatherEntity = state.cityEntity.weather!!, scrollBehavior = scrollBehavior)
      }

      is WeatherPageState.Empty -> {
        EmptyPage()
      }
    }
  }
}

@Composable
private fun Loading() {
  Box(
    modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center
  ) {
    InfiniteProgressIndicator(
      modifier = Modifier.size(48.dp), color = Color.White
    )
  }
}

@Composable
private fun DataPage(
  modifier: Modifier = Modifier,
  weatherEntity: WeatherData,
  scrollBehavior: ScrollBehavior
) {

  val currentAnimType = remember(weatherEntity) {
    WeatherAnimType.getAnimType(
      iconId = weatherEntity.todayWeather.iconId.toString()
    )
  }

  val menuState = LocalMenuState.current

  val navController = LocalNavController.current

  Column(
    modifier = Modifier
      .fillMaxSize()
      .verticalScroll(rememberScrollState())
      .nestedScroll(scrollBehavior.nestedScrollConnection)
      .scrollEndHaptic()
    ,
    verticalArrangement = Arrangement.spacedBy(12.dp),
  ) {

    AnimateOnEnter(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp), animationSpec = spring(
        dampingRatio = .5f, stiffness = 50f
      )
    ) { progress, anim ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pressable(delay = 0)
          .graphicsLayer {
            alpha = 1.0f * progress
            scaleX = .8f + .2f * progress
            scaleY = .8f + .2f * progress
            translationY = -24.dp.toPx() + 24.dp.toPx() * progress
          }
          .cardBackground()
          .clip(WIDGET_CARD_CORNER_SHAPE)
      ) {
        RainDropContent(
          modifier = Modifier.fillMaxWidth(),
          enable = currentAnimType.showRain,
          uRunningDropAmount = .3f,
          uStaticDropAmount = .3f,
          uStaticDropSize = .7f,
          uRunningDropSize = .4f
        ) {
          NowCard(
            modifier = Modifier.fillMaxWidth(),
            weatherData = weatherEntity,
            startAnim = !anim.isRunning && anim.value > 0
          )
        }
      }
    }

    if (weatherEntity.warnings.isNotEmpty()){
      AnimateOnEnter(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp), animationSpec = spring(
          dampingRatio = .7f,
          stiffness = 180f
        )
      ) { progress, _ ->
        Box(
          modifier = Modifier
            .fillMaxWidth()
            .pressable()
            .graphicsLayer {
              alpha = .6f + .4f * progress
              scaleX = .9f + .1f * progress
              scaleY = .9f + .1f * progress
              translationY = -16.dp.toPx() + 16.dp.toPx() * progress
            }
            .cardBackground()
        ) {
          AlertCard(weatherEntity)
        }
      }
    }

    if (currentAnimType.showRain) {
      AnimateOnEnter(
        modifier = Modifier
          .fillMaxWidth()
          .padding(horizontal = 16.dp), animationSpec = spring(
          dampingRatio = .5f, stiffness = 50f
        )
      ) { progress, anim ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pressable()
            .graphicsLayer {
              alpha = 1.0f * progress
              scaleX = .8f + .2f * progress
              scaleY = .8f + .2f * progress
              translationY = -24.dp.toPx() + 24.dp.toPx() * progress
            }
            .cardBackground()
            .clip(WIDGET_CARD_CORNER_SHAPE)
        ) {
          RainDropContent(
            modifier = Modifier.fillMaxWidth(),
            enable = true,
          ) {
            MinutelyCard(
              modifier = Modifier.fillMaxWidth(),
              weatherData = weatherEntity
            )
          }
        }
      }
    }

    AnimateOnEnter(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp), animationSpec = spring(
        dampingRatio = .5f, stiffness = 50f
      )
    ) { progress, anim ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pressable()
          .graphicsLayer {
            alpha = 1.0f * progress
            scaleX = .8f + .2f * progress
            scaleY = .8f + .2f * progress
            translationY = -24.dp.toPx() + 24.dp.toPx() * progress
          }
          .cardBackground()
          .clip(WIDGET_CARD_CORNER_SHAPE)
      ) {
        RainDropContent(
          modifier = Modifier.fillMaxWidth(),
          enable = currentAnimType == WeatherAnimType.RainDark
              ||
              currentAnimType == WeatherAnimType.RainLight
        ) {
          HourlyCard(
            modifier = Modifier.fillMaxWidth(),
            weatherData = weatherEntity
          )
        }
      }
    }

    AnimateOnEnter(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp), animationSpec = spring(
        dampingRatio = .5f, stiffness = 50f
      )
    ) { progress, anim ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .graphicsLayer {
            alpha = 1.0f * progress
            scaleX = .8f + .2f * progress
            scaleY = .8f + .2f * progress
            translationY = -24.dp.toPx() + 24.dp.toPx() * progress
          }
          .cardBackground()
      ) {
        DailyCard(data = weatherEntity)
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      AnimateOnEnter(
        modifier = Modifier
          .weight(1f)
          .aspectRatio(1f), animationSpec = spring(
          dampingRatio = .9f, stiffness = 540f
        )
      ) { progress, anim ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pressable()
            .graphicsLayer {
              alpha = .5f + .5f * progress
              scaleX = .5f + .5f * progress
              scaleY = .5f + .5f * progress
              translationY = -24.dp.toPx() + 24.dp.toPx() * progress
            }
            .cardBackground()
        ) {
          ShowOnIdleContent(true) {
            UVCard(
              weatherEntity.dailyWeather.firstOrNull()?.uv?.toIntOrNull() ?: 0,
              !anim.isRunning && progress > 0f
            )
          }
        }
      }
      AnimateOnEnter(
        modifier = Modifier
          .weight(1f)
          .aspectRatio(1f), animationSpec = spring(
          dampingRatio = .2f, stiffness = 600f
        )
      ) { progress, anim ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pressable()
            .graphicsLayer {
              alpha = progress
              scaleX = .8f + .2f * progress
              scaleY = .8f + .2f * progress
              translationY = -24.dp.toPx() + 24.dp.toPx() * progress
            }
            .cardBackground()
        ) {
          ShowOnIdleContent(true) {
            FeelTempCard(
              feelTemp = weatherEntity.todayWeather.feelTemp * progress.fastRoundToInt(),
              startAnim = !anim.isRunning && anim.value > 0
            )
          }
        }
      }
    }

    Row(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp),
      horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
      AnimateOnEnter(
        modifier = Modifier
          .weight(1f)
          .aspectRatio(1f), animationSpec = spring(
          dampingRatio = .2f, stiffness = 500f
        )
      ) { progress, anim ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pressable(enabled = true)
            .graphicsLayer {
              scaleX = .8f + .2f * progress
              scaleY = .8f + .2f * progress
              alpha = progress
            }
            .cardBackground()
        ) {
          ShowOnIdleContent(true) {
            PrecipitationCard(
              precipitation = weatherEntity.dailyWeather.firstOrNull()?.precip?.toInt() ?: 0,
              pop = weatherEntity.hourlyWeather.firstOrNull()?.pop ?: 0,
              startAnim = anim.value > 0.5
            )
          }
        }
      }
      AnimateOnEnter(
        modifier = Modifier
          .weight(1f)
          .aspectRatio(1f), animationSpec = spring(
          dampingRatio = .7f, stiffness = 200f
        )
      ) { progress, anim ->
        Box(
          modifier = Modifier
            .fillMaxSize()
            .pressable(delay = 0)
            .graphicsLayer {
              alpha = progress
              scaleX = .5f + .5f * progress
              scaleY = .5f + .5f * progress
            }
            .cardBackground()
            .clip(WIDGET_CARD_CORNER_SHAPE)
        ) {
          ShowOnIdleContent(true) {
            HumidityCard(
              humidity = weatherEntity.todayWeather.water,
              startAnim = !anim.isRunning && anim.value > 0
            )
          }
        }
      }
    }
    AnimateOnEnter(
      modifier = Modifier
        .fillMaxWidth()
        .padding(horizontal = 16.dp), animationSpec = spring(
        dampingRatio = .5f, stiffness = 500f
      )
    ) { progress, anim ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pressable(delay = 0)
          .graphicsLayer {
            scaleX = .5f + .5f * progress
            scaleY = .5f + .5f * progress
            alpha = progress
          }
          .cardBackground()
      ) {
        SunriseCard(weatherEntity = weatherEntity, startAnim = !anim.isRunning && anim.value > 0)
      }
    }

    AnimateOnEnter(
      modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(1.68f)
        .padding(horizontal = 16.dp), animationSpec = spring(
        dampingRatio = .5f, stiffness = 200f
      )
    ) { progress, anim ->
      Box(
        modifier = Modifier
          .fillMaxSize()
          .pressable(delay = 0)
          .noRippleClickable {
            menuState.show {
              Column(
                modifier = Modifier
                  .fillMaxWidth()
                  .padding(horizontal = 16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
              ) {
                Text(
                  stringResource(R.string.air_quality_title),
                  style = MiuixTheme.textStyles.title3,
                  color = MiuixTheme.colorScheme.onSurface,
                  fontWeight = FontWeight.W600
                )
                weatherEntity.air2.indexes.forEach { indexe ->
                  Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                  ) {
                    Text(
                      indexe.name,
                      style = MiuixTheme.textStyles.body1,
                      color = MiuixTheme.colorScheme.onSurface,
                      modifier = Modifier.weight(1f)
                    )
                    Text(
                      indexe.category,
                      fontWeight = FontWeight.W600,
                      style = MiuixTheme.textStyles.body1,
                      color = COLOR_BLACK_100,
                      modifier = Modifier
                        .background(
                          color = Color(
                            red = indexe.color.red,
                            green = indexe.color.green,
                            blue = indexe.color.blue
                          ), shape = WIDGET_CARD_CORNER_SHAPE
                        )
                        .padding(
                          horizontal = 8.dp, vertical = 6.dp
                        )
                    )
                  }
                  Text(
                    indexe.health.advice.generalPopulation,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                  )
                  Text(
                    indexe.health.advice.sensitivePopulation,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                  )
                  Text(
                    indexe.health.effect,
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                  )
                }
                weatherEntity.air2.pollutants.forEach { pollutant ->
                  Box(
                    modifier = Modifier
                      .fillMaxWidth()
                      .background(
                        color = MiuixTheme.colorScheme.surfaceContainerHigh,
                        shape = WIDGET_CARD_CORNER_SHAPE
                      )
                      .padding(
                        horizontal = 16.dp, vertical = 8.dp
                      )
                  ) {
                    Text(
                      pollutant.name,
                      modifier = Modifier.align(Alignment.CenterStart),
                    )
                    Text(
                      "${pollutant.concentration.value}${pollutant.concentration.unit}",
                      modifier = Modifier.align(Alignment.CenterEnd),
                    )
                  }
                }
              }
            }
          }
          .graphicsLayer {
            scaleX = .5f + .5f * progress
            scaleY = .5f + .5f * progress
            alpha = progress
          }
          .cardBackground()
      ) {
        with(LocalSharedTransitionScope.current) {
          AqiCard(
            modifier = Modifier
              .sharedBounds(
                enter = fadeIn() + scaleIn(),
                exit = fadeOut(),
                animatedVisibilityScope = LocalAnimatedContentScope.current,
                sharedContentState = rememberSharedContentState(SharedContentKey.KEY_AIR_QUALITY),
                clipInOverlayDuringTransition = OverlayClip(
                  ContinuousRoundedRectangle(12.dp)
                ),
                resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds()
              )
              .fillMaxWidth()
              .noRippleClickable {
                menuState.show {
                  Text(
                    stringResource(R.string.air_quality_please_use_menu),
                    style = MiuixTheme.textStyles.body1,
                    color = MiuixTheme.colorScheme.onSurface
                  )
                }
              }, weatherData = weatherEntity, startAnim = !anim.isRunning && anim.value > 0
          )
        }
      }
    }
    Row(
      modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center
    ) {
      Icon(
        MiuixIcons.Other.GitHub,
        contentDescription = null,
        modifier = Modifier.size(16.dp),
        tint = MiuixTheme.colorScheme.onSurface
      )
      Text(
        "Project Source Code Hosted On Github",
        modifier = Modifier
          .padding(start = 12.dp)
          .alignByBaseline(),
        style = MiuixTheme.textStyles.footnote1.copy(color = MiuixTheme.colorScheme.onSurface)
      )
    }
    Spacer(
      modifier = Modifier.navigationBarsPadding()
    )
  }
}


@Composable
private fun EmptyPage(modifier: Modifier = Modifier) {
  val viewModel = koinActivityViewModel<WeatherViewModel>()

  Box(
    modifier = Modifier,
    contentAlignment = Alignment.Center
  ) {
    Text(
      stringResource(R.string.empty_state_no_data),
      modifier = Modifier.align(Alignment.Center),
      style = MiuixTheme.textStyles.title2,
      color = COLOR_WHITE_100
    )
  }

}

@Composable
private fun Modifier.cardBackground() = this.background(
  color = MiuixTheme.colorScheme.surfaceContainer,
  shape = WIDGET_CARD_CORNER_SHAPE
)
