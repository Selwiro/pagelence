package com.pagelence.sync

import com.pagelence.core.progress.ReadingState
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption

/** A consistent view of the local document; [revision] guards against races. */
data class LocalSnapshot(
    val json: String,
    val revision: Long,
)

/**
 * Local storage as the sync engine sees it.
 *
 * [replaceIfUnchanged] is the important one: a sync must never overwrite a
 * save the reader made between GET and write-back. The store compares
 * [LocalSnapshot.revision] and refuses when it moved; the engine then restarts
 * the round instead of clobbering the new card or page turn.
 */
interface LocalState {
    /** Current document; callers must not mutate it. */
    fun current(): ReadingState

    fun snapshot(): LocalSnapshot

    /** Replace only when nothing changed since [snapshot]; true on success. */
    fun replaceIfUnchanged(snapshot: LocalSnapshot, state: ReadingState): Boolean

    /** Unconditional replace, used by local import after an explicit merge. */
    fun replace(state: ReadingState)
}

/** Temp-file + fsync-free atomic rename; never leaves a half-written file. */
internal object AtomicFiles {
    fun write(path: Path, text: String) {
        val parent = path.parent
        if (parent != null) Files.createDirectories(parent)
        val temp = path.resolveSibling("${path.fileName}.tmp")
        Files.write(temp, text.toByteArray(Charsets.UTF_8))
        try {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING)
        }
    }

    fun read(path: Path): String? {
        // java.io on purpose: Files.readString is a Java 11 API that Android's
        // java.nio.file does not implement, where it would throw and silently
        // look like a missing file (every sync then sees "no baseline").
        val file = path.toFile()
        if (!file.isFile) return null
        return runCatching { file.readText(Charsets.UTF_8) }.getOrNull()
    }

    fun delete(path: Path) {
        runCatching { Files.deleteIfExists(path) }
    }
}
