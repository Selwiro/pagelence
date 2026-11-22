package com.pagelence

/**
 * 每个壳自己的构建期信息。
 *
 * [ShellConfig] 是 app 传给 [:shell] 的唯一入口：版本号来自各 app 的
 * `BuildConfig`，完整词表开关来自各自的 Gradle 属性。这样 `:shell` 不再
 * 依赖某个 app 的 `BuildConfig`，两个壳可以各自决定是否开启下载入口。
 */
data class ShellConfig(
    val appVersionName: String,
    val appVersionCode: Int,
    /** 完整词表（可选下载）的切换与地址；空串／false 表示入口关闭。 */
    val fullDictUrl: String = "",
    val fullDictEnabled: Boolean = false,
)
