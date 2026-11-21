package com.pagelence.core.dict

import com.pagelence.core.content.PagelenceJson
import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.nio.file.StandardOpenOption
import java.time.Instant

/**
 * The network seam for a resumable download. The app implements it with
 * `HttpURLConnection` plus a `Range` header; tests use in-memory streams.
 *
 * [open] must send `Range: bytes=<offset>-`. A server that ignores it answers
 * `200` with the whole body; the installer then restarts from zero.
 */
interface DictTransport {
    fun open(url: String, offset: Long): DictStream
}

interface DictStream : AutoCloseable {
    /** HTTP status: 200, 206 or an error code. */
    val code: Int

    /** Bytes of the body; `-1` when unknown. */
    fun read(buffer: ByteArray): Int

    override fun close()
}

sealed interface DictInstallResult {
    data object Installed : DictInstallResult

    /** The reader paused; the partial file is kept for a resume. */
    data object Cancelled : DictInstallResult

    data class Failed(
        /** Non-null when the bytes arrived but were rejected. */
        val rejection: DictRejection? = null,
        val reason: String? = null,
    ) : DictInstallResult
}

/**
 * Downloads, verifies and atomically installs the optional full ECDICT table.
 *
 * Layout inside [dir] (`filesDir/dictionaries/`):
 * - `full-dict.sqlite` — the installed table, replaced atomically;
 * - `full-dict.sqlite.part` — the in-flight download, kept on pause/failure;
 * - `full-dict.part.json` — which manifest the partial belongs to;
 * - `full-dict.json` — the installed manifest fingerprint.
 *
 * Nothing here lives under the seeded `content/` tree, so an app update
 * reseeds the books without touching the reader's downloaded table.
 */
