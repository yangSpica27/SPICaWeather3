package me.spica.spicaweather3.ui.weather_list


import androidx.compose.animation.Animatable
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Search
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.kyant.capsule.ContinuousRoundedRectangle
import dev.chrisbanes.haze.HazeProgressive
import dev.chrisbanes.haze.hazeEffect
import dev.chrisbanes.haze.hazeSource
import dev.chrisbanes.haze.materials.HazeMaterials
import dev.chrisbanes.haze.rememberHazeState
import me.spica.spicaweather3.R
import me.spica.spicaweather3.common.type.WeatherAnimType
import me.spica.spicaweather3.db.entity.CityEntity
import me.spica.spicaweather3.route.LocalNavController
import me.spica.spicaweather3.route.Routes
import me.spica.spicaweather3.ui.LocalAnimatedContentScope
import me.spica.spicaweather3.ui.LocalSharedTransitionScope
import me.spica.spicaweather3.ui.main.WeatherViewModel
import me.spica.spicaweather3.ui.main.weather.WeatherPageState
import me.spica.spicaweather3.ui.widget.MainTopBar
import me.spica.spicaweather3.ui.widget.particle.ThanosDisintegrateContainer
import me.spica.spicaweather3.utils.noRippleClickable
import org.koin.compose.viewmodel.koinActivityViewModel
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState
import top.yukonga.miuix.kmp.basic.ButtonDefaults
import top.yukonga.miuix.kmp.basic.Icon
import top.yukonga.miuix.kmp.basic.IconButton
import top.yukonga.miuix.kmp.basic.InfiniteProgressIndicator
import top.yukonga.miuix.kmp.basic.MiuixScrollBehavior
import top.yukonga.miuix.kmp.basic.Scaffold
import top.yukonga.miuix.kmp.basic.Text
import top.yukonga.miuix.kmp.basic.TextButton
import top.yukonga.miuix.kmp.extra.SuperDialog
import top.yukonga.miuix.kmp.theme.MiuixTheme
import top.yukonga.miuix.kmp.utils.overScrollVertical
import kotlin.random.Random


/**
 * 城市管理页面
 * 展示已添加的城市列表，支持拖拽排序和删除操作
 */
