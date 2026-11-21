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
    /** Published artifact path; the manifest in the APK pins its sha256. */
    const val URL = "https://raw.githubusercontent.com/pagelence/books/main/dict/full-dict.sqlite"

    val url: String get() = BuildConfig.FULL_DICT_URL_OVERRIDE.ifBlank { URL }

    val enabled: Boolean get() = BuildConfig.FULL_DICT_ENABLED
}
