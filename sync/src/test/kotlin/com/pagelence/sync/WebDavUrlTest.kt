package com.pagelence.sync

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class WebDavUrlTest {
    @Test
    fun acceptsHttpsFileUrl() {
        assertEquals(
            "https://dav.jianguoyun.com/dav/state.json",
            WebDavUrl.validateFileUrl("https://dav.jianguoyun.com/dav/state.json"),
        )
    }

    @Test
    fun rejectsDirectoryAndRoot() {
        assertCode("NOT_A_FILE_URL", "https://host/dav/")
        assertCode("NOT_A_FILE_URL", "https://host/")
    }

    @Test
    fun rejectsEmbeddedCredentials() {
        assertCode("CREDENTIALS_IN_URL", "https://user:pass@host/dav/state.json")
    }

    @Test
    fun rejectsQueryAndFragment() {
        assertCode("QUERY_OR_HASH_IN_URL", "https://host/dav/state.json?token=1")
        assertCode("QUERY_OR_HASH_IN_URL", "https://host/dav/state.json#x")
    }

    @Test
    fun rejectsPublicHttpButAllowsLoopbackWhenEnabled() {
        assertCode("HTTP_NOT_ALLOWED", "http://example.com/dav/state.json", allowHttp = true)
        assertCode("HTTP_NOT_ALLOWED", "http://127.0.0.1/dav/state.json", allowHttp = false)
        assertEquals(
            "http://127.0.0.1/dav/state.json",
            WebDavUrl.validateFileUrl("http://127.0.0.1/dav/state.json", allowHttp = true),
        )
        assertEquals(
            "http://10.0.2.2/dav/state.json",
            WebDavUrl.validateFileUrl("http://10.0.2.2/dav/state.json", allowHttp = true),
        )
    }

    @Test
    fun rejectsBlankAndOtherSchemes() {
        assertCode("EMPTY_URL", "   ")
        assertCode("UNSUPPORTED_SCHEME", "ftp://host/dav/state.json")
    }

    @Test
    fun derivesAncestorDirectoriesShallowFirst() {
        assertEquals(
            listOf("https://h/dav/", "https://h/dav/a/"),
            WebDavUrl.ancestorDirectories("https://h/dav/a/state.json"),
        )
        assertEquals(
            emptyList(),
            WebDavUrl.ancestorDirectories("https://h/state.json"),
        )
    }

    @Test
    fun normalizesEtag() {
        assertEquals("abc", Etag.normalize("W/\"abc\""))
        assertEquals("abc", Etag.normalize("\"abc\""))
        assertNull(Etag.normalize("  "))
        assertEquals("\"abc\"", Etag.toHeader("abc"))
        assertEquals("\"abc\"", Etag.toHeader("\"abc\""))
        assertEquals(true, Etag.equal("W/\"abc\"", "\"abc\""))
        assertEquals(false, Etag.equal("abc", "ABC"))
        assertEquals(false, Etag.equal(null, null))
    }

    private fun assertCode(code: String, url: String, allowHttp: Boolean = false) {
        val error = assertFailsWith<SyncException> { WebDavUrl.validateFileUrl(url, allowHttp) }
        assertEquals(code, error.code)
        assertEquals(SyncErrorCategory.URL, error.category)
    }
}
