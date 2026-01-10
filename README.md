<h1 align="center">柠檬天气3</h1>

<p align="center">  
🌤️ 基于 Jetpack Compose 开发的现代化 Android 天气应用
</p>

<p align="center">
  <img src="https://img.shields.io/badge/Compose-2025.11.00-brightgreen" alt="Compose">
  <img src="https://img.shields.io/badge/Kotlin-2.2.21-blue" alt="Kotlin">
  <img src="https://img.shields.io/badge/minSdk-33-orange" alt="minSdk">
  <img src="https://img.shields.io/badge/License-MIT-yellow" alt="License">
</p>

## ✨ 特性亮点

- 🎨 **完全采用 Jetpack Compose** - 纯声明式 UI，无 XML layouts
- 🌈 **精美的天气动画** - 雨、雪、云、太阳等多种自定义天气动画效果
- 🔄 **共享元素转场** - 流畅的页面切换动画体验
- 🏗️ **现代化架构** - MVVM + Koin + Flow + Room 响应式架构
- 🎭 **MIUI 风格设计** - 采用 MiuiX 组件库，提供原生般的体验
- 🌐 **实时天气数据** - 接入和风天气 API，提供准确的气象信息
- 📍 **智能定位** - 百度定位服务，自动获取当前位置天气

## 📸 应用截图

<p>
<img src="/pic/Screenshot_20251227_194828.png" width="20%"/>
<img src="/pic/Screenshot_20251118_171558.png" width="20%"/>
<img src="/pic/Screenshot_20251118_171608.png" width="20%"/>
<img src="/pic/Screenshot_20260102_150406.png" width="20%"/>
<img src="/pic/Screenshot_20260102_150432.png" width="20%"/>
</p>

