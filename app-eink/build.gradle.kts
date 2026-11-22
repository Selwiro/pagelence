// :app-eink —— 墨水屏壳：applicationId com.pagelence.eink。
// 只做主题、导航与依赖装配；阅读会话与平台适配来自共享的 :shell，
// 页面来自 :ui-eink（不依赖 material3）。业务规则仍全部在 :core。
import java.util.Properties

plugins {
    alias(libs.plugins.android.application)
    alias(libs.plugins.kotlin.compose)
}

// 与普通版共用同一把发布签名（密钥库在仓库外，口令经 keystore.properties 注入）；
// 缺文件时 release 仍可构建，只是产物不带签名。
val keystoreProperties = Properties().apply {
    val file = rootProject.file("keystore.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val releaseStoreFile = keystoreProperties.getProperty("storeFile")?.let { rootProject.file(it) }
val hasReleaseSigning = releaseStoreFile?.isFile == true

// 版本号单一来源：`version.properties`（与普通版同一份）。
val versionProperties = Properties().apply {
    val file = rootProject.file("version.properties")
    if (file.isFile) file.inputStream().use { load(it) }
}
val appVersionCode = versionProperties.getProperty("versionCode")?.trim()?.toIntOrNull() ?: 1
val appVersionName = versionProperties.getProperty("versionName")?.trim().orEmpty().ifBlank { "0.1.0" }

android {
    namespace = "com.pagelence.eink"
    compileSdk = 37

    defaultConfig {
        applicationId = "com.pagelence.eink"
        minSdk = 26
        targetSdk = 37
        versionCode = appVersionCode
        versionName = appVersionName
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
    // 与普通版读同一份随包内容；三份大 JSON 仍只以 lexicon.bin 形式进包。
    sourceSets.getByName("main").assets.srcDirs("../content")
    androidResources {
        ignoreAssetsPattern = "ecdict.json:word-levels.json:word-frequencies.json"
    }
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
                enableV1Signing = false
                enableV2Signing = true
                enableV3Signing = true
            }
        }
    }
    buildTypes {
        release {
            // R8 与 keep 规则与普通版一致；墨屏壳没有 material3，混淆后更小。
            isMinifyEnabled = true
            isShrinkResources = true
            proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt"), "proguard-rules.pro")
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
}

dependencies {
    implementation(project(":shell"))
    implementation(project(":ui-eink"))
    implementation(project(":core"))
    implementation(project(":presentation"))
    implementation(project(":data"))
    implementation(project(":sync"))
    implementation(libs.androidx.core.ktx)
    implementation(libs.androidx.activity.compose)
    implementation(libs.androidx.lifecycle.viewmodel.ktx)
    implementation(platform(libs.compose.bom))
    implementation(libs.compose.ui)
    implementation(libs.compose.foundation)
}
