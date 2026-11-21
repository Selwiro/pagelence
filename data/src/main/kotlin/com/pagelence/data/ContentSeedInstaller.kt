package com.pagelence.data

import java.io.File

/**
 * Installs the bundled content tree into app storage.
 *
 * The freshness decision is a caller-supplied install stamp: the Android shell
 * passes version + `lastUpdateTime`, tests pass any string. The copy itself is
 * a callback so this class stays pure JVM and can be tested without a Context.
 *
 * A failed copy keeps the previous `content/` directory and marker, so readers
 * are never left with a half-copied book set.
 */
object ContentSeedInstaller {
    private const val MARKER = "content-seed.txt"
    private const val STAGING = "content.new"
    private const val BACKUP = "content.old"

    data class Result(val content: File, val copied: Boolean)

    fun install(filesDir: File, stamp: String, copy: (target: File) -> Unit): Result {
        val target = File(filesDir, "content")
        val marker = File(filesDir, MARKER)
        val staging = File(filesDir, STAGING)
        val backup = File(filesDir, BACKUP)

        // Recover from an interrupted swap before deciding anything.
        if (!target.exists() && backup.isDirectory) backup.renameTo(target)
        backup.deleteRecursively()

        val currentStamp = runCatching { marker.readText().trim() }.getOrNull()
        if (target.isDirectory && currentStamp == stamp) return Result(target, copied = false)

        staging.deleteRecursively()
        return try {
            staging.mkdirs()
            copy(staging)
            swap(staging, target, backup)
            marker.writeText(stamp)
            Result(target, copied = true)
        } catch (error: Exception) {
            staging.deleteRecursively()
            if (backup.isDirectory) {
                target.deleteRecursively()
                backup.renameTo(target)
            }
            if (target.isDirectory) Result(target, copied = false) else throw error
        }
    }

    private fun swap(staging: File, target: File, backup: File) {
        backup.deleteRecursively()
        if (target.exists() && !target.renameTo(backup)) {
            target.copyRecursively(backup, overwrite = true)
            target.deleteRecursively()
        }
        if (!staging.renameTo(target)) {
            staging.copyRecursively(target, overwrite = true)
            staging.deleteRecursively()
        }
        backup.deleteRecursively()
    }
}
