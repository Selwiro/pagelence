package com.pagelence.core.dict

import com.pagelence.core.content.PagelenceJson
import java.nio.file.Files
import java.nio.file.Path
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue

class FullDictInstallerTest {
    private val body = ByteArray(300_000) { (it % 251).toByte() }

    @Test
    fun `a fresh download is verified and installed atomically`() {
        val dir = tempDir()
        val installer = FullDictInstaller(dir, FakeTransport(body))
        val manifest = manifestOf(dir, body)
        val progress = mutableListOf<Long>()
        val result = installer.install(manifest, URL) { downloaded, _ -> progress += downloaded }
        assertEquals(DictInstallResult.Installed, result)
        assertContentEquals(body, Files.readAllBytes(installer.databaseFile))
        assertTrue(installer.isInstalled(manifest))
        assertEquals("39cfb96", installer.installedVersion())
        assertEquals(body.size.toLong(), progress.last())
        assertFalse(Files.exists(dir.resolve("full-dict.sqlite.part")))
    }

    @Test
    fun `a paused partial resumes from its byte offset`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body)
        val partial = dir.resolve("full-dict.sqlite.part")
        Files.write(partial, body.copyOf(10_000))
        writePartMeta(dir, manifest, 10_000)
        val transport = FakeTransport(body)
        val installer = FullDictInstaller(dir, transport)

        assertEquals(DictInstallResult.Installed, installer.install(manifest, URL))

        assertEquals(10_000L, transport.lastOffset)
        assertContentEquals(body, Files.readAllBytes(installer.databaseFile))
    }

    @Test
    fun `a server that ignores the range header restarts from zero`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body)
        Files.write(dir.resolve("full-dict.sqlite.part"), body.copyOf(10_000))
        writePartMeta(dir, manifest, 10_000)
        val transport = FakeTransport(body, rangeSupport = false)
        val installer = FullDictInstaller(dir, transport)

        assertEquals(DictInstallResult.Installed, installer.install(manifest, URL))

        assertEquals(10_000L, transport.lastOffset)
        assertContentEquals(body, Files.readAllBytes(installer.databaseFile))
    }

    @Test
    fun `a checksum mismatch is refused and leaves no table`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body).copy(sha256 = "b".repeat(64))
        val installer = FullDictInstaller(dir, FakeTransport(body))

        val result = installer.install(manifest, URL)

        val failed = assertIs<DictInstallResult.Failed>(result)
        assertEquals(DictRejection.ChecksumMismatch, failed.rejection)
        assertFalse(Files.exists(installer.databaseFile))
        assertFalse(installer.isInstalled(manifest))
        assertFalse(installer.canResume(manifest))
    }

    @Test
    fun `cancelling keeps the partial for a later resume`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body)
        val installer = FullDictInstaller(dir, FakeTransport(body))
        val result = installer.install(manifest, URL) { downloaded, _ ->
            if (downloaded > 10_000) installer.cancel()
        }

        assertEquals(DictInstallResult.Cancelled, result)
        assertFalse(Files.exists(installer.databaseFile))
        assertTrue(installer.canResume(manifest))
        assertTrue(installer.downloadedBytes() in 1 until manifest.bytes)
    }

    @Test
    fun `a table left by an interrupted install is adopted`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body)
        val installer = FullDictInstaller(dir, FakeTransport(body))
        installer.install(manifest, URL)
        // Simulate the app dying between the rename and the metadata write.
        Files.deleteIfExists(dir.resolve("full-dict.json"))

        assertTrue(installer.isInstalled(manifest))
        assertTrue(Files.isRegularFile(dir.resolve("full-dict.json")))
        assertEquals("39cfb96", installer.installedVersion())
    }

    @Test
    fun `remove returns to the core table only`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body)
        val installer = FullDictInstaller(dir, FakeTransport(body))
        installer.install(manifest, URL)

        assertTrue(installer.remove())
        assertFalse(Files.exists(installer.databaseFile))
        assertNull(installer.installedVersion())
        assertFalse(installer.isInstalled(manifest))
        assertFalse(installer.remove())
    }

    @Test
    fun `an installed table from another manifest is not current`() {
        val dir = tempDir()
        val manifest = manifestOf(dir, body)
        val installer = FullDictInstaller(dir, FakeTransport(body))
        installer.install(manifest, URL)

        assertFalse(installer.isInstalled(manifest.copy(dictVersion = "another")))
        assertFalse(installer.canResume(manifest.copy(dictVersion = "another")))
    }

    private fun manifestOf(dir: Path, bytes: ByteArray): DictManifest {
        val tmp = dir.resolve("manifest-source.bin")
        Files.write(tmp, bytes)
        return DictManifest(
            dictVersion = "39cfb96",
            schemaVersion = FULL_DICT_SCHEMA_VERSION,
            bytes = bytes.size.toLong(),
            sha256 = DictRules.sha256(tmp),
            minAppVersionCode = 1,
        )
    }

    private fun writePartMeta(dir: Path, manifest: DictManifest, downloaded: Long) {
        val meta = DictFileMeta(
            dictVersion = manifest.dictVersion,
            schemaVersion = manifest.schemaVersion,
            sha256 = manifest.sha256,
            bytes = manifest.bytes,
            downloadedBytes = downloaded,
        )
        Files.writeString(
            dir.resolve("full-dict.part.json"),
            PagelenceJson.encodeToString(DictFileMeta.serializer(), meta),
        )
    }

    private fun tempDir(): Path = Files.createTempDirectory("full-dict")

    /** In-memory HTTP body with (optional) Range support. */
    private class FakeTransport(
        private val body: ByteArray,
        private val rangeSupport: Boolean = true,
    ) : DictTransport {
        var lastOffset: Long = -1
            private set

        override fun open(url: String, offset: Long): DictStream {
            lastOffset = offset
            val start = if (rangeSupport) offset.toInt() else 0
            val slice = body.copyOfRange(start, body.size)
            return object : DictStream {
                override val code: Int = if (rangeSupport && offset > 0) 206 else 200
                private var position = 0

                override fun read(buffer: ByteArray): Int {
                    if (position >= slice.size) return -1
                    val count = minOf(buffer.size, slice.size - position)
                    slice.copyInto(buffer, 0, position, position + count)
                    position += count
                    return count
                }

                override fun close() = Unit
            }
        }
    }

    private companion object {
        const val URL = "https://example.test/full-dict.sqlite"
    }
}
