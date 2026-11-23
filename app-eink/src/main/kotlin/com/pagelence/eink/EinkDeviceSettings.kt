package com.pagelence.eink

import android.content.Context
import com.pagelence.presentation.eink.EinkRefreshPresets
import com.pagelence.presentation.eink.EinkRefreshSettings

/**
 * 只属于本机的墨屏设置：刷新强度。
 *
 * 不写进 `state.json`——刷新策略取决于具体设备的残影表现，不该跟着 WebDAV
 * 同步到另一块屏幕上去。存 SharedPreferences，随卸载清除。
 */
object EinkDeviceSettings {
    private const val FILE = "eink-settings"
    private const val KEY_REFRESH = "refreshStrength"
    private const val KEY_FORCE_MOAAN = "forceMoaanBroadcast"

    fun refresh(context: Context): EinkRefreshSettings {
        val prefs = context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        val index = prefs.getInt(KEY_REFRESH, 1)
        return EinkRefreshPresets.all.getOrNull(index) ?: EinkRefreshSettings.Default
    }

    /** 设置页的「强制发送墨案全刷广播」开关；默认关，按机型自动判断。 */
    fun forceMoaanBroadcast(context: Context): Boolean =
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .getBoolean(KEY_FORCE_MOAAN, false)

    fun saveForceMoaanBroadcast(context: Context, force: Boolean) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_FORCE_MOAAN, force)
            .apply()
    }

    fun saveRefresh(context: Context, settings: EinkRefreshSettings) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE)
            .edit()
            .putInt(KEY_REFRESH, EinkRefreshPresets.indexOf(settings))
            .apply()
    }
}
