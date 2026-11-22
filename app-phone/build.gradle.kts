// :app-phone —— 普通版壳：Activity、依赖装配、内容首次落盘。
// applicationId 固定为 com.pagelence；业务规则不写在这一层。
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 发布签名：密钥库在仓库外，口令经被 .gitignore 排除的 keystore.properties 注入。
// 该文件缺失时 release 仍可构建，只是产物不带签名（app-phone-release-unsigned.apk）；
// debug 构建完全不读这里，本地开发与 CI 不会因缺密钥而失败。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val releaseStoreFile = keystoreProperties.getProperty("storeFile")?.let { rootProject.file(it) }
val hasReleaseSigning = releaseStoreFile?.isFile == true
val signingNote = if (hasReleaseSigning) {
    "release 使用 $releaseStoreFile 签名"
} else {
    "未找到 keystore.properties 或密钥库，release 产物将不带签名"
}
logger.lifecycle(":app-phone 发布签名：$signingNote")

// 版本号单一来源：`version.properties`。发版用 tools/bump_version.py 递增，
// 构建脚本不再散落写死版本；文件缺失时退回 1/0.1.0，保证局部检出也能构建。
val versionProperties = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val appVersionCode = versionProperties.getProperty("versionCode")?.trim()?.toIntOrNull() ?: 1
val appVersionName = versionProperties.getProperty("versionName")?.trim().orEmpty().ifBlank { "0.1.0" }

android {
    namespace = "com.pagelence"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pagelence"
        minSdk = 26
        // 与 compileSdk 对齐。targetSdk 每落后一版，上架要求就会在未来某个节点
        // 逼着一次跳两级；edge-to-edge / 预测性返回等行为变更列入真机走查清单。
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
        // 完整词表是可选下载；托管上线前默认关闭，本地验收可用 -P 覆盖地址并打开。
        val fullDictUrl = (project.findProperty("pagelence.fullDictUrl") as String? ?: "").replace("\"", "")
        val fullDictEnabled = (project.findProperty("pagelence.fullDictEnabled") as String? ?: "false")
            .equals("true", ignoreCase = true)
        buildConfigField("String", "FULL_DICT_URL_OVERRIDE", "\"$fullDictUrl\"")
        buildConfigField("boolean", "FULL_DICT_ENABLED", "$fullDictEnabled")
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    buildFeatures {
        compose = true
        buildConfig = true
    }
    // 内容包随 APK 分发。给编辑看的缺口与审计报告放在仓库根 `reports/`，
    // 不在 assets 源目录里，所以不会进包。
    sourceSets.getByName("main").assets.srcDirs("../content")
    packaging {
        resources.excludes += setOf("/META-INF/{AL2.0,LGPL2.1}")
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = releaseStoreFile
                storePassword = keystoreProperties.getProperty("storePassword")
                keyAlias = keystoreProperties.getProperty("keyAlias")
                keyPassword = keystoreProperties.getProperty("keyPassword")
                // minSdk 26：v1（JAR）签名已无必要；v2 覆盖 Android 7+，v3 用于密钥轮换。
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        release {
            // 暂不开混淆与资源压缩：Compose 与 kotlinx.serialization 需要额外 keep 规则，
            // 等有发布渠道、有真机回归矩阵时再单独评估。
            isMinifyEnabled = false
            isShrinkResources = false
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":core"))
    implementation(project(":data"))
    implementation(project(":sync"))
    implementation(project(":ui-m3"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(libs.androidx.lifecycle.viewmodel.compose)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
    implementation(libs.compose.material3)
    implementation(libs.compose.material.icons.core)
}
