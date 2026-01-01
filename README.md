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
</p>

> [!TIP]
> 可以查看项目的其他版本：[Flutter 版本](https://github.com/yangSpica27/spica_weather_flutter) | [Android View 版本](https://github.com/yangSpica27/SpicaWeather2)

## 🛠️ 技术栈

### 核心框架
- **Jetpack Compose** - 声明式 UI 工具包
- **Kotlin 2.2.21** - 项目开发语言
- **Material 3** - Material Design 3 组件

### 架构组件
- **Koin** - 轻量级依赖注入框架
- **Room** - 本地数据库，配合 Flow 实现响应式数据流
- **ViewModel** - MVVM 架构的视图模型层
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

```
┌─────────────────────────────────────────┐
│              UI Layer (Compose)         │
│  ┌──────────┐  ┌──────────┐  ┌────────┐│
│  │  Main    │  │  City    │  │  Air   ││
│  │  Screen  │  │ Selector │  │Quality ││
│  └──────────┘  └──────────┘  └────────┘│
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│           ViewModel Layer               │
│  ┌──────────────────────────────────┐   │
│  │  StateFlow + LiveData            │   │
│  └──────────────────────────────────┘   │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│          Repository Layer               │
│  ┌────────────┐      ┌──────────────┐   │
│  │    API     │      │  Database    │   │
│  │ Repository │      │  Repository  │   │
│  └────────────┘      └──────────────┘   │
└───────────────────┬─────────────────────┘
                    │
┌───────────────────▼─────────────────────┐
│          Data Source Layer              │
│  ┌────────────┐      ┌──────────────┐   │
│  │   Retrofit │      │     Room     │   │
│  │  API Service      │   Database   │   │
│  └────────────┘      └──────────────┘   │
└─────────────────────────────────────────┘
```

## 📦 项目结构

```
app/src/main/java/me/spica/spicaweather3/
├── common/          # 通用类和枚举
│   └── WeatherAnimType.kt    # 天气动画类型定义
├── db/              # 数据库层
│   ├── entity/      # Room 实体类
│   ├── dao/         # 数据访问对象
│   └── PersistenceRepository.kt
├── network/         # 网络层
│   ├── model/       # API 响应模型
│   ├── ApiService.kt
│   └── ApiRepository.kt
├── ui/              # UI 层
│   ├── main/        # 主页面
│   ├── city_selector/    # 城市选择
│   ├── weather_list/     # 天气列表
│   ├── air_quality/      # 空气质量
│   └── widget/      # 自定义组件
│       ├── rain/    # 雨动画
│       ├── cloud/   # 云动画
│       ├── sun/     # 太阳动画
│       └── galaxy/  # 星空动画
├── utils/           # 工具类
│   ├── DataStoreUtil.kt
│   ├── LocationHelper.kt
│   └── RainDropManager.kt
├── module/          # Koin 依赖注入模块
├── route/           # 导航路由
└── App.kt           # Application 类
```

## 🚀 快速开始

### 环境要求
- Android Studio Ladybug 或更高版本
- JDK 11 或更高版本
- Android SDK 33+
- Gradle 8.13+


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

- [和风天气](https://www.qweather.com/) - 提供稳定可靠的天气数据服务
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