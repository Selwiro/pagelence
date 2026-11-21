package com.pagelence.core.dict

import kotlinx.serialization.Serializable
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

import com.pagelence.core.content.PagelenceJson

/**
 * `dict-manifest.json`, compiled into the app next to `content/`.
 *
 * The full ECDICT table is an optional download; this manifest is the pinned
 * contract for whichever artifact the project publishes. The app only accepts a
 * table whose schema and size/sha256 match, and refuses to run on a build older
 * than [minAppVersionCode] instead of guessing at a newer field layout.
 */
@Serializable
data class DictManifest(
    /** Upstream ECDICT revision, e.g. `39cfb96`. */
    val dictVersion: String,
    /** Field layout of the table; bumped only when a column changes. */
    val schemaVersion: Int,
    /** Exact byte size of the published `full-dict.sqlite`. */
    val bytes: Long,
    /** Lower-case hex SHA-256 of the published file. */
    val sha256: String,
    /** The smallest `versionCode` that can read this table. */
    val minAppVersionCode: Int,
)

/** The only `schemaVersion` this build understands. */
const val FULL_DICT_SCHEMA_VERSION = 1

/** Parse the manifest without leaking serialization into the Android shell. */
object DictManifestCodec {
    fun decode(json: String): DictManifest? = runCatching {
        PagelenceJson.decodeFromString<DictManifest>(json)
    }.getOrNull()
}

/** Why a manifest or a downloaded file cannot be used. */
sealed interface DictRejection {
    data class Schema(val supported: Int, val found: Int) : DictRejection
    data class AppTooOld(val required: Int, val current: Int) : DictRejection
    data class Malformed(val field: String) : DictRejection
    data class SizeMismatch(val expected: Long, val found: Long) : DictRejection
    data object ChecksumMismatch : DictRejection
}

object DictRules {
    private val SHA256 = Regex("[0-9a-fA-F]{64}")

    /** Validate the pinned manifest against the running app. */
    fun validate(manifest: DictManifest, appVersionCode: Int): DictRejection? = when {
        manifest.schemaVersion != FULL_DICT_SCHEMA_VERSION ->
            DictRejection.Schema(FULL_DICT_SCHEMA_VERSION, manifest.schemaVersion)

        manifest.minAppVersionCode > appVersionCode ->
            DictRejection.AppTooOld(manifest.minAppVersionCode, appVersionCode)

        manifest.dictVersion.isBlank() -> DictRejection.Malformed("dictVersion")
        manifest.bytes <= 0 -> DictRejection.Malformed("bytes")
        !SHA256.matches(manifest.sha256) -> DictRejection.Malformed("sha256")
        else -> null
    }

    /** Streaming SHA-256; a 80 MB table is never loaded into memory. */
    fun sha256(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(1 shl 16)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    /** Size first, then hash: the cheap check rejects most bad files. */
    fun verify(file: Path, manifest: DictManifest): DictRejection? {
        if (!Files.isRegularFile(file)) return DictRejection.SizeMismatch(manifest.bytes, 0)
        val size = Files.size(file)
        if (size != manifest.bytes) return DictRejection.SizeMismatch(manifest.bytes, size)
        return if (sha256(file).equals(manifest.sha256, ignoreCase = true)) {
            null
        } else {
            DictRejection.ChecksumMismatch
        }
    }
}
