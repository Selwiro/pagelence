package com.pagelence.eink

import android.app.Application
import com.pagelence.AppGraph
import com.pagelence.ShellConfig

class EinkApplication : Application() {
    /** 与普通版同一套装配，只有版本号与完整词表开关来自本壳的 `BuildConfig`。 */
    val graph: AppGraph by lazy {
        AppGraph(
            this,
            ShellConfig(
                appVersionName = BuildConfig.VERSION_NAME,
                appVersionCode = BuildConfig.VERSION_CODE,
                fullDictUrl = BuildConfig.FULL_DICT_URL_OVERRIDE,
                fullDictEnabled = BuildConfig.FULL_DICT_ENABLED,
            ),
        )
    }
}
