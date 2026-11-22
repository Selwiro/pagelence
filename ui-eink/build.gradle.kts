// :ui-eink —— 墨水屏版的页面与组件：只依赖 Compose foundation，不引入 material3、
// 图标库与动效库。设计规则见 docs/app-eink.md：纯黑白 + 灰阶、无阴影、无动画、
// 翻页不滚动、触控 ≥48dp、正文衬线 ≥18sp。
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.compose)
}

android {
    namespace = "com.pagelence.uieink"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
    }
}

dependencies {
    api(project(":core"))
    api(project(":presentation"))
    implementation(libs.androidx.core.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
}
