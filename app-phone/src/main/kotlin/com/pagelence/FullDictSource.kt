package com.pagelence

/**
 * Where the published full ECDICT table lives.
 *
 * Hosting is live on GitHub Releases (tag `dict-39cfb96`); `gradle.properties`
 * turns the settings entry on by default. A build can still close the entry or
 * point the URL elsewhere:
 *
 * `./gradlew :app-phone:assembleDebug -Ppagelence.fullDictEnabled=true \\
 *   -Ppagelence.fullDictUrl=http://10.0.2.2:9000/full-dict.sqlite`
 */
object FullDictSource {
    /**
     * Published artifact on GitHub Releases, tag per dictionary version so the
     * URL never points at a moving file. The manifest compiled into the APK
     * pins the sha256, and the Release answer 302s to a signed CDN URL that
     * supports `Range` (verified 2026-11-22).
     */
    const val URL =
        "https://github.com/Selwiro/pagelence/releases/download/dict-39cfb96/full-dict.sqlite"

    val url: String get() = BuildConfig.FULL_DICT_URL_OVERRIDE.ifBlank { URL }

    val enabled: Boolean get() = BuildConfig.FULL_DICT_ENABLED
}
