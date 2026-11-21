// 页有所成 · Android 构建入口。
//
// :core 与 :data 为纯 JVM；:ui-m3 与 :app-phone 是 Android 模块。
// 仓库策略：默认直连 Google Maven、Maven Central、Gradle Plugin Portal。
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "pagelence"
include(":core")
include(":data")
include(":ui-m3")
include(":app-phone")
