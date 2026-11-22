package com.pagelence

/**
 * 完整 ECDICT 词表的出厂地址。
 *
 * 托管在 GitHub Releases（tag `dict-39cfb96`），按词典版本固定 URL，永远不指向
 * 会变的文件；清单里的 sha256 钉在 APK 内，Release 会 302 到支持 `Range` 的
 * 签名 CDN 地址（2026-11-22 实测）。
 *
 * 开关与覆盖地址来自各壳的 [ShellConfig]（Gradle 属性
 * `pagelence.fullDictEnabled` / `pagelence.fullDictUrl`），`:shell` 本身不读
 * 任何 app 的 `BuildConfig`。
 */
object FullDictSource {
    const val DEFAULT_URL =
        "https://github.com/Selwiro/pagelence/releases/download/dict-39cfb96/full-dict.sqlite"
}

/** 实际下载地址：构建期覆盖优先，否则用出厂地址。 */
val ShellConfig.fullDictEntryUrl: String
    get() = fullDictUrl.ifBlank { FullDictSource.DEFAULT_URL }