@OptIn(ExperimentalSharedTransitionApi::class)
@Composable
fun WeatherListScreen() {

  val navController = LocalNavController.current

  // 滚动行为控制器，用于处理顶栏的折叠和展开
  val scrollBehavior = MiuixScrollBehavior()

  val viewModel = koinActivityViewModel<WeatherViewModel>()

  // 从 ViewModel 获取已跟踪的城市列表
  val trackedCities = viewModel.weatherPageStates.collectAsStateWithLifecycle().value

  // 临时列表，用于拖拽排序时避免频繁更新数据库
  var tempList by remember { mutableStateOf(listOf<WeatherPageState>()) }

  // 拖拽状态标记
  var isDrag by remember { mutableStateOf(false) }

  // 删除确认对话框显示状态
  var showDialog = remember { mutableStateOf(false) }

  // 当前选中要删除的城市
  var selectedCity by remember { mutableStateOf<CityEntity?>(null) }

  // 正在消散的城市ID（用于灭霸效果）
  var disintegratingCityId by remember { mutableStateOf<Long?>(null) }

  // 监听城市列表变化，非拖拽状态时同步到临时列表
  LaunchedEffect(trackedCities) {
    if (!isDrag) {
      tempList = trackedCities
    }
  }

  val listHazeState = rememberHazeState()

  Scaffold(
    topBar = {
      MainTopBar(
        modifier = Modifier.hazeEffect(
          state = listHazeState,
          style = HazeMaterials.ultraThin(
            MiuixTheme.colorScheme.surface
          )
        ){
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
                  .noRippleClickable {
                    navController.navigate(Routes.CitySelect)
                  }
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
                  stringResource(R.string.weather_list_search_placeholder), style = MiuixTheme.textStyles.subtitle,
                  color = MiuixTheme.colorScheme.onSurface,
                  fontSize = 17.sp
                )
              }
            }
          }

        },
        navigationIcon = {
          IconButton(
            onClick = {
              navController.popBackStack()
            }
          ) {
            Icon(
              Icons.AutoMirrored.Default.ArrowBack,
              contentDescription = stringResource(R.string.cd_back)
            )
          }
        }
      )
    }
  ) {

    // 删除城市确认对话框
    SuperDialog(
      title = stringResource(R.string.dialog_title_notice),
      summary = stringResource(R.string.weather_list_delete_city_message, selectedCity?.name.orEmpty()),
      show = showDialog,
      onDismissRequest = { showDialog.value = false }
    ) {
      TextButton(
        text = stringResource(R.string.action_confirm),
        onClick = {
          selectedCity?.let { c ->
            // 触发灭霸消散效果
            disintegratingCityId = c.id.hashCode().toLong()
          }
          showDialog.value = false
        },
        colors = ButtonDefaults.textButtonColorsPrimary(),
        modifier = Modifier.fillMaxWidth()
      )
    }

    // 触觉反馈控制器
    val hapticFeedback = LocalHapticFeedback.current

    val listState = remember { androidx.compose.foundation.lazy.LazyListState() }

    // 可拖拽排序的列表状态，限制首个 item 不参与拖拽
    val reorderableLazyListState = rememberReorderableLazyListState(listState) { from, to ->
      // 如果涉及到索引 0（首个城市），则不允许交换
      if (from.index == 0 || to.index == 0) {
        return@rememberReorderableLazyListState
      }

      val city1 = tempList[from.index].cityEntity
      val city2 = tempList[to.index].cityEntity
      // 在数据库中交换两个城市的排序
      viewModel.swapSort(
        city1 = city1,
        city2 = city2
      )
      // 更新临时列表的顺序
      tempList = tempList.toMutableList().apply {
        add(to.index, removeAt(from.index))
      }
      // 触发触觉反馈
      hapticFeedback.performHapticFeedback(HapticFeedbackType.SegmentFrequentTick)
    }

    LazyColumn(
      state = listState,
      modifier =
        Modifier
          .hazeSource(listHazeState)
          .fillMaxSize()
          .padding(it)
          .nestedScroll(scrollBehavior.nestedScrollConnection)
          .overScrollVertical(),
      contentPadding = PaddingValues(top = 12.dp, bottom = 48.dp),
      overscrollEffect = null
    ) {
      itemsIndexed(tempList, key = { index, c ->
        c.cityEntity.id
      }) { index, item ->

        val appearAnim =
          remember(item.cityEntity.id) { androidx.compose.animation.core.Animatable(0f) }
        val delayMillis = remember(item.cityEntity.id) { Random.nextInt(0, 200) }
        val durationMillis = remember(item.cityEntity.id) { Random.nextInt(260, 520) }

        LaunchedEffect(item.cityEntity.id) {
          appearAnim.snapTo(0f)
          appearAnim.animateTo(
            targetValue = 1f,
            animationSpec = tween(
              durationMillis = durationMillis,
              delayMillis = delayMillis
            )
          )
        }

        val progress = appearAnim.value

        with(LocalSharedTransitionScope.current) {

          ReorderableItem(
            reorderableLazyListState,
            key = item.cityEntity.id,
            // 首个 item（index 0）禁用拖拽，其他 item 可拖拽
            enabled = index != 0
          ) { isDragging ->

            // 拖拽时的阴影动画
            val elevation by animateDpAsState(if (isDragging) 1.dp else 2.dp)

            // 拖拽时的缩放动画
            val scale by animateFloatAsState(if (isDragging) 1.02f else 1.0f)

            // iOS 风格的抖动动画 - 当有任何 item 被拖拽且当前 item 未被拖拽时启用
            val infiniteTransition = rememberInfiniteTransition(label = "shake")
            val shakeOffset by infiniteTransition.animateFloat(
              initialValue = -1f,
              targetValue = 1f,
              animationSpec = infiniteRepeatable(
                animation = tween(80, easing = LinearEasing),
                repeatMode = RepeatMode.Reverse
              ),
              label = "shake_offset"
            )

            // 判断是否应该显示抖动效果：有拖拽发生且当前 item 未被拖拽
            val shouldShake = isDrag && !isDragging

            // 判断当前卡片是否正在消散
            val isThisCardDisintegrating = disintegratingCityId == item.cityEntity.id.hashCode().toLong()

            // 消散完成后标记，用于隐藏卡片防止闪烁
            var hasDisintegrated by remember { mutableStateOf(false) }

            ThanosDisintegrateContainer(
              isDisintegrating = isThisCardDisintegrating,
              onDisintegrationComplete = {
                // 先标记已消散，隐藏卡片
                hasDisintegrated = true
                // 消散完成后执行实际删除
                selectedCity?.let { city ->
                  viewModel.deleteCity(city)
                }
                disintegratingCityId = null
                selectedCity = null
              },
              modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                  scaleX = scale * progress
                  scaleY = scale * progress
                  translationY = (1f - progress) * 36.dp.toPx()
                  // 消散完成后设置透明度为0，避免列表动画导致的闪烁
                  alpha = if (hasDisintegrated) 0f else 1f
                  // 应用细微的抖动效果
                  if (shouldShake && index != 0) {
                    rotationZ = shakeOffset * 0.5f  // 轻微旋转
                    translationX = shakeOffset * 1.5f  // 轻微水平偏移
                  }
                }
                .padding(
                  top = if (index != 0) 12.dp else 0.dp
                )
                .padding(horizontal = 22.dp)
                // 对于已消散的项，禁用动画以避免闪烁
                .then(
                  if (hasDisintegrated) Modifier else Modifier.animateItem()
                )
            ) {
              WeatherItem(
                modifier = Modifier
                  .fillMaxSize()
                  // 点击显示删除对话框
                  .noRippleClickable {
                    if (!item.cityEntity.isUserLoc && disintegratingCityId == null) {
                      selectedCity = item.cityEntity
                      showDialog.value = true
                    }
                  }
                  // 长按拖拽排序（首个 item 禁用拖拽）
                  .longPressDraggableHandle(enabled = index != 0, onDragStarted = {
                    isDrag = true
                  }, onDragStopped = {
                    isDrag = false
                  })
                  .shadow(
                    elevation = elevation,
                    shape = ContinuousRoundedRectangle(12.dp),
                  )
                  .clip(
                    ContinuousRoundedRectangle(12.dp)
                  ),
                cityData = item,
              )
            }

          }
        }
      }
    }
  }
}

