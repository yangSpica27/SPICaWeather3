import org.gradle.kotlin.dsl.implementation

plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.jetbrains.kotlin.serialization)
  alias(libs.plugins.ksp)
}

android {
  namespace = "me.spica.spicaweather3"
  compileSdk = 36



  defaultConfig {
    applicationId = "me.spica.spicaweather3"
    minSdk = 31
    targetSdk = 36
    versionCode = 2
    versionName = "1.0_preview2"

    testInstrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
    ndk {
      //armeabi armeabi-v7a arm64-v8a x86 x86_64
      abiFilters.add("arm64-v8a")
    }
  }

  signingConfigs {
    register("signingConfig") {
      storePassword = "SPICa27"
      keyAlias = "wuqi"
      keyPassword = "SPICa27"
      storeFile = rootProject.file("key.jks")
    }
  }

  buildTypes {
    release {
      isMinifyEnabled = true
      isShrinkResources = true
      proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
      signingConfig = signingConfigs.getByName("signingConfig")
      ndk {
        debugSymbolLevel = "SYMBOL_TABLE"
      }
    }
    debug {
      isMinifyEnabled = false
      signingConfig = signingConfigs.getByName("signingConfig")
    }
  }
  compileOptions {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
  }
  kotlinOptions {
    jvmTarget = "11"
    freeCompilerArgs = listOf(
      "-XXLanguage:+PropertyParamAnnotationDefaultTargetMode",
      "-opt-in=kotlin.RequiresOptIn"
    )
  }
  buildFeatures {
    compose = true
  }
  
  packaging {
    resources {
      excludes += setOf(
        "META-INF/DEPENDENCIES",
        "META-INF/LICENSE",
        "META-INF/LICENSE.txt",
        "META-INF/license.txt",
        "META-INF/NOTICE",
        "META-INF/NOTICE.txt",
        "META-INF/notice.txt",
        "META-INF/*.kotlin_module",
        "META-INF/AL2.0",
        "META-INF/LGPL2.1"
      )
    }
  }
}

dependencies {
  implementation(fileTree(mapOf("dir" to "libs", "include" to listOf("*.aar"))))
  implementation(project(":jbox2d"))
  implementation(libs.androidx.core.ktx)
  implementation(libs.androidx.lifecycle.runtime.ktx)
  implementation(libs.androidx.activity.compose)
  implementation(platform(libs.androidx.compose.bom))
  implementation(libs.androidx.compose.ui)
  implementation(libs.androidx.compose.ui.graphics)
  implementation(libs.androidx.compose.ui.tooling.preview)
  implementation(libs.androidx.compose.material3)
  implementation(libs.kotlinx.serialization.core)
  implementation(libs.androidx.adaptive)
  implementation(libs.androidx.adaptive.layout)
  implementation(libs.androidx.adaptive.navigation)
  testImplementation(libs.junit)
  androidTestImplementation(libs.androidx.junit)
  androidTestImplementation(libs.androidx.espresso.core)
  androidTestImplementation(platform(libs.androidx.compose.bom))
  androidTestImplementation(libs.androidx.compose.ui.test.junit4)
  debugImplementation(libs.androidx.compose.ui.tooling)
  debugImplementation(libs.androidx.compose.ui.test.manifest)
  implementation(libs.androidx.compose.material.icons.core)
  implementation(libs.koin.androidx.compose)
  // Jetpack Compose integration
  implementation(libs.androidx.navigation.compose)
  implementation(libs.androidx.datastore.preferences)
  implementation(libs.androidx.dynamicanimation.ktx)
  implementation(libs.miuix.android)
//  implementation(libs.miuix.android.icon)
  implementation(libs.backdrop)
  implementation(libs.capsule)
//  implementation(libs.haze.materials)
  implementation(libs.accompanist.systemuicontroller)
  // define a BOM and its version
  implementation(platform(libs.okhttp.bom))
  // define any required OkHttp artifacts without version
//  implementation(libs.haze)
  implementation(libs.okhttp)
  implementation(libs.logging.interceptor)
  implementation(libs.retrofit)
  implementation(libs.sandwich.retrofit)
  implementation(libs.converter.gson)
  implementation(libs.gson)
  implementation(libs.androidx.room.runtime)
  ksp(libs.androidx.room.compiler)
  implementation(libs.androidx.room.ktx)
  implementation(libs.reorderable)
  implementation(libs.androidautosize)
  implementation(libs.baidumapsdk.location)
  val accompanistVersion = "0.37.3"
  implementation(libs.accompanist.permissions)
  implementation(libs.androidx.glance)
  implementation(libs.androidx.glance.appwidget)
}