> [!TIP]
> 可以查看项目的其他版本：[Flutter 版本](https://github.com/yangSpica27/spica_weather_flutter) | [Android View 版本](https://github.com/yangSpica27/SpicaWeather2)

## 🛠️ 技术栈

### 核心框架
- **Jetpack Compose** - 声明式 UI 工具包
- **Kotlin 2.2.21** - 项目开发语言
- **Material 3** - Material Design 3 组件

### 架构组件
- **Clean Architecture** - 清晰的分层架构（presentation → domain → data）
- **Koin** - 轻量级依赖注入框架，模块化配置
- **Room** - 本地数据库，配合 Flow 实现响应式数据流
- **ViewModel** - MVVM 架构的视图模型层
- **UseCase** - 业务逻辑封装层，实现关注点分离
- **DataStore** - 替代 SharedPreferences 的数据持久化方案
- **Navigation Compose** - Jetpack Compose 导航组件

### 网络层
- **Retrofit** - RESTful API 客户端
- **OkHttp** - HTTP 客户端和拦截器
- **Sandwich** - Retrofit 响应封装库
- **Gson** - JSON 序列化/反序列化

### UI 增强
- **MiuiX** - MIUI 风格组件库
- **AndroidLiquidGlass** - 毛玻璃模糊效果
- **AndroidAutoSize** - 屏幕适配解决方案
- **Accompanist** - Compose 辅助库（权限、系统 UI 控制器）

### 其他
- **Baidu Location SDK** - 百度定位服务
- **JBox2D** - 2D 物理引擎（用于高级动画效果）
- **KSP** - Kotlin Symbol Processing（Room 注解处理）

## 🏗️ 项目架构

采用 **Clean Architecture** 分层架构，确保代码的可维护性、可测试性和可扩展性：

```
┌─────────────────────────────────────────────────────────┐
│           Presentation Layer (UI + ViewModel)           │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Jetpack Compose UI                              │   │
│  │  ┌────────┐  ┌──────────┐  ┌──────────────────┐ │   │
│  │  │ Main   │  │  City    │  │  Weather List    │ │   │
│  │  │ Screen │  │ Selector │  │  Air Quality     │ │   │
│  │  └────────┘  └──────────┘  └──────────────────┘ │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  ViewModels (StateFlow/LiveData)                │   │
│  │  WeatherViewModel | CitySelectorViewModel       │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │ 依赖
┌────────────────────────▼────────────────────────────────┐
│                  Domain Layer (业务逻辑)                 │
│  ┌──────────────────────────────────────────────────┐   │
│  │  UseCases (业务用例)                             │   │
│  │  • GetAllCitiesUseCase                          │   │
│  │  • RefreshWeatherUseCase                        │   │
│  │  • SearchCityUseCase                            │   │
│  │  • ManageCitiesUseCase                          │   │
│  │  • LocationUseCase                              │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Repository Interfaces (接口定义)                │   │
│  │  IWeatherRepository | ICityRepository           │   │
│  └──────────────────────────────────────────────────┘   │
└────────────────────────┬────────────────────────────────┘
                         │ 实现
┌────────────────────────▼────────────────────────────────┐
│                   Data Layer (数据层)                    │
│  ┌──────────────────────────────────────────────────┐   │
│  │  Repository Implementations                      │   │
│  │  WeatherRepositoryImpl | CityRepositoryImpl     │   │
│  └──────────────────────────────────────────────────┘   │
│  ┌───────────────────┐      ┌───────────────────────┐   │
│  │  Remote Data      │      │   Local Data          │   │
│  │  • ApiService     │      │   • Room Database     │   │
│  │  • Retrofit       │      │   • CityDao           │   │
│  │  • API Models     │      │   • DataStore         │   │
│  └───────────────────┘      └───────────────────────┘   │
└─────────────────────────────────────────────────────────┘

                    ┌───────────────────┐
                    │   Core Layer      │
                    │  • Config         │
                    │  • DI Modules     │
                    │  • Constants      │
                    └───────────────────┘
```

**架构优势：**
- ✅ **单向依赖流**：外层依赖内层，内层不依赖外层
- ✅ **业务逻辑隔离**：UseCase 封装业务规则，易于测试和复用
- ✅ **数据源抽象**：Repository 接口隔离数据层实现细节
- ✅ **关注点分离**：UI、业务逻辑、数据操作各司其职

## 📦 项目结构

```
app/src/main/java/me/spica/spicaweather3/
├── core/                # 核心层
│   ├── config/          # 应用配置（API Key 等）
│   ├── constants/       # 常量定义
│   └── di/              # 依赖注入模块
│       ├── NetworkModule.kt      # 网络层 DI
│       ├── DatabaseModule.kt     # 数据库层 DI
│       ├── ViewModelModule.kt    # ViewModel 和 UseCase DI
│       └── UtilsModule.kt        # 工具类 DI
├── domain/              # 领域层（业务逻辑）
│   ├── repository/      # Repository 接口定义
│   │   ├── IWeatherRepository.kt
│   │   └── ICityRepository.kt
│   └── usecase/         # 业务用例
│       ├── GetAllCitiesUseCase.kt
│       ├── RefreshWeatherUseCase.kt
│       ├── SearchCityUseCase.kt
│       ├── ManageCitiesUseCase.kt
│       └── LocationUseCase.kt
├── data/                # 数据层
│   ├── local/           # 本地数据源
│   │   └── db/          # Room 数据库
│   │       ├── entity/  # 数据库实体
│   │       ├── dao/     # 数据访问对象
│   │       └── AppDatabase.kt
│   ├── remote/          # 远程数据源
│   │   └── api/         # Retrofit API
│   │       ├── model/   # API 响应模型
│   │       └── ApiService.kt
│   └── repository/      # Repository 实现
│       ├── WeatherRepositoryImpl.kt
│       └── CityRepositoryImpl.kt
├── ui/                  # 表现层（UI）
│   ├── main/            # 主页面
│   │   └── WeatherViewModel.kt
│   ├── city_selector/   # 城市选择
│   │   └── CitySelectorViewModel.kt
│   ├── weather_list/    # 天气列表
│   ├── air_quality/     # 空气质量
│   ├── app_widget/      # 桌面小组件
│   ├── widget/          # 自定义 UI 组件
│   │   ├── rain/        # 雨动画
│   │   ├── cloud/       # 云动画
│   │   ├── sun/         # 太阳动画
│   │   ├── galaxy/      # 星空动画
│   │   └── haze/        # 雾霾动画（JBox2D）
│   └── theme/           # 主题配置
├── common/              # 通用工具
│   ├── type/            # 类型定义
│   │   └── WeatherAnimType.kt
│   ├── model/           # 数据模型
│   └── result/          # 结果封装
├── utils/               # 工具类
│   ├── DataStoreUtil.kt
│   ├── LocationHelper.kt
│   └── blur/            # 自定义模糊效果
├── route/               # 导航路由
└── App.kt               # Application 类
```

**目录说明：**
- **core/** - 核心配置和依赖注入，不依赖其他层
- **domain/** - 业务逻辑层，定义 UseCase 和 Repository 接口
- **data/** - 数据层，实现 Repository 接口，管理数据源
- **ui/** - 表现层，包含 Compose UI 和 ViewModel

## 🚀 快速开始

### 环境要求
- Android Studio Ladybug 或更高版本
- JDK 17 或更高版本
- Android SDK 33+
- Gradle 8.13+

### 配置步骤

#### 1. 获取和风天气 API Key

本项目使用和风天气 API 获取天气数据，你需要：

1. 访问 [和风天气开发平台](https://dev.qweather.com/)
2. 注册并登录账号
3. 创建应用获取 **API Key** 和 **API ID**

#### 2. 配置 local.properties

在项目根目录下创建或编辑 `local.properties` 文件，添加以下内容：

```properties
# Android SDK 路径（通常由 Android Studio 自动生成）
sdk.dir=/path/to/your/android/sdk

# 和风天气 API 配置
HEFENG_API_KEY=your_api_key_here
HEFENG_API_ID=your_api_id_here
```

#### 3. 构建运行

配置完成后，即可构建并运行项目：

```bash
./gradlew assembleDebug
./gradlew installDebug
```

或直接在 Android Studio 中点击运行按钮。


## 🌟 核心功能

- ✅ 实时天气查询
- ✅ 多城市管理
- ✅ 自动定位
- ✅ 空气质量指数
- ✅ 24小时天气预报
- ✅ 15天天气趋势
- ✅ 生活指数建议
- ✅ 天气动画效果
- ✅ 深色模式支持
- ✅ 拖拽排序卡片
- ✅ 桌面小组件


## 📄 开源协议

本项目基于 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

## 🙏 致谢

- [和风天气](https://www.qweather.com/) - 提供稳定可靠的天气数据服务（[如何配置 API Key](#2-配置-localproperties)）
- [Jetpack Compose](https://developer.android.com/jetpack/compose) - Google 官方现代 UI 工具包
- [MiuiX](https://github.com/miuix-kotlin-multiplatform/miuix) - MIUI 风格组件库
- 所有为本项目做出贡献的开发者

## 📮 联系方式

如有问题或建议，欢迎通过以下方式联系：

- 提交 [Issue](https://github.com/yangSpica27/SPICaWeather3/issues)
---

<p align="center">
  ⭐ 如果这个项目对你有帮助，请给一个 Star！⭐
</p>


## 📄 开源协议

本项目基于 MIT 协议开源，详见 [LICENSE](LICENSE) 文件。

```
MIT License

Copyright (c) 2024 SPICa27

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.

THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
SOFTWARE.
```