package com.pagelence.data

import java.io.File
import java.io.IOException
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ContentSeedInstallerTest {
    private fun tempDir(): File =
        Files.createTempDirectory("pagelence-content-seed").toFile()

    private fun install(
        dir: File,
        stamp: String,
        copy: (File) -> Unit,
    ): ContentSeedInstaller.Result = ContentSeedInstaller.install(dir, stamp, copy)

    @Test
    fun `same install stamp skips the copy`() {
        val dir = tempDir()
        var copies = 0
        val copy: (File) -> Unit = { target ->
            copies += 1
            File(target, "a.txt").writeText("one")
        }

        val first = install(dir, "v1", copy)
        assertTrue(first.copied)
        assertEquals("one", File(first.content, "a.txt").readText())

        val second = install(dir, "v1", copy)
        assertFalse(second.copied)
        assertEquals(1, copies)
        assertEquals("one", File(second.content, "a.txt").readText())
    }

    @Test
    fun `new install stamp replaces the whole tree`() {
        val dir = tempDir()
        install(dir, "v1") { target -> File(target, "old.txt").writeText("old") }

        val next = install(dir, "v2") { target ->
            File(target, "new.txt").writeText("new")
            File(target, "third-party").mkdirs()
            File(target, "third-party/word-frequencies.json").writeText("{}")
        }

        assertTrue(next.copied)
        assertFalse(File(next.content, "old.txt").exists())
        assertEquals("new", File(next.content, "new.txt").readText())
        assertTrue(File(next.content, "third-party/word-frequencies.json").isFile)
        assertEquals("v2", File(dir, "content-seed.txt").readText().trim())
    }

    @Test
    fun `failed copy keeps the previous content and marker`() {
        val dir = tempDir()
        val first = install(dir, "v1") { target -> File(target, "a.txt").writeText("one") }

        val failed = install(dir, "v2") { target ->
            File(target, "partial.txt").writeText("half")
            throw IOException("disk full")
        }

        assertFalse(failed.copied)
        assertEquals(first.content, failed.content)
        assertEquals("one", File(failed.content, "a.txt").readText())
        assertFalse(File(failed.content, "partial.txt").exists())
        assertEquals("v1", File(dir, "content-seed.txt").readText().trim())
    }
}