/**
 * 天气卡片项组件
 * 展示单个城市的天气信息，包括城市名、地区和当前温度
 *
 * @param modifier 修饰符
 * @param cityData 城市天气数据状态
 * @param initColor 初始背景颜色
 */
@Composable
fun WeatherItem(
  modifier: Modifier = Modifier,
  cityData: WeatherPageState,
  initColor: Color = MiuixTheme.colorScheme.onSurface
) {
  // 卡片背景色，会根据天气类型动画变化
  val cardColor = remember { Animatable(initColor) }

  // 从天气状态中提取城市实体（使用 remember 避免重复计算）
  val cityEntity = remember(cityData) { cityData.cityEntity }

  val isUserLoc = remember(cityEntity) { cityEntity.isUserLoc }

  // 根据天气数据动画更新卡片背景色
  LaunchedEffect(cityData) {
    if (cityData is WeatherPageState.Data) {
      val iconId = cityEntity.weather?.current?.icon ?: "100"
      cardColor.animateTo(WeatherAnimType.getAnimType(iconId).topColor)
    } else {
      cardColor.animateTo(initColor)
    }
  }
  Row(
    modifier = modifier
      .background(cardColor.value)
      .padding(
        horizontal = 16.dp, vertical = 12.dp
      ),
    verticalAlignment = Alignment.CenterVertically,
    horizontalArrangement = Arrangement.spacedBy(12.dp)
  ) {
    Column(
      modifier = Modifier
        .weight(1f),
      verticalArrangement = Arrangement.spacedBy(8.dp),
      horizontalAlignment = Alignment.Start,
    ) {
      // 城市名称
      Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.CenterVertically
      ) {
        Text(
          cityEntity.name,
          color = MiuixTheme.colorScheme.surface,
          textAlign = TextAlign.Start,
          style = MiuixTheme.textStyles.title2,
          fontWeight = FontWeight.SemiBold,
          fontSize = 22.sp
        )
        if (isUserLoc){
          Icon(
            painter = painterResource(R.drawable.ic_location),
            contentDescription = null,
            tint = MiuixTheme.colorScheme.surface,
            modifier = Modifier.size(18.dp)
          )
        }
      }
      // 省份和市区信息
      Text(
        "${cityEntity.adm1},${cityEntity.adm2}",
        color = MiuixTheme.colorScheme.surface,
        textAlign = TextAlign.Start,
        style = MiuixTheme.textStyles.body2,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp
      )
    }
    // 温度或加载动画，根据数据状态切换
    AnimatedContent(
      targetState = cityData,
      label = "city_state",
      transitionSpec = { fadeIn() togetherWith fadeOut() }
    ) { cityData ->
      Box(
        modifier = Modifier.fillMaxHeight(),
        contentAlignment = Alignment.Center
      ) {
        when (cityData) {
          // 有数据时显示温度
          is WeatherPageState.Data -> {
            Text(
              buildAnnotatedString {
                // 温度数字部分（大字体）
                withStyle(style = SpanStyle(fontSize = 40.sp, fontWeight = FontWeight.Light)) {
                  append("${cityData.cityEntity.weather?.current?.temperature}")
                }
                // 摄氏度符号（小字体）
                withStyle(style = SpanStyle(fontSize = 32.sp,fontWeight = FontWeight.Light)) {
                  append("℃")
                }
              },
              color = MiuixTheme.colorScheme.surface,
              style = MiuixTheme.textStyles.main.copy(
                fontSize = 32.sp
              ),
              fontWeight = FontWeight.Light,
              textAlign = TextAlign.Center,
              modifier = Modifier
                .fillMaxHeight(),
            )
          }

          // 加载中显示进度指示器
          else -> {
            InfiniteProgressIndicator(
              color = MiuixTheme.colorScheme.surface,
              modifier = Modifier.size(40.dp),
            )
          }
        }
      }
    }
  }
}


