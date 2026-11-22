// :shell —— 两个壳共用的 Android 侧编排层：依赖装配、阅读会话 ViewModel、
// 平台适配（TTS、Keystore、Uri、网络、SQLite）。两个 app 只做主题、导航与入口，
// 业务规则仍在 :core / :presentation，这里不放规则。
plugins {
    alias(libs.plugins.android.library)
}

android {
    // 资源命名空间与 Kotlin 包名分开：:shell 没有资源，命名空间只是清单合并的标识，
    // 不能与 :app-phone 的 com.pagelence 相同。
    namespace = "com.pagelence.shell"
    compileSdk = 37
    buildToolsVersion = "36.0.0"

    defaultConfig {
        minSdk = 26
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

dependencies {
    api(project(":core"))
    api(project(":data"))
    api(project(":sync"))
    api(project(":presentation"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
}