class FullDictInstaller(
    private val dir: Path,
    private val transport: DictTransport,
    private val now: () -> String = { Instant.now().toString() },
) {
    private val lock = Any()

    @Volatile
    private var cancelled = false

    val databaseFile: Path get() = dir.resolve(DATABASE)
    private val partialFile: Path get() = dir.resolve("$DATABASE.part")
    private val partialMetaFile: Path get() = dir.resolve("full-dict.part.json")
    private val installedMetaFile: Path get() = dir.resolve("full-dict.json")

    /** Version of the table on disk, or null when nothing is installed. */
    fun installedVersion(): String? {
        val meta = readMeta(installedMetaFile) ?: return null
        return meta.dictVersion.takeIf { Files.isRegularFile(databaseFile) }
    }

    fun isInstalled(manifest: DictManifest): Boolean {
        if (!Files.isRegularFile(databaseFile)) return false
        val meta = readMeta(installedMetaFile)
        if (meta != null) {
            return meta.dictVersion == manifest.dictVersion &&
                meta.sha256.equals(manifest.sha256, ignoreCase = true) &&
                meta.bytes == manifest.bytes &&
                runCatching { Files.size(databaseFile) == manifest.bytes }.getOrDefault(false)
        }
        // The app can die between the atomic rename and the metadata write. The
        // table is still there, so verify it once and rebuild the metadata
        // instead of downloading 80 MB again.
        val size = runCatching { Files.size(databaseFile) }.getOrNull() ?: return false
        if (size != manifest.bytes) return false
        if (DictRules.verify(databaseFile, manifest) != null) return false
        writeMeta(installedMetaFile, metaOf(manifest, manifest.bytes).copy(installedAt = now()))
        return true
    }

    /** Bytes already on disk for the in-flight download. */
    fun downloadedBytes(): Long =
        if (Files.isRegularFile(partialFile)) runCatching { Files.size(partialFile) }.getOrDefault(0) else 0

    /** True when the kept partial belongs to [manifest] and can be continued. */
    fun canResume(manifest: DictManifest): Boolean {
        val meta = readMeta(partialMetaFile) ?: return false
        return meta.dictVersion == manifest.dictVersion &&
            meta.sha256.equals(manifest.sha256, ignoreCase = true) &&
            downloadedBytes() in 1 until manifest.bytes
    }

    fun cancel() {
        cancelled = true
    }

    /**
     * Download or resume, verify, then replace the installed table.
     *
     * [onProgress] receives `(downloaded, total)` and is called once before the
     * first chunk so a resumed download starts at the right percentage.
     */
    fun install(
        manifest: DictManifest,
        url: String,
        onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> },
    ): DictInstallResult {
        synchronized(lock) {
            return installLocked(manifest, url, onProgress)
        }
    }

    private fun installLocked(
        manifest: DictManifest,
        url: String,
        onProgress: (downloaded: Long, total: Long) -> Unit,
    ): DictInstallResult {
        cancelled = false
        Files.createDirectories(dir)
        val resumable = canResume(manifest)
        if (!resumable) {
            delete(partialFile)
            delete(partialMetaFile)
        }
        var offset = if (resumable) downloadedBytes() else 0L
        if (offset > manifest.bytes) {
            delete(partialFile)
            offset = 0
        }
        writeMeta(partialMetaFile, metaOf(manifest, offset))
        onProgress(offset, manifest.bytes)

        if (offset < manifest.bytes) {
            val result = runCatching { transfer(manifest, url, offset, onProgress) }
                .getOrElse { error -> DictInstallResult.Failed(reason = error.message ?: "network") }
            if (result != DictInstallResult.Installed) return result
        }
        return finish(manifest)
    }

    private fun transfer(
        manifest: DictManifest,
        url: String,
        startOffset: Long,
        onProgress: (Long, Long) -> Unit,
    ): DictInstallResult {
        var offset = startOffset
        transport.open(url, offset).use { stream ->
            if (cancelled) return DictInstallResult.Cancelled
            when {
                stream.code == 416 -> return DictInstallResult.Failed(reason = "range not satisfiable")
                stream.code == 200 -> {
                    // The server ignored the Range header; start over.
                    if (offset > 0) {
                        delete(partialFile)
                        offset = 0
                        onProgress(0, manifest.bytes)
                    }
                }

                stream.code == 206 -> Unit
                stream.code !in 200..299 -> return DictInstallResult.Failed(reason = "http ${stream.code}")
            }
            Files.newOutputStream(
                partialFile,
                StandardOpenOption.CREATE,
                StandardOpenOption.WRITE,
                StandardOpenOption.APPEND,
            ).use { output ->
                val buffer = ByteArray(CHUNK)
                while (true) {
                    if (cancelled) return DictInstallResult.Cancelled
                    val read = stream.read(buffer)
                    if (read < 0) break
                    offset += read
                    if (offset > manifest.bytes) {
                        delete(partialFile)
                        return DictInstallResult.Failed(reason = "larger than the manifest")
                    }
                    output.write(buffer, 0, read)
                    onProgress(offset, manifest.bytes)
                }
            }
        }
        return DictInstallResult.Installed
    }

    private fun finish(manifest: DictManifest): DictInstallResult {
        if (!Files.isRegularFile(partialFile)) {
            return DictInstallResult.Failed(reason = "no data")
        }
        val rejection = DictRules.verify(partialFile, manifest)
        if (rejection != null) {
            delete(partialFile)
            delete(partialMetaFile)
            return DictInstallResult.Failed(rejection = rejection)
        }
        runCatching {
            Files.move(
                partialFile,
                databaseFile,
                StandardCopyOption.REPLACE_EXISTING,
                StandardCopyOption.ATOMIC_MOVE,
            )
        }.getOrElse {
            Files.move(partialFile, databaseFile, StandardCopyOption.REPLACE_EXISTING)
        }
        writeMeta(
            installedMetaFile,
            metaOf(manifest, manifest.bytes).copy(installedAt = now()),
        )
        delete(partialMetaFile)
        return DictInstallResult.Installed
    }

    /** Delete the installed table; the app falls back to the shipped core table. */
    fun remove(): Boolean {
        synchronized(lock) {
            val existed = Files.isRegularFile(databaseFile)
            delete(databaseFile)
            delete(installedMetaFile)
            delete(partialFile)
            delete(partialMetaFile)
            return existed
        }
    }

    private fun metaOf(manifest: DictManifest, downloaded: Long): DictFileMeta = DictFileMeta(
        dictVersion = manifest.dictVersion,
        schemaVersion = manifest.schemaVersion,
        sha256 = manifest.sha256,
        bytes = manifest.bytes,
        downloadedBytes = downloaded,
    )

    private fun readMeta(path: Path): DictFileMeta? {
        if (!Files.isRegularFile(path)) return null
        return runCatching {
            // `Files.readString` is API 33+; minSdk is 26, so read bytes.
            val json = Files.readAllBytes(path).toString(Charsets.UTF_8)
            PagelenceJson.decodeFromString<DictFileMeta>(json)
        }.getOrNull()
    }

    private fun writeMeta(path: Path, meta: DictFileMeta) {
        // Same reason: `Files.writeString` is API 33+.
        runCatching {
            Files.write(
                path,
                PagelenceJson.encodeToString(DictFileMeta.serializer(), meta).toByteArray(Charsets.UTF_8),
            )
        }
    }

    private fun delete(path: Path) {
        runCatching { Files.deleteIfExists(path) }
    }

    private companion object {
        const val DATABASE = "full-dict.sqlite"
        const val CHUNK = 64 * 1024
    }
}

@Serializable
internal data class DictFileMeta(
    val dictVersion: String,
    val schemaVersion: Int,
    val sha256: String,
    val bytes: Long,
    val downloadedBytes: Long = 0,
    val installedAt: String? = null,
)
