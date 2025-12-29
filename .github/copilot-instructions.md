# SPICaWeather3 - Copilot Instructions

## 项目概述
基于 Jetpack Compose 开发的 Android 天气应用（柠檬天气3），使用和风天气 API，采用现代 MVVM 架构模式。项目强调声明式 UI 和响应式数据流，集成了丰富的自定义天气动画效果。支持多城市管理、天气卡片拖拽排序、实时定位等功能。

## 架构模式

### 核心架构层级
```
UI层 (Compose) → ViewModel → Repository → Data Source (API/DB)
                                        ↓
                                 LocationHelper
                                 DataStoreUtil
```

**技术栈核心组件:**
- **UI**: Jetpack Compose (完全声明式,无 XML layouts)
- **DI**: Koin (在 `App.onCreate()` 初始化,所有模块注册在 `InjectModules.kt`)
- **Navigation**: Compose Navigation + Kotlinx Serialization (路由在 `route/Routes.kt` 用 `@Serializable` 标记)
- **Database**: Room + Flow (响应式数据流,主实体 `CityEntity`)
- **Network**: Retrofit + OkHttp + Sandwich (API 响应封装在 `ApiResponse<T>`)
- **状态管理**: StateFlow + DataStore (偏好设置)

### 依赖注入结构 (Koin)
所有注入配置集中在 `module/InjectModules.kt`:
- **`networkModule`**: OkHttpClient (超时配置), Retrofit, ApiService, ApiRepository
- **`viewModelModules`**: 使用 `viewModel { }` DSL 注册所有 ViewModel
- **`persistenceModule`**: Room Database, CityDao, PersistenceRepository, DataStoreUtil
- **`utilsModule`**: LocationHelper (百度定位)

**新增组件流程:**
1. 在对应模块中添加 Koin 注册代码
2. 在 `App.kt` 的 `modules()` 列表中引用(如已存在则跳过)
3. ViewModel 使用 `koinActivityViewModel<T>()` 或 `koinViewModel<T>()` 获取

## 关键开发约定

### Compose UI 开发规范

**1. 共享元素转场动画**  
页面间过渡使用 `SharedTransitionLayout` 和组合局部变量实现:
```kotlin
// 在 AppMain.kt 中全局提供
SharedTransitionLayout {
  CompositionLocalProvider(LocalSharedTransitionScope provides this@SharedTransitionLayout) {
    // NavHost...
  }
}

// 在各 Screen 中使用
with(LocalSharedTransitionScope.current) {
  Modifier.sharedElement(
    rememberSharedContentState(key = "unique-key"),
    animatedVisibilityScope = LocalAnimatedContentScope.current
  )
}
```
**关键点**: `key` 必须在源页面和目标页面保持一致。

**CompositionLocal 访问模式**:
- `LocalSharedTransitionScope` - 共享转场作用域 (来自 AppMain.kt)
- `LocalAnimatedContentScope` - 动画内容作用域 (在 NavHost composable 中提供)
- `LocalNavController` - 导航控制器 (全局路由，定义在 route/Routes.kt)
- `LocalMenuState` - 底部菜单状态管理

**使用方式**:
```kotlin
// 在 Screen composable 中获取 NavController
val navController = LocalNavController.current
navController.navigate(Routes.WeatherList)

// 访问共享转场作用域
with(LocalSharedTransitionScope.current) {
  Modifier.sharedElement(...)
}
```

**2. 自定义天气动画系统**  
基于 `common/WeatherAnimType.kt` 密封类配置,每个类型定义:
- 顶部/底部渐变色 (`topColor`, `bottomColor`)
- 动画元素开关 (`showGalaxy`, `showCloud`, `showRain`, `showSnow`, `showSun`)

实现位置: `ui/widget/WeatherBackground.kt`，使用 `Canvas` + `Shader` 绘制渐变  
动画组件包: `ui/widget/rain/`, `ui/widget/cloud/`, `ui/widget/sun/`, `ui/widget/galaxy/`

