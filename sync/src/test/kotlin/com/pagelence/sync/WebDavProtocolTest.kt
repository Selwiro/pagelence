package com.pagelence.sync

import com.pagelence.core.progress.ReadingStateCodec
import com.sun.net.httpserver.HttpExchange
import com.sun.net.httpserver.HttpServer
import java.net.InetSocketAddress
import java.util.Base64
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull

/**
 * Protocol tests against a real HTTP server on loopback. The stub implements
 * the server behaviours that broke MileNote in production: HEAD rejected,
 * missing ETag, 409 before MKCOL, 401 vs 403, If-Match and If-None-Match.
 */
class WebDavProtocolTest {
    private val servers = mutableListOf<HttpServer>()

    @AfterTest
    fun stop() {
        servers.forEach { it.stop(0) }
    }

    @Test
    fun headAndGetReturnMetadataAndText() {
        val stub = stub(
            seed = ReadingStateCodec.encode(
                Fixtures.state(books = mapOf("b" to Fixtures.book())),
            ),
        )
        val client = OkHttpWebDavClient()
        val credentials = stub.credentials()

        val head = client.head(credentials)
        assertEquals(true, head.exists)
        assertEquals("v1", head.etag)
        val content = assertNotNull(client.get(credentials))
        assertEquals(stub.text, content.text)
        assertEquals("v1", content.etag)
    }

    @Test
    fun conditionalPutRejectsStaleEtag() {
        val stub = stub(seed = "{\"hello\":1}")
        val client = OkHttpWebDavClient()
        val credentials = stub.credentials()

        val stale = assertFailsWith<SyncException> {
            client.put(credentials, "{\"n\":2}", PutCondition.IfMatch("v0"))
        }
        assertEquals(SyncErrorCategory.PRECONDITION, stale.category)
        assertEquals(412, stale.status)

        val written = client.put(credentials, "{\"n\":2}", PutCondition.IfMatch("v1"))
        assertEquals("v2", written.etag)
        assertEquals("{\"n\":2}", stub.text)
    }

    @Test
    fun putCreatesMissingParentCollectionAfter409() {
        val stub = stub(seed = null, requireDirectory = true)
        val client = OkHttpWebDavClient()
        val credentials = stub.credentials()

        val written = client.put(credentials, "{\"n\":1}", PutCondition.MustNotExist)
        assertEquals(true, written.exists)
        assertEquals("{\"n\":1}", stub.text)
        assertEquals(true, stub.mkcolPaths.contains("/dav/"))
    }

    @Test
    fun head405FallsBackToRangeGetAndReadsRealSize() {
        val stub = stub(seed = "0123456789", headMethodAllowed = false)
        val client = OkHttpWebDavClient()
        val credentials = stub.credentials()

        val meta = client.head(credentials)
        assertEquals(true, meta.exists)
        assertEquals(10, meta.size)
    }

    @Test
    fun missingFileIsNotAnError() {
        val stub = stub(seed = null)
        val client = OkHttpWebDavClient()
        val credentials = stub.credentials()

        assertEquals(false, client.head(credentials).exists)
        assertNull(client.get(credentials))
    }

    @Test
    fun authAndPermissionAreSeparateCategories() {
        val authStub = stub(seed = "{}", expectedAuth = "user:app-password")
        val client = OkHttpWebDavClient()
        val wrong = assertFailsWith<SyncException> {
            client.get(authStub.credentials(password = "login-password"))
        }
        assertEquals(SyncErrorCategory.AUTH, wrong.category)

        val readOnlyStub = stub(seed = "{}", readOnly = true)
        val denied = assertFailsWith<SyncException> {
            client.put(readOnlyStub.credentials(), "{\"n\":1}", PutCondition.IfMatch("v1"))
        }
        assertEquals(SyncErrorCategory.PERMISSION, denied.category)
        assertEquals(403, denied.status)
    }

    @Test
    fun oversizedContentIsRejectedBeforeBuffering() {
        val stub = stub(seed = "0123456789abcdef")
        val client = OkHttpWebDavClient(maxBytes = 8)
        val error = assertFailsWith<SyncException> { client.get(stub.credentials()) }
        assertEquals(SyncErrorCategory.TOO_LARGE, error.category)
    }

    @Test
    fun plainHttpToLoopbackNeedsExplicitPermission() {
        val stub = stub(seed = "{}")
        val client = OkHttpWebDavClient()
        val strict = assertFailsWith<SyncException> { client.get(stub.credentials(allowHttp = false)) }
        assertEquals(SyncErrorCategory.URL, strict.category)
        assertEquals("{}", assertNotNull(client.get(stub.credentials(allowHttp = true))).text)
    }

