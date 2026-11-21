// :core —— 内容模型、解析、进度规则、生词卡、词级词频与偏好。
// 不依赖 Android：可直接跑纯 JVM 单测。
plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    // 单测直接读取仓库内已迁移的内容，验证"迁移产物可被 :core 读取"。
    systemProperty("pagelence.contentDir", layout.projectDirectory.dir("../content").asFile.absolutePath)
    testLogging {
        events("passed", "failed", "skipped")
    }
}