**Canvas 集成模式** (参考 `RainView.kt`):
```kotlin
Canvas(Modifier.fillMaxSize()) {
  drawIntoCanvas { canvas ->
    customDrawLogic(canvas.nativeCanvas) // 使用 Android 原生 Canvas API
  }
}
```

**性能优化 - 延迟渲染重型动画**:
使用 `ShowOnIdleContent` 包装复杂动画组件，在主线程空闲时才显示:
```kotlin
ShowOnIdleContent(
  visible = show,
  modifier = Modifier.fillMaxSize()
) {
  // 复杂动画组件 (如 HazeView, 使用 JBox2D 物理引擎)
}
```
实现位置: `ui/widget/ShowOnIdleContent.kt`，内部使用 `doOnMainThreadIdle()` 延迟 750ms。

**JBox2D 物理引擎集成** (用于烟雾/粒子动画):
- 在组件 `remember {}` 中创建物理世界和粒子系统
- 使用专用 `Executors.newFixedThreadPool(1)` + `asCoroutineDispatcher()` 处理物理计算
- **必须**在 `DisposableEffect` 中关闭线程池: `computeContext.close()`
- 示例: `ui/widget/haze/HazeView.kt`

```kotlin
// 正确的 JBox2D 集成模式
val computeContext = remember { 
  Executors.newFixedThreadPool(1).asCoroutineDispatcher() 
}
DisposableEffect(Unit) {
  onDispose { 
    computeContext.close()  // ← 防止内存泄漏
  }
}
```

**3. 屏幕适配 (AndroidAutoSize)**  
- 基准宽度: **375dp**
- 实现位置: `MainActivity` 实现 `CustomAdapt` 接口
- 所有尺寸自动按设备宽度缩放，无需手动适配

### 网络请求模式
使用 **Sandwich** 库封装 Retrofit 响应，提供类型安全的错误处理:
```kotlin
apiService.lookupCity(keyword)
  .onSuccess { data -> 
    // data 类型已推断，可直接访问 data.location
    onSucceed(data.location)
  }
  .onFailure { error -> 
    // error 提供 message() 方法
    onError(error.message())
  }
```
**重要约定**:
- 所有 API 方法返回 `ApiResponse<T>` 类型 (参考 `ApiService.kt`)
- Repository 层使用 `withContext(Dispatchers.IO)` 切换到 IO 线程
- 响应数据类使用 `@Keep` 注解防止混淆 (位于 `network/model/`)

### 数据流管理
**响应式数据流模式**:
```kotlin
// ViewModel 层
val cities: StateFlow<List<CityEntity>> = persistenceRepository
  .getAllCitiesFlow()  // Room Dao 返回 Flow<List<CityEntity>>
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

// UI 层
val cities by viewModel.cities.collectAsStateWithLifecycle()
```
**数据库更新自动传播**: Room 的 `@Query` + Flow 返回值会在数据库变更时自动发射新值，UI 无需手动刷新。

### DataStore 使用规范
集中管理在 `utils/DataStoreUtil.kt`:
- 所有 Key 定义为伴生对象常量 (如 `KEY_IS_FIRST_LAUNCH`, `KEY_WEATHER_CARDS_CONFIG`)
- 读取方法返回 `Flow<T>`，自动去重 (`distinctUntilChanged()`)
- 写入方法使用 `suspend` + `withContext(Dispatchers.IO)`
- **天气卡片配置**: 使用 Kotlinx Serialization 序列化为 JSON 保存，支持拖拽排序和显示/隐藏管理

```kotlin
// 读取
val isFirstLaunch = dataStoreUtil.getIsFirstLaunch().collectAsStateWithLifecycle(false).value

// 写入
dataStoreUtil.setIsFirstLaunch(true)

// 天气卡片配置管理
val cardsConfig = dataStoreUtil.getWeatherCardsConfig()
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
dataStoreUtil.updateCardVisibility(WeatherCardType.HOURLY, false)
```

