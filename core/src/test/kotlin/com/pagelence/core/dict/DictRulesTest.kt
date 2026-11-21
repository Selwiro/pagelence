package com.pagelence.core.dict

import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class DictRulesTest {
    private val manifest = DictManifest(
        dictVersion = "39cfb96",
        schemaVersion = FULL_DICT_SCHEMA_VERSION,
        bytes = 5,
        sha256 = "a".repeat(64),
        minAppVersionCode = 1,
    )

    @Test
    fun `a well formed manifest passes`() {
        assertNull(DictRules.validate(manifest, appVersionCode = 1))
    }

    @Test
    fun `a newer schema is refused instead of parsed`() {
        assertEquals(
            DictRejection.Schema(FULL_DICT_SCHEMA_VERSION, 2),
            DictRules.validate(manifest.copy(schemaVersion = 2), appVersionCode = 9),
        )
    }

    @Test
    fun `a table that needs a newer app is refused`() {
        assertEquals(
            DictRejection.AppTooOld(required = 7, current = 3),
            DictRules.validate(manifest.copy(minAppVersionCode = 7), appVersionCode = 3),
        )
    }

    @Test
    fun `malformed checksums sizes and versions are refused`() {
        assertEquals(DictRejection.Malformed("sha256"), DictRules.validate(manifest.copy(sha256 = "xyz"), 1))
        assertEquals(DictRejection.Malformed("bytes"), DictRules.validate(manifest.copy(bytes = 0), 1))
        assertEquals(DictRejection.Malformed("dictVersion"), DictRules.validate(manifest.copy(dictVersion = " "), 1))
    }

    @Test
    fun `verify checks size then hash`() {
        val dir = Files.createTempDirectory("dict-rules")
        val file = dir.resolve("full-dict.sqlite")
        Files.writeString(file, "hello")
        val valid = manifest.copy(bytes = 5, sha256 = DictRules.sha256(file))
        assertNull(DictRules.verify(file, valid))
        assertEquals(
            DictRejection.SizeMismatch(expected = 9, found = 5),
            DictRules.verify(file, valid.copy(bytes = 9)),
        )
        assertEquals(DictRejection.ChecksumMismatch, DictRules.verify(file, valid.copy(sha256 = "b".repeat(64))))
    }
}
