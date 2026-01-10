# Add project specific ProGuard rules here.
# You can control the set of applied configuration files using the
# proguardFiles setting in build.gradle.
#
# For more details, see
#   http://developer.android.com/guide/developing/tools/proguard.html

# If your project uses WebView with JS, uncomment the following
# and specify the fully qualified class name to the JavaScript interface
# class:
#-keepclassmembers class fqcn.of.javascript.interface.for.webview {
#   public *;
#}

# Uncomment this to preserve the line number information for
# debugging stack traces.
-keepattributes SourceFile,LineNumberTable

# If you keep the line number information, uncomment this to
# hide the original source file name.
#-renamesourcefileattribute SourceFile

# 优化配置
-optimizationpasses 5
-dontusemixedcaseclassnames
-dontskipnonpubliclibraryclasses
-verbose

# 移除日志
-assumenosideeffects class android.util.Log {
    public static *** v(...);
    public static *** d(...);
    public static *** i(...);
    public static *** w(...);
}

-keep,allowobfuscation,allowshrinking interface com.skydoves.sandwich.ApiResponse
-keep class me.spica.spicaweather3.network.** { *;}
-keep class com.skydoves.sandwich.** { *;}
-keep,allowobfuscation,allowshrinking interface com.skydoves.sandwich.ApiResponse

# Koin - 保留所有 ViewModel 和依赖注入相关类
-keep class org.koin.core.annotation.** { *; }
-keep @org.koin.core.annotation.* class * { *; }
-keep class org.koin.** { *; }
-keepnames class org.koin.** { *; }

# 保留所有 ViewModel 类（防止 Koin 实例化失败）
-keep class * extends androidx.lifecycle.ViewModel { *; }
-keep class me.spica.spicaweather3.ui.**.*ViewModel { *; }

# 保留所有被 Koin 注入的类（Repository, Helper, Util 等）
-keep class me.spica.spicaweather3.db.** { *; }
-keep class me.spica.spicaweather3.utils.** { *; }

# 保留百度定位
-keep class com.baidu.location.** {*;}

# 保留通用模型和常量
-keep class me.spica.spicaweather3.common.** { *;}

# 保留第三方 UI 库
-keep class org.jbox2d.** { *;}

# Kotlin 优化
-dontwarn kotlin.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Kotlin 协程

# Kotlin Serialization
-keepattributes *Annotation*, InnerClasses
-dontnote kotlinx.serialization.AnnotationsKt
-keepclassmembers class kotlinx.serialization.json.** {
    *** Companion;
}
-keepclasseswithmembers class kotlinx.serialization.json.** {
    kotlinx.serialization.KSerializer serializer(...);
}
-keep,includedescriptorclasses class me.spica.spicaweather3.**$$serializer { *; }
-keepclassmembers class me.spica.spicaweather3.** {
    *** Companion;
}
-keepclasseswithmembers class me.spica.spicaweather3.** {
    kotlinx.serialization.KSerializer serializer(...);
}

# Retrofit 和 OkHttp
-keepattributes Signature, InnerClasses, EnclosingMethod
-keepattributes RuntimeVisibleAnnotations, RuntimeVisibleParameterAnnotations
-keepattributes AnnotationDefault
-keepclassmembers,allowshrinking,allowobfuscation interface * {
    @retrofit2.http.* <methods>;
}
-dontwarn org.codehaus.mojo.animal_sniffer.IgnoreJRERequirement
-dontwarn javax.annotation.**
-dontwarn kotlin.Unit
-dontwarn retrofit2.KotlinExtensions
-dontwarn retrofit2.KotlinExtensions$*
-if interface * { @retrofit2.http.* <methods>; }
-keep class me.spica.spicaweather3.data.** { *;}
-keep,allowobfuscation interface <1>
-dontwarn kotlinx.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}
