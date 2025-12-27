pluginManagement {
  repositories {
    google {
      content {
        includeGroupByRegex("com\\.android.*")
        includeGroupByRegex("com\\.google.*")
        includeGroupByRegex("androidx.*")
      }
    }
    mavenCentral()
    gradlePluginPortal()
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
  }
}
dependencyResolutionManagement {
  repositories {
    mavenCentral()
    maven(url = "https://jitpack.io")
    maven(url = "https://maven.aliyun.com/repository/central")
    maven(url = "https://maven.aliyun.com/repository/google")
    maven(url = "https://maven.aliyun.com/repository/public")
    maven(url = "https://maven.aliyun.com/repository/gradle-plugin")
    google()
  }
}

rootProject.name = "SPICaWeather3"
include(":app")
include(":jbox2d")
 