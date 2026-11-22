package com.pagelence

import android.app.Application

class PagelenceApplication : Application() {
    /**
     * 版本与完整词表开关都在这里从 `BuildConfig` 读一次，交给共享的 `:shell`；
     * 墨屏壳用同一套装配，只是它自己的 `BuildConfig` 值。
     */
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