### 天气卡片系统
**核心组件** (`common/WeatherCardType.kt`):
- `WeatherCardType` 枚举: 定义所有卡片类型（NOW, ALERT, HOURLY, DAILY, UV, AQI 等）
- `WeatherCardConfig`: 卡片配置数据类，包含类型、可见性、排序
- NOW 和 ALERT 卡片不可移除，其他卡片可自定义显示

**卡片管理方法**:
```kotlin
// 获取默认配置
WeatherCardType.getDefaultCards()

// 过滤可见/隐藏卡片
configs.getVisibleCards()
configs.getHiddenCards()

// 切换显示状态
dataStoreUtil.updateCardVisibility(cardType, isVisible)

// 更新排序
dataStoreUtil.updateCardsOrder(reorderedCards)
```
实现位置: `common/WeatherCardType.kt`  
使用示例: 参考项目根目录的 `WEATHER_CARD_REFACTOR_README.md`

## 项目特殊配置

### 构建配置
- **minSdk**: 31 (注意: README 显示为 33，但实际配置是 31)
- **targetSdk/compileSdk**: 36
- **Java版本**: 11
- **Gradle**: 8.13+
- **KSP**: 用于 Room 注解处理 (2.2.21-2.0.4)
- **签名配置**: debug/release 使用同一签名 (key.jks，密钥: SPICa27)
- **JBox2D 模块**: 独立子项目 (`jbox2d/`) 用于物理引擎，需在 settings.gradle.kts 包含

### ProGuard 混淆规则
关键保留规则 (在 `app/proguard-rules.pro`):
```proguard
# 保留网络层数据类 (防止 Gson 反序列化失败)
-keep class me.spica.spicaweather3.network.** { *;}
-keep class com.skydoves.sandwich.** { *;}

# 保留 Koin 注解
-keep class org.koin.core.annotation.** { *; }
-keep @org.koin.core.annotation.* class * { *; }

# 保留百度定位 SDK
-keep class com.baidu.location.** {*;}
```
**重要**: 所有网络响应模型必须添加 `@Keep` 注解 (位于 `network/model/`)

### 版本管理
使用 **Gradle Version Catalog** (`gradle/libs.versions.toml`) 统一管理依赖版本。
新增依赖时更新 TOML 文件，不要在 build.gradle.kts 中硬编码版本号。

示例:
```toml
[versions]
retrofit = "3.0.0"

[libraries]
retrofit = { module = "com.squareup.retrofit2:retrofit", version.ref = "retrofit" }
```
在 build.gradle.kts 中引用: `implementation(libs.retrofit)`

### 关键依赖
- **Compose BOM**: 统一 Compose 版本 (2025.11.00)
- **Haze**: 实现毛玻璃模糊效果 (`dev.chrisbanes.haze`)
- **MiuiX**: MIUI 风格组件库 (`top.yukonga.miuix.kmp.theme`)
- **Baidu Location**: 百度定位服务，需在 `App.onCreate()` 调用 `LocationClient.setAgreePrivacy(true)`
- **Sandwich**: Retrofit 响应封装 (`com.skydoves.sandwich`)
- **AndroidAutoSize**: 屏幕适配库 (`com.github.JessYanCoding:AndroidAutoSize`)

### 必须的初始化顺序
`App.onCreate()` 中:
1. **先** 调用 `LocationClient.setAgreePrivacy(true)` (百度定位要求)
2. **后** 调用 `startKoin { ... }` (初始化依赖注入)

## 开发工作流

### 构建与运行
```bash
# 构建 debug 版本
./gradlew assembleDebug

# 安装到设备
./gradlew installDebug

# 清理构建
./gradlew clean
```

**Windows 环境使用 gradlew.bat**

### 添加新功能页面
1. 在 `ui/` 下创建新包和 Screen 文件
2. 在 `route/Routes.kt` 添加路由定义 (使用 `@Serializable` 标记):
   ```kotlin
   @Serializable
   data object NewScreen
   // 或带参数: data class Detail(val id: String)
   ```
