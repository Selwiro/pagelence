package com.pagelence

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import com.pagelence.core.prefs.ThemeMode

/**
 * Mirror the in-app theme choice into Android's application night mode.
 *
 * The starting window is painted by the system from the manifest theme before
 * Compose can read `state.json`, so a reader who chose Dark on a light system
 * would otherwise get a white flash on every cold start. `setApplicationNightMode`
 * is API 31+; older releases keep the system-driven splash.
 */
internal fun applyApplicationNightMode(context: Context, theme: ThemeMode) {
    if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
    val manager = context.getSystemService(UiModeManager::class.java) ?: return
    val mode = when (theme) {
        ThemeMode.SYSTEM -> UiModeManager.MODE_NIGHT_AUTO
        ThemeMode.LIGHT -> UiModeManager.MODE_NIGHT_NO
        ThemeMode.DARK -> UiModeManager.MODE_NIGHT_YES
    }
    manager.setApplicationNightMode(mode)
}
