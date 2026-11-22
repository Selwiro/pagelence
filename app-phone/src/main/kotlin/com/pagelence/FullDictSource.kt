package com.pagelence

/**
 * Where the published full ECDICT table lives.
 *
 * As with the default book source, hosting is not live yet, so [enabled] is
 * false by release default and the settings row explains that instead of
 * offering a download that would 404. A local verification build can point the
 * URL elsewhere and turn the entry on:
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