3. 在 `ui/AppMain.kt` 的 NavHost 中添加 `composable` 条目:
   ```kotlin
   composable<Routes.NewScreen>(
     enterTransition = { materialSharedAxisXIn() },  // 可选过渡动画
     exitTransition = { slideOutHorizontally() }
   ) {
     CompositionLocalProvider(LocalAnimatedContentScope provides this) {
       NewScreen()
     }
   }
   ```
4. 如需 ViewModel，在 `InjectModules.viewModelModules` 中注册:
   ```kotlin
   viewModel { NewScreenViewModel(get()) }
   ```
5. 页面导航使用:
   ```kotlin
   val navController = LocalNavController.current
   navController.navigate(Routes.NewScreen)
   ```

### 添加新 API 接口
1. 在 `network/ApiService.kt` 定义接口方法 (返回 `ApiResponse<T>`):
   ```kotlin
   @Headers("${HefengConfig.HEADER}:${HefengConfig.APIKEY}")
   @GET("/path")
   suspend fun newApi(@Query("param") param: String): ApiResponse<ResponseModel>
   ```
2. 在 `network/model/` 添加响应数据类 (使用 `@Keep` 注解):
   ```kotlin
   @Keep
   data class ResponseModel(val data: List<Item>)
   ```
3. 在 `ApiRepository.kt` 添加业务逻辑方法 (使用 `withContext(Dispatchers.IO)`):
   ```kotlin
   suspend fun fetchNewData(
     param: String,
     onError: (String?) -> Unit,
     onSucceed: (Data) -> Unit
   ) = withContext(Dispatchers.IO) {
     apiService.newApi(param)
       .onSuccess { onSucceed(data) }
       .onFailure { onError(message()) }
   }
   ```

## 文件结构重点

**包命名空间**: `me.spica.spicaweather3` (配置在 app/build.gradle.kts)

**源码结构** (`app/src/main/java/me/spica/spicaweather3/`):
- `common/`: 枚举和常量 (如 `WeatherAnimType` 密封类)
- `db/`: Room 数据库层 (entity, dao, repository)
  - `entity/`: 数据库表实体 (如 `CityEntity`)
  - `dao/`: 数据访问对象，返回 Flow 实现响应式
- `network/`: 网络层 (service, repository, model)
  - `model/`: API 响应数据类 (必须添加 `@Keep` 注解)
- `ui/`: Compose UI 层，按功能模块分包
  - `main/`: 主页面及 WeatherViewModel
  - `city_selector/`: 城市选择器
  - `weather_list/`: 天气列表
  - `air_quality/`: 空气质量详情
  - `widget/`: 可复用的自定义 Compose 组件
    - `rain/`, `cloud/`, `sun/`, `galaxy/`, `wind/`, `haze/`: 天气动画组件
- `utils/`: 工具类 (DataStoreUtil, LocationHelper, RainDropManager)
- `module/`: Koin 依赖注入模块配置 (InjectModules.kt)
- `route/`: 导航路由定义 (Routes.kt + LocalNavController)

**资源目录**:
- `app/src/main/res/`: Android 资源 (drawable, mipmap 等)
- `app/libs/`: 本地 AAR/JAR 依赖 (如百度定位 SDK)

## 代码风格
- Kotlin 优先，遵循官方命名规范
- Compose 函数使用 PascalCase
- 使用 `@Composable` 和 `@Preview` 注解
- Repository 方法使用 suspend 函数 + withContext(Dispatchers.IO)
- 网络响应模型使用 `@Keep` 注解防混淆
- 数据类优先使用 `data class`，密封类用于有限状态枚举

## 常见任务速查

### 添加新的 DataStore 配置项
```kotlin
// 在 DataStoreUtil.kt 的伴生对象中添加
val NEW_KEY = booleanPreferencesKey("new_key")

// 添加读写方法
fun getNewValue(): Flow<Boolean> = context.dataStore.data
  .map { it[NEW_KEY] ?: false }
  .distinctUntilChanged()

suspend fun setNewValue(value: Boolean) = withContext(Dispatchers.IO) {
  context.dataStore.edit { it[NEW_KEY] = value }
}
```

