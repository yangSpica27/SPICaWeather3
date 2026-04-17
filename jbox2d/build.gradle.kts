plugins {
    alias(libs.plugins.android.library)
}

android {
    namespace = "org.jbox2d"
    compileSdk = 37
    enableKotlin = false

    defaultConfig {
        // 保留 JBox2D 库的完整类名（防止主应用混淆破坏反射）
        consumerProguardFiles("consumer-rules.pro")
        minSdk = 31
    }

    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_11
        targetCompatibility = JavaVersion.VERSION_11
    }

    buildTypes {
        release {
            // 库模块不应处理混淆，由主应用统一管理
            isMinifyEnabled = false
        }
    }
}
