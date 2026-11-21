package com.pagelence.sync

import com.pagelence.core.progress.ReadingStateCodec
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue
import org.junit.jupiter.api.io.TempDir

class SyncEngineTest {
    @TempDir
    lateinit var dir: Path

    private val clock = TestClock(Instant.parse("2026-11-21T10:00:00Z"))
    private val credentials = WebDavCredentials(url = "https://dav.example.com/dav/state.json")

    @Test
    fun statusSurvivesARestartOfTheStore() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        assertIs<SyncOutcome.Success>(engine(local, client).sync(credentials, SyncMode.MANUAL))
        val reloaded = SyncStateStore(dir.resolve("sync-state.json")).load()
        assertNotNull(reloaded.lastSuccessAt)
        assertNotNull(reloaded.lastEtag)
        assertNotNull(reloaded.baselineHash)
    }

    @Test
    fun firstSyncCreatesRemoteAndSecondDeviceDownloads() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val phone = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        val created = engine(phone, client, storeDir = dir.resolve("a")).sync(credentials, SyncMode.MANUAL)
        assertEquals(SyncAction.CREATE_REMOTE, assertIs<SyncOutcome.Success>(created).action)
        assertNotNull(remote.text)

        val tablet = MemoryLocalState(Fixtures.state(deviceId = "device-b"))
        val downloaded = engine(tablet, client, storeDir = dir.resolve("b")).sync(credentials, SyncMode.MANUAL)
        assertEquals(SyncAction.DOWNLOAD, assertIs<SyncOutcome.Success>(downloaded).action)
        assertEquals(setOf("b"), tablet.state.books.keys)
        assertEquals("device-b", tablet.state.deviceId)
    }

    @Test
    fun bothDevicesEditDifferentBooksAndConverge() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val a = MemoryLocalState(
            Fixtures.state(
                deviceId = "device-a",
                books = mapOf("b1" to Fixtures.book(id = "b1")),
            ),
        )
        val b = MemoryLocalState(
            Fixtures.state(
                deviceId = "device-b",
                books = mapOf("b2" to Fixtures.book(id = "b2")),
            ),
        )
        assertIs<SyncOutcome.Success>(engine(a, client, storeDir = dir.resolve("a")).sync(credentials, SyncMode.MANUAL))
        // B has not synced yet; its first round merges remote b1 with local b2.
        val bResult = engine(b, client, storeDir = dir.resolve("b")).sync(credentials, SyncMode.MANUAL)
        assertEquals(SyncAction.UPLOAD_MERGED, assertIs<SyncOutcome.Success>(bResult).action)
        assertEquals(setOf("b1", "b2"), b.state.books.keys)

        // A downloads B's merged document and ends with the same content.
        assertIs<SyncOutcome.Success>(engine(a, client, storeDir = dir.resolve("a")).sync(credentials, SyncMode.MANUAL))
        assertEquals(setOf("b1", "b2"), a.state.books.keys)
        assertEquals(a.state.copy(updatedAt = "", deviceId = ""), b.state.copy(updatedAt = "", deviceId = ""))
    }

    @Test
    fun preconditionRetriesWholeRoundAndSucceeds() {
        val remote = FakeRemote()
        // Seed through the fake, then use the fake for the constrained device.
        val seed = engine(
            MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book()))),
            FakeWebDavClient(remote),
            storeDir = dir.resolve("a"),
        )
        assertIs<SyncOutcome.Success>(seed.sync(credentials, SyncMode.MANUAL))

        val local = MemoryLocalState(
            Fixtures.state(
                deviceId = "device-b",
                books = mapOf("b2" to Fixtures.book(id = "b2")),
            ),
        )
        // Another device writes between our GET and PUT, once.
        var interfered = false
        remote.beforePut = {
            if (!interfered) {
                interfered = true
                remote.bumpFromOutside(
                    ReadingStateCodec.encode(
                        Fixtures.state(
                            deviceId = "device-c",
                            updatedAt = "2026-11-21T09:00:00Z",
                            books = mapOf("b3" to Fixtures.book(id = "b3")),
                        ),
                    ),
                )
            }
        }
        val outcome = engine(local, FakeWebDavClient(remote), storeDir = dir.resolve("b")).sync(credentials, SyncMode.MANUAL)
        assertEquals(SyncAction.UPLOAD_MERGED, assertIs<SyncOutcome.Success>(outcome).action)
        assertTrue(interfered)
        assertEquals(setOf("b2", "b", "b3"), local.state.books.keys)
    }

    @Test
    fun repeatedPreconditionsFailWithActionableCategory() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        assertIs<SyncOutcome.Success>(engine(local, client).sync(credentials, SyncMode.MANUAL))
        local.edit {
            it.copy(
                updatedAt = "2026-11-21T10:30:00Z",
                books = it.books + ("b2" to Fixtures.book(id = "b2")),
            )
        }

        remote.beforePut = { remote.bumpFromOutside(remote.text ?: "") }
        val outcome = engine(local, client, clock).sync(credentials, SyncMode.MANUAL)
        val failure = assertIs<SyncOutcome.Failure>(outcome)
        assertEquals(SyncErrorCategory.PRECONDITION, failure.category)
        assertNotNull(failure.nextAttemptAt)
    }

    @Test
    fun localEditDuringDownloadRestartsInsteadOfOverwriting() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val uploaded = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        assertIs<SyncOutcome.Success>(engine(uploaded, client, storeDir = dir.resolve("a")).sync(credentials, SyncMode.MANUAL))

        val tablet = MemoryLocalState(Fixtures.state(deviceId = "device-b"))
        // Reader saves a card after the engine snapshotted local state.
        var edited = false
        remote.beforeGet = {
            if (!edited) {
                edited = true
                tablet.edit { state ->
                    state.copy(
                        updatedAt = "2026-11-21T10:00:01Z",
                        wordCards = listOf(Fixtures.card()),
                    )
                }
            }
        }
        val outcome = engine(tablet, client, storeDir = dir.resolve("b")).sync(credentials, SyncMode.MANUAL)
        assertEquals(SyncAction.UPLOAD_MERGED, assertIs<SyncOutcome.Success>(outcome).action)
        assertEquals(1, tablet.state.wordCards.size)
        assertEquals(setOf("b"), tablet.state.books.keys)
    }

    @Test
    fun serverWithoutEtagBacksUpRemoteBeforeOverwrite() {
        val remote = FakeRemote(noEtag = true)
        val client = FakeWebDavClient(remote)
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        assertIs<SyncOutcome.Success>(engine(local, client).sync(credentials, SyncMode.MANUAL))
        val previous = remote.text

        local.edit { it.copy(updatedAt = "2026-11-21T11:00:00Z", books = mapOf("b" to Fixtures.book(units = listOf("u1", "u2")))) }
        val outcome = assertIs<SyncOutcome.Success>(engine(local, client).sync(credentials, SyncMode.MANUAL))
        assertEquals(SyncAction.UPLOAD, outcome.action)
        assertTrue(outcome.warnings.any { it.contains("ETag") })
        assertEquals(1, backupFiles().size)
        assertEquals(previous, Files.readString(backupFiles().first()))
    }

    @Test
    fun remoteDeletedAfterSuccessIsRecreatedWithMustNotExist() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        assertIs<SyncOutcome.Success>(engine(local, client).sync(credentials, SyncMode.MANUAL))
        remote.text = null
        remote.etag = null

        val outcome = engine(local, client).sync(credentials, SyncMode.MANUAL)
        assertEquals(SyncAction.CREATE_REMOTE, assertIs<SyncOutcome.Success>(outcome).action)
        assertTrue(remote.puts >= 2)
        assertNotNull(remote.text)
    }

    @Test
    fun changingServerRewritesStateWithoutOldEtagOrBaseline() {
        val remote = FakeRemote()
        val client = FakeWebDavClient(remote)
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        assertIs<SyncOutcome.Success>(engine(local, client).sync(credentials, SyncMode.MANUAL))
        val stateFile = SyncStateStore(dir.resolve("sync-state.json"))
        assertNotNull(stateFile.load().lastEtag)

        val other = WebDavCredentials(url = "https://other.example.com/dav/state.json")
        assertIs<SyncOutcome.Success>(engine(local, client).sync(other, SyncMode.MANUAL))
        assertEquals("https://other.example.com/dav/state.json\n", stateFile.load().serverKey)
        assertTrue(Files.exists(dir.resolve("baseline.json")))
    }

    @Test
    fun failureBacksOffAutoSyncButNotManual() {
        val remote = FakeRemote(text = "<html>login</html>", etag = "\"x\"")
        val client = FakeWebDavClient(remote)
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        val first = engine(local, client, clock).sync(credentials, SyncMode.MANUAL)
        assertIs<SyncOutcome.Failure>(first)
        assertEquals(SyncOutcome.Skipped, engine(local, client, clock).sync(credentials, SyncMode.AUTO))
        clock.advanceSeconds(31)
        assertIs<SyncOutcome.Failure>(engine(local, client, clock).sync(credentials, SyncMode.AUTO))
    }

    @Test
    fun invalidRemoteJsonStopsWithoutOverwriting() {
        val remote = FakeRemote(text = "<html>login</html>", etag = "\"x\"")
        val local = MemoryLocalState(Fixtures.state(books = mapOf("b" to Fixtures.book())))
        val outcome = engine(local, FakeWebDavClient(remote)).sync(credentials, SyncMode.MANUAL)
        val failure = assertIs<SyncOutcome.Failure>(outcome)
        assertEquals("REMOTE_DECODE", failure.code)
        assertEquals("<html>login</html>", remote.text)
        assertFalse(Files.exists(dir.resolve("baseline.json")))
    }

    private fun engine(
        local: LocalState,
        client: WebDavClient,
        clock: TestClock = this.clock,
        storeDir: Path = dir,
    ): SyncEngine = SyncEngine(
        client = client,
        local = local,
        stateStore = SyncStateStore(storeDir.resolve("sync-state.json")),
        baselineStore = SyncBaselineStore(storeDir.resolve("baseline.json")),
        backupDir = dir.resolve("backups"),
        now = { clock.instant },
        today = { "2026-11-21" },
    )

    private fun backupFiles(): List<Path> {
        val backupDir = dir.resolve("backups")
        if (!Files.isDirectory(backupDir)) return emptyList()
        return Files.list(backupDir).use { stream -> stream.toList() }
    }
}