    @Test
    fun engineSyncsEndToEndAgainstTheStub() {
        val stub = stub(seed = null)
        val temp = java.nio.file.Files.createTempDirectory("pagelence-sync-e2e")
        val client = OkHttpWebDavClient()
        fun engine(local: LocalState, name: String) = SyncEngine(
            client = client,
            local = local,
            stateStore = SyncStateStore(temp.resolve("$name-state.json")),
            baselineStore = SyncBaselineStore(temp.resolve("$name-baseline.json")),
            backupDir = temp.resolve("$name-backups"),
        )

        val phone = MemoryLocalState(
            Fixtures.state(books = mapOf("b" to Fixtures.book())),
        )
        val created = engine(phone, "a").sync(stub.credentials(), SyncMode.MANUAL)
        assertEquals(SyncAction.CREATE_REMOTE, assertIs<SyncOutcome.Success>(created).action)

        val tablet = MemoryLocalState(
            Fixtures.state(deviceId = "device-b", books = mapOf("b2" to Fixtures.book(id = "b2"))),
        )
        val merged = engine(tablet, "b").sync(stub.credentials(), SyncMode.MANUAL)
        assertEquals(SyncAction.UPLOAD_MERGED, assertIs<SyncOutcome.Success>(merged).action)
        val remote = assertNotNull(stub.text)
        val decoded = assertNotNull(ReadingStateCodec.decode(remote)).state
        assertEquals(setOf("b", "b2"), decoded.books.keys)
    }

    // --- stub ------------------------------------------------------------

    private class Stub(
        val server: HttpServer,
        var text: String?,
        val headMethodAllowed: Boolean,
        val requireDirectory: Boolean,
        val readOnly: Boolean,
        val expectedAuth: String?,
    ) {
        var etag: String = "v1"
        val mkcolPaths = mutableSetOf<String>()

        fun credentials(password: String = "secret", allowHttp: Boolean = true) = WebDavCredentials(
            url = "http://127.0.0.1:${server.address.port}/dav/state.json",
            username = "user",
            password = password,
            allowHttp = allowHttp,
        )
    }

    private fun stub(
        seed: String?,
        headMethodAllowed: Boolean = true,
        requireDirectory: Boolean = false,
        readOnly: Boolean = false,
        expectedAuth: String? = null,
    ): Stub {
        val server = HttpServer.create(InetSocketAddress("127.0.0.1", 0), 0)
        servers += server
        val stub = Stub(server, seed, headMethodAllowed, requireDirectory, readOnly, expectedAuth)
        server.createContext("/dav/") { exchange -> handle(stub, exchange) }
        server.start()
        return stub
    }

    private fun handle(stub: Stub, exchange: HttpExchange) {
        try {
            if (stub.expectedAuth != null) {
                val expected = "Basic " + Base64.getEncoder()
                    .encodeToString(stub.expectedAuth.toByteArray(Charsets.UTF_8))
                if (exchange.requestHeaders.getFirst("Authorization") != expected) {
                    respond(exchange, 401, null)
                    return
                }
            }
            val method = exchange.requestMethod
            val path = exchange.requestURI.path
            if (method == "MKCOL") {
                stub.mkcolPaths += path
                respond(exchange, 201, null)
                return
            }
            if (method == "HEAD") {
                if (!stub.headMethodAllowed) {
                    respond(exchange, 405, null)
                    return
                }
                val text = stub.text
                if (text == null) {
                    respond(exchange, 404, null)
                } else {
                    exchange.responseHeaders.add("ETag", "\"${stub.etag}\"")
                    exchange.responseHeaders.add("Content-Length", text.length.toString())
                    exchange.sendResponseHeaders(200, -1)
                }
                return
            }
            if (method == "GET") {
                val text = stub.text
                if (text == null) {
                    respond(exchange, 404, null)
                    return
                }
                val range = exchange.requestHeaders.getFirst("Range")
                if (range != null) {
                    exchange.responseHeaders.add("ETag", "\"${stub.etag}\"")
                    exchange.responseHeaders.add("Content-Range", "bytes 0-0/${text.length}")
                    val body = text.take(1).toByteArray(Charsets.UTF_8)
                    exchange.sendResponseHeaders(206, body.size.toLong())
                    exchange.responseBody.use { it.write(body) }
                    return
                }
                exchange.responseHeaders.add("ETag", "\"${stub.etag}\"")
                val body = text.toByteArray(Charsets.UTF_8)
                exchange.sendResponseHeaders(200, body.size.toLong())
                exchange.responseBody.use { it.write(body) }
                return
            }
            if (method == "PUT") {
                if (stub.readOnly) {
                    respond(exchange, 403, null)
                    return
                }
                if (stub.requireDirectory && stub.mkcolPaths.isEmpty()) {
                    respond(exchange, 409, null)
                    return
                }
                val ifMatch = exchange.requestHeaders.getFirst("If-Match")
                val ifNoneMatch = exchange.requestHeaders.getFirst("If-None-Match")
                if (ifMatch != null && Etag.normalize(ifMatch) != stub.etag) {
                    respond(exchange, 412, null)
                    return
                }
                if (ifNoneMatch == "*" && stub.text != null) {
                    respond(exchange, 412, null)
                    return
                }
                val body = exchange.requestBody.readBytes().toString(Charsets.UTF_8)
                stub.text = body
                stub.etag = "v" + (stub.etag.removePrefix("v").toIntOrNull()?.plus(1) ?: 1)
                exchange.responseHeaders.add("ETag", "\"${stub.etag}\"")
                respond(exchange, 201, null)
                return
            }
            respond(exchange, 405, null)
        } finally {
            exchange.close()
        }
    }

    private fun respond(exchange: HttpExchange, code: Int, body: String?) {
        val bytes = body?.toByteArray(Charsets.UTF_8)
        if (bytes == null) {
            exchange.sendResponseHeaders(code, -1)
        } else {
            exchange.sendResponseHeaders(code, bytes.size.toLong())
            exchange.responseBody.use { it.write(bytes) }
        }
    }

}
