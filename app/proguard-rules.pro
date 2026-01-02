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
# Keep annotation definitions
-keep class org.koin.core.annotation.** { *; }
# Keep classes annotated with Koin annotations
-keep @org.koin.core.annotation.* class * { *; }
-keep class com.baidu.location.** {*;}
-keep class me.spica.spicaweather3.common.** { *;}
-keep class com.kyant.** { *;}

# Kotlin优化
-dontwarn kotlin.**
-dontwarn kotlinx.**
-keepclassmembers class **$WhenMappings {
    <fields>;
}

# Compose优化
-keep class androidx.compose.** { *; }
-dontwarn androidx.compose.**