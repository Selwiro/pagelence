package com.pagelence.presentation.pagination

import com.pagelence.core.prefs.FontSize
import com.pagelence.uim3.UiLanguage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class PaginationCacheTest {
    @Test
    fun hitSkipsTheSecondCompute() {
        val cache = PaginationCache<String>(maxEntries = 2)
        var calls = 0

        val first = cache.getOrPut(key()) { calls += 1; "pages" }
        val second = cache.getOrPut(key()) { calls += 1; "other" }

        assertEquals("pages", first)
        assertEquals("pages", second)
        assertEquals(1, calls)
        assertEquals(1, cache.size)
    }

    @Test
    fun geometryAndContentChangesMiss() {
        val cache = PaginationCache<String>(maxEntries = 4)
        cache.getOrPut(key(width = 400)) { "400" }

        assertEquals("400", cache.getOrPut(key(width = 400)) { "unused" })
        assertEquals("401", cache.getOrPut(key(width = 401)) { "401" })
        assertEquals("other-unit", cache.getOrPut(key(unitId = "u2")) { "other-unit" })
        assertEquals("new-text", cache.getOrPut(key(fingerprint = 99L)) { "new-text" })
        assertEquals("large", cache.getOrPut(key(fontSize = FontSize.LARGE)) { "large" })
        assertEquals("english", cache.getOrPut(key(language = UiLanguage.EN)) { "english" })
        assertEquals("denser", cache.getOrPut(key(density = 3.0f)) { "denser" })
        assertEquals("scaled", cache.getOrPut(key(fontScale = 1.3f)) { "scaled" })
    }

    @Test
    fun leastRecentlyUsedEntryIsEvicted() {
        val cache = PaginationCache<String>(maxEntries = 2)
        cache.getOrPut(key(unitId = "u1")) { "a" }
        cache.getOrPut(key(unitId = "u2")) { "b" }

        // Touching u1 makes u2 the eldest entry.
        assertEquals("a", cache.get(key(unitId = "u1")))
        cache.getOrPut(key(unitId = "u3")) { "c" }

        assertNull(cache.get(key(unitId = "u2")))
        assertNotNull(cache.get(key(unitId = "u1")))
        assertEquals("c", cache.get(key(unitId = "u3")))
        assertEquals(2, cache.size)
    }

    @Test
    fun clearDropsEveryEntry() {
        val cache = PaginationCache<String>(maxEntries = 2)
        cache.getOrPut(key(unitId = "u1")) { "a" }
        cache.getOrPut(key(unitId = "u2")) { "b" }

        cache.clear()

        assertEquals(0, cache.size)
        assertNull(cache.get(key(unitId = "u1")))
    }

    @Test
    fun capacityMustBePositive() {
        assertFailsWith<IllegalArgumentException> { PaginationCache<String>(maxEntries = 0) }
    }

    private fun key(
        unitId: String = "u1",
        width: Int = 400,
        fingerprint: Long = 1L,
        fontSize: FontSize = FontSize.MEDIUM,
        language: UiLanguage = UiLanguage.ZH,
        density: Float = 2.75f,
        fontScale: Float = 1f,
    ) = PaginationKey(
        bookId = "doctor-dolittle",
        unitId = unitId,
        contentWidthPx = width,
        pageHeightPx = 600,
        fontSize = fontSize,
        language = language,
        density = density,
        fontScale = fontScale,
        fingerprint = fingerprint,
    )
}
