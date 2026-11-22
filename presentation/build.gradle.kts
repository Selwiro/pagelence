// :presentation —— 与 Android 无关的呈现层：UI 模型、阅读会话与页面流。
// 普通版（ui-m3 + app-phone）和将来的墨水屏壳共用；只依赖 :core 与 :sync。
plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}

dependencies {
    api(project(":core"))
    api(project(":sync"))

    testImplementation(kotlin("test"))
    testImplementation(libs.junit.jupiter)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
    systemProperty("pagelence.contentDir", layout.projectDirectory.dir("../content").asFile.absolutePath)
    testLogging {
        events("passed", "failed", "skipped")
    }
}