### 实现新的天气动画类型
```kotlin
// 1. 在 WeatherAnimType.kt 中添加新类型
object NewWeatherType : WeatherAnimType(
  key = "new-type",
  topColor = Color(0xFFXXXXXX),
  bottomColor = Color(0xFFXXXXXX),
  showCloud = true  // 根据需要配置
)

// 2. 在 ui/widget/ 创建动画组件 (如需要)
// 3. 在 WeatherBackground.kt 中添加渲染逻辑
```

### 调试网络请求
- 查看 OkHttp 日志: 已配置 `HttpLoggingInterceptor.Level.BODY`
- 超时配置在 `InjectModules.networkModule` 中 (连接 1.5s, 读写 3s)
- API 基础 URL: `https://n85egdbbrr.re.qweatherapi.com/`
- 私有天气服务: `http://106.54.25.152:4040/api/weather/all`

## 常见陷阱与最佳实践

### 内存泄漏防范
**线程池管理**:
```kotlin
// ✅ 正确: 使用 DisposableEffect 清理
val computeContext = remember { Executors.newFixedThreadPool(1).asCoroutineDispatcher() }
DisposableEffect(Unit) {
  onDispose { computeContext.close() }
}

// ❌ 错误: 忘记关闭线程池导致内存泄漏
```

### 百度定位 SDK 集成
**必须在 Koin 初始化之前**调用 `LocationClient.setAgreePrivacy(true)`:
```kotlin
// App.kt - initKoin() 方法
LocationClient.setAgreePrivacy(true)  // ← 必须在第一行
startKoin { ... }
```
原因: 百度 SDK 要求在首次使用前设置隐私政策同意状态。

**权限请求**: 在 AppMain.kt 使用 Accompanist Permissions 库请求定位权限:
```kotlin
val locationPermissionState = rememberMultiplePermissionsState(
  permissions = listOf(
    android.Manifest.permission.ACCESS_COARSE_LOCATION,
    android.Manifest.permission.ACCESS_FINE_LOCATION
  )
)
LaunchedEffect(isFirstLaunch) {
  if (!locationPermissionState.allPermissionsGranted) {
    locationPermissionState.launchMultiplePermissionRequest()
  }
}
```

### Compose 重组优化
- 使用 `remember` 缓存昂贵对象 (如物理引擎实例、Paint 对象)
- 使用 `derivedStateOf` 避免不必要的重组
- 复杂动画组件使用 `ShowOnIdleContent` 延迟加载
- 使用 `key()` 确保列表项稳定性 (参考 WeatherListScreen.kt)

### Flow 订阅模式
**ViewModel 中**:
```kotlin
// ✅ 使用 stateIn 转换为 StateFlow，设置合理的停止超时
val data = repository.getDataFlow()
  .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
```

**UI 层中**:
```kotlin
// ✅ 使用 collectAsStateWithLifecycle (自动管理生命周期)
val data by viewModel.data.collectAsStateWithLifecycle()

// ❌ 避免使用 collectAsState (不感知生命周期)
```

## 项目文档引用

### 重要文档
- **README.md**: 项目概述、技术栈说明、架构图
- **WEATHER_CARD_REFACTOR_README.md**: 天气卡片拖拽排序功能详细文档
- **gradle/libs.versions.toml**: 所有依赖版本的集中管理
- **app/proguard-rules.pro**: ProGuard 混淆规则

### API 文档
- **和风天气 API**: 主要天气数据来源 (https://n85egdbbrr.re.qweaterapi.com/)
- **私有天气服务**: 补充天气接口 (http://106.54.25.152:4040/api/weather/all)
- API 密钥配置在 `network/HefengConfig.kt`

### 关键实现参考
- **共享元素转场**: `ui/AppMain.kt` 中的 `SharedTransitionLayout` 集成
- **天气动画系统**: `ui/widget/WeatherBackground.kt` 渐变背景 + 各动画组件
- **卡片拖拽**: 参考 `WEATHER_CARD_REFACTOR_README.md` 实现指南
- **物理引擎动画**: `ui/widget/haze/HazeView.kt` JBox2D 集成示例