//@Composable
//fun WeatherItem(modifier: Modifier = Modifier) {
//  LaunchedEffect(cityData) {
//    if (cityData is WeatherPageState.Data) {
//      color.animateTo(
//        WeatherAnimType
//          .getAnimType(
//            cityData.cityEntity
//              .weather?.todayWeather?.iconId.toString()
//          ).topColor
//      )
//    } else {
//      color.animateTo(COLOR_BLACK_100)
//    }
//  }
//
//  Box(
//    modifier = Modifier
//      .padding(vertical = 8.dp, horizontal = 16.dp)
//      .fillMaxWidth()
//      .animateItem()
//      .sharedBounds(
//        animatedVisibilityScope = LocalAnimatedContentScope.current,
//        sharedContentState = rememberSharedContentState(cityData.cityEntity.id),
//        resizeMode = SharedTransitionScope.ResizeMode.scaleToBounds(contentScale = ContentScale.FillHeight),
//        clipInOverlayDuringTransition = OverlayClip(
//          ContinuousRoundedRectangle(12.dp)
//        )
//      ),
//  ) {
//    Row(
//      modifier = Modifier
//        .fillMaxSize()
//        .background(
//          color.value,
//          ContinuousRoundedRectangle(12.dp)
//        )
//        .padding(
//          horizontal = 22.dp,
//          vertical = 16.dp
//        ),
//      verticalAlignment = Alignment.CenterVertically
//    ) {
//      Column(
//        modifier = Modifier
//          .weight(1f)
//          .fillMaxHeight(),
//        verticalArrangement = Arrangement.spacedBy(8.dp),
//        horizontalAlignment = Alignment.Start,
//      ) {
//        Text(
//          cityData.cityEntity.name,
//          color = MiuixTheme.colorScheme.surface,
//          textAlign = TextAlign.Center,
//          style = MiuixTheme.textStyles.title2,
//          fontWeight = FontWeight.W600,
//          fontSize = 24.sp
//        )
//        Text(
//          "${cityData.cityEntity.adm1},${cityData.cityEntity.adm2}",
//          color = MiuixTheme.colorScheme.surface,
//          textAlign = TextAlign.Center,
//          style = MiuixTheme.textStyles.body2,
//          fontWeight = FontWeight.W500,
//          fontSize = 18.sp
//        )
//      }
//      AnimatedContent(
//        targetState = cityData,
//        label = "city_state",
//      ) { cityData ->
//        Box(
//          modifier = Modifier.fillMaxHeight(),
//          contentAlignment = Alignment.Center
//        ) {
//          when (cityData) {
//            is WeatherPageState.Data -> {
//              Text(
//                "${cityData.cityEntity.weather?.todayWeather?.temp}℃",
//                color = MiuixTheme.colorScheme.surface,
//                style = MiuixTheme.textStyles.main.copy(
//                  fontSize = 32.sp
//                ),
//                fontWeight = FontWeight.W900,
//                textAlign = TextAlign.Center,
//                modifier = Modifier
//                  .fillMaxHeight(),
//              )
//            }
//
//            else -> {
//              InfiniteProgressIndicator(
//                color = MiuixTheme.colorScheme.surface,
//                modifier = Modifier.size(40.dp),
//              )
//            }
//          }
//        }
//      }
//    }
//  }
//}