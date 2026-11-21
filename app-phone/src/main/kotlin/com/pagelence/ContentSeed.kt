package com.pagelence

import android.content.Context
import android.os.Build
import com.pagelence.data.ContentSeedInstaller
import java.io.File

/**
 * Ships the content package inside the APK (assets come from `work/android/content`)
 * and copies it into app storage on first launch or after the app is updated.
 * `:core` stays platform-free by reading a plain directory.
 *
 * The freshness check is an install stamp, not a byte comparison: reading the
 * 13 MB asset tree on every cold start was both slow and incomplete (a new word
 * frequency table could be missed). `lastUpdateTime` changes on every install,
 * so the whole tree is re-seeded after `adb install -r` or a store update, and
 * skipped otherwise.
 */
object ContentSeed {
    fun ensure(context: Context): File =
        ContentSeedInstaller.install(context.filesDir, installStamp(context)) { staging ->
            copyTree(context, "", staging)
        }.content

    /** Install time + version; any app update re-seeds the whole content tree. */
    @Suppress("DEPRECATION")
    private fun installStamp(context: Context): String {
        val info = context.packageManager.getPackageInfo(context.packageName, 0)
        val code = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            info.longVersionCode
        } else {
            info.versionCode.toLong()
        }
        return "name=${info.versionName};code=$code;updated=${info.lastUpdateTime}"
    }

    private fun copyTree(context: Context, path: String, target: File) {
        val assets = context.assets
        val children = assets.list(path).orEmpty()
        for (child in children) {
            val childPath = if (path.isEmpty()) child else "$path/$child"
            val nested = assets.list(childPath).orEmpty()
            if (nested.isNotEmpty()) {
                val directory = File(target, child)
                directory.mkdirs()
                copyTree(context, childPath, directory)
            } else {
                val file = File(target, child)
                file.parentFile?.mkdirs()
                assets.open(childPath).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
