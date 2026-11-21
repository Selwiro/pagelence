package com.pagelence.data

import com.pagelence.core.content.HintExample
import com.pagelence.core.prefs.Language
import com.pagelence.core.prefs.Preferences
import com.pagelence.core.progress.BookProgress
import com.pagelence.core.progress.CefrLevel
import com.pagelence.core.progress.DailyActivity
import com.pagelence.core.progress.LevelSource
import com.pagelence.core.progress.ReaderPhase
import com.pagelence.core.progress.ReaderPosition
import com.pagelence.core.progress.ReadingStateCodec
import com.pagelence.core.wordcards.WordCard
import java.io.File
import java.nio.file.Files
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class ReadingStateStoreTest {
    private fun tempStore(
        file: java.io.File,
        now: () -> String = { "2026-11-19T01:00:00Z" },
    ) = ReadingStateStore(file, now = now, newDeviceId = { "device-test" })

    private fun card(createdAt: String = "2026-11-19T01:00:00Z") = WordCard(
        id = WordCard.idOf("doctor-dolittle-1920", "chapter-01-p002", "chapter-01-p002-folks"),
        bookId = "doctor-dolittle-1920",
        paragraphId = "chapter-01-p002",
        hintId = "chapter-01-p002-folks",
        unitId = "chapter-01-u01",
        term = "skiff",
        lemma = "skiff",
        pos = "noun",
        zh = "小艇",
        en = "A small open boat.",
        ipa = "skɪf",
        example = HintExample(en = "He fished from a skiff.", zh = "他驾着小艇捕鱼。"),
        createdAt = createdAt,
        updatedAt = createdAt,
    )

    @Test
    fun `progress survives a restart`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = tempStore(file)
        val progress = BookProgress(
            bookId = "doctor-dolittle-1920",
            paragraphId = "chapter-01-p003",
            readUnitIds = listOf("chapter-01-u01"),
        )
        StateProgressRepository(first).save(progress)

        val reopened = StateProgressRepository(tempStore(file))
        val restored = reopened.progress("doctor-dolittle-1920")
        assertEquals(progress.bookId, restored.bookId)
        assertEquals(progress.paragraphId, restored.paragraphId)
        assertEquals(progress.readUnitIds, restored.readUnitIds)
        assertNotNull(restored.lastReadAt, "saving stamps lastReadAt for recent reading")
        assertEquals("device-test", tempStore(file).state().deviceId)
    }

    @Test
    fun `reader position survives a restart with phase page and quiz answers`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = tempStore(file)
        val position = ReaderPosition(
            unitId = "chapter-01-u01",
            phase = ReaderPhase.QUIZ,
            textPage = 2,
            textProgress = 0.4f,
            quizAnswers = mapOf(0 to 1, 1 to 2),
            quizSubmitted = false,
            grammarReached = false,
        )
        StateProgressRepository(first).save(
            BookProgress(
                bookId = "doctor-dolittle-1920",
                paragraphId = "chapter-01-p003",
                readUnitIds = listOf("chapter-01-u01"),
                reader = position,
            ),
        )

        val restored = StateProgressRepository(tempStore(file))
            .progress("doctor-dolittle-1920")
            .reader
        assertEquals(position, restored)
    }

    @Test
    fun `loading the state file writes a backup`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = tempStore(file)
        StateProgressRepository(first).save(
            BookProgress(bookId = "doctor-dolittle-1920", readUnitIds = listOf("chapter-01-u01")),
        )
        val backup = File(file.parentFile, "state.json.bak")
        assertFalse(backup.exists())

        val reopened = tempStore(file)
        assertTrue(backup.isFile)
        val decoded = ReadingStateCodec.decode(backup.readText(Charsets.UTF_8))?.state
        assertEquals(listOf("chapter-01-u01"), decoded?.books?.get("doctor-dolittle-1920")?.readUnitIds)
        assertEquals(1, reopened.state().books.size)
    }

    @Test
    fun `a corrupt main file is restored from the backup`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = tempStore(file)
        StateProgressRepository(first).save(
            BookProgress(bookId = "doctor-dolittle-1920", readUnitIds = listOf("chapter-01-u01")),
        )
        tempStore(file) // load once so the backup exists
        file.writeText("{ definitely not json", Charsets.UTF_8)

        val recovered = tempStore(file)
        assertTrue(recovered.recoveredFromBackup)
        assertFalse(recovered.corruptWithoutBackup)
        assertEquals(
            listOf("chapter-01-u01"),
            StateProgressRepository(recovered).progress("doctor-dolittle-1920").readUnitIds,
        )
        assertTrue(File(file.parentFile, "state.json.corrupt").isFile)
        assertNotNull(ReadingStateCodec.decode(file.readText(Charsets.UTF_8)))
        // The main file is healed, so the next launch does not report again.
        val next = tempStore(file)
        assertFalse(next.recoveredFromBackup)
        assertFalse(next.corruptWithoutBackup)
    }

    @Test
    fun `corrupt main and backup start empty and flag the damage`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = tempStore(file)
        StateProgressRepository(first).save(
            BookProgress(bookId = "doctor-dolittle-1920", readUnitIds = listOf("chapter-01-u01")),
        )
        tempStore(file) // create the backup
        file.writeText("{ broken", Charsets.UTF_8)
        File(file.parentFile, "state.json.bak").writeText("also broken", Charsets.UTF_8)

        val empty = tempStore(file)
        assertFalse(empty.recoveredFromBackup)
        assertTrue(empty.corruptWithoutBackup)
        assertTrue(empty.state().books.isEmpty())
        assertTrue(File(file.parentFile, "state.json.corrupt").isFile)
    }

    @Test
    fun `cards survive a restart and keep their tombstone`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = tempStore(file)
        val cards = StateWordCardRepository(first)
        cards.add(card())
        assertTrue(cards.isCollected("doctor-dolittle-1920", "chapter-01-p002", "chapter-01-p002-folks"))

        cards.remove(card().id, "2026-11-19T02:00:00Z")

        val reopened = StateWordCardRepository(tempStore(file))
        assertTrue(reopened.list("doctor-dolittle-1920").isEmpty())
        assertEquals(1, reopened.list("doctor-dolittle-1920", includeDeleted = true).size)
        assertFalse(reopened.isCollected("doctor-dolittle-1920", "chapter-01-p002", "chapter-01-p002-folks"))
        assertEquals(1, reopened.all(includeDeleted = true).size)
    }

    @Test
    fun `cards merge by lemma in the state file`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val cards = StateWordCardRepository(tempStore(file))
        val dogs = card().copy(
            id = WordCard.idOf("doctor-dolittle-1920", "p1", "h1"),
            paragraphId = "p1",
            hintId = "h1",
            term = "dogs",
            lemma = "dog",
        )
        val dog = card().copy(
            id = WordCard.idOf("doctor-dolittle-1920", "p2", "h2"),
            paragraphId = "p2",
            hintId = "h2",
            term = "dog",
            lemma = "dog",
        )

        assertEquals(dogs.id, cards.add(dogs).id)
        assertEquals(dogs.id, cards.add(dog).id)
        assertEquals(1, cards.list("doctor-dolittle-1920").size)
        assertEquals(dogs.id, cards.find("doctor-dolittle-1920", "dog", "dog")?.id)
    }

    @Test
    fun `re-adding a card keeps createdAt and clears the tombstone`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val cards = StateWordCardRepository(tempStore(file))
        cards.add(card())
        cards.remove(card().id, "2026-11-19T02:00:00Z")
        val restored = cards.add(card(createdAt = "2026-11-19T03:00:00Z"))

        assertEquals("2026-11-19T01:00:00Z", restored.createdAt)
        assertFalse(restored.isDeleted)
        assertEquals(1, cards.list("doctor-dolittle-1920").size)
    }

    @Test
    fun `preferences survive a restart`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val prefs = Preferences(
            language = Language.EN,
            handledOffers = listOf("pilot"),
        )
        StatePreferencesRepository(tempStore(file)).save(prefs)

        assertEquals(prefs, StatePreferencesRepository(tempStore(file)).preferences())
    }

    @Test
    fun `updatedAt moves on every write`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        var clock = "2026-11-19T01:00:00Z"
        val store = tempStore(file) { clock }
        val progress = StateProgressRepository(store)
        progress.save(BookProgress("book", readUnitIds = listOf("u1")))
        assertEquals("2026-11-19T01:00:00Z", store.state().updatedAt)

        clock = "2026-11-19T02:00:00Z"
        progress.save(BookProgress("book", readUnitIds = listOf("u1", "u2")))
        assertEquals("2026-11-19T02:00:00Z", store.state().updatedAt)
    }

    @Test
    fun `corrupt state falls back to an empty document`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        file.writeText("{not json")
        val store = tempStore(file)
        assertEquals("device-test", store.state().deviceId)
        assertTrue(store.state().books.isEmpty())
        assertTrue(store.state().wordCards.isEmpty())
    }

    @Test
    fun `v1 state is migrated in place without losing anything`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        file.writeText(javaClass.getResource("/state-v1.json")!!.readText())

        val state = tempStore(file).state()
        assertEquals(2, state.schemaVersion)
        assertEquals("device-legacy", state.deviceId)
        assertEquals("chapter-01-p003", state.progress("doctor-dolittle-1920").paragraphId)
        assertEquals(
            listOf("chapter-01-u01", "chapter-01-u02"),
            state.progress("doctor-dolittle-1920").readUnitIds,
        )
        val card = state.wordCards.single()
        assertEquals("folks", card.term)
        assertEquals("fəʊks", card.ipa)
        assertEquals(Language.EN, state.preferences.language)
        assertEquals("large", state.preferences.fontSize.name.lowercase())
        assertTrue(state.preferences.levelOnboardingDone, "old users are not prompted for a level")
        assertTrue(file.readText().contains("\"schemaVersion\":2"), "the file is upgraded on load")
        assertEquals(state, ReadingStateCodec.decodeOrNull(file.readText()))
    }

    @Test
    fun `a state from a newer build keeps its known fields and is not rewritten`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val future = javaClass.getResource("/state-v1.json")!!.readText()
            .replace("\"schemaVersion\": 1", "\"schemaVersion\": 3")
        file.writeText(future)

        val state = tempStore(file).state()
        assertEquals(3, state.schemaVersion)
        assertEquals("chapter-01-p003", state.progress("doctor-dolittle-1920").paragraphId)
        assertEquals("folks", state.wordCards.single().term)
        assertTrue(file.readText().contains("\"schemaVersion\": 3"), "the newer file is left alone")
    }

    @Test
    fun `level and daily activity survive a restart`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val first = StateReadingProfileRepository(tempStore(file))
        first.setLevel(CefrLevel.B1, LevelSource.QUICK, 88, "2026-11-19T03:00:00Z")
        first.recordReadingDay("2026-11-18")
        first.recordReadingDay("2026-11-19")
        first.recordCompletedUnit("2026-11-19")
        first.recordCompletedUnit("2026-11-19")

        val restored = StateReadingProfileRepository(tempStore(file)).profile()
        assertEquals(CefrLevel.B1, restored.selfLevel)
        assertEquals(LevelSource.QUICK, restored.levelSource)
        assertEquals(88, restored.levelScore)
        assertEquals("2026-11-19T03:00:00Z", restored.levelUpdatedAt)
        assertEquals(2, restored.readingDays)
        assertEquals(2, restored.activity("2026-11-19").completedUnits)
    }

    @Test
    fun `snapshot json carries v2 fields and can be decoded as an export`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val store = tempStore(file)
        val profile = StateReadingProfileRepository(store)
        profile.setLevel(CefrLevel.A2, LevelSource.SELF, null, "2026-11-19T03:00:00Z")
        profile.recordCompletedUnit("2026-11-19")
        StateWordCardRepository(store).add(card())

        val snapshot = store.snapshotJson()
        val decoded = requireNotNull(ReadingStateCodec.decode(snapshot)).state
        assertEquals(CefrLevel.A2, decoded.selfLevel)
        assertEquals(LevelSource.SELF, decoded.levelSource)
        assertEquals(DailyActivity(1), decoded.dailyActivity.getValue("2026-11-19"))
        assertEquals("skiff", decoded.wordCards.single().term)
    }

    @Test
    fun `state file is valid json after a write`() {
        val file = Files.createTempDirectory("pagelence-state").resolve("state.json").toFile()
        val store = tempStore(file)
        StateWordCardRepository(store).add(card())
        val text = file.readText()
        assertTrue(text.contains("\"schemaVersion\":2"))
        assertTrue(text.contains("\"deviceId\":\"device-test\""))
        assertNotNull(com.pagelence.core.content.PagelenceJson.parseToJsonElement(text))
        assertFalse(file.parentFile.listFiles()!!.any { it.name.endsWith(".tmp") })
    }

    @Test
    fun `backupNow captures the document before a destructive overwrite`() {
        val dir = Files.createTempDirectory("pagelence-state")
        val file = dir.resolve("state.json").toFile()
        val store = tempStore(file)
        StateWordCardRepository(store).add(card())
        val backup = dir.resolve("state.json.bak").toFile()
        backup.delete()

        assertTrue(store.backupNow())

        val backedUp = requireNotNull(ReadingStateCodec.decode(backup.readText())).state
        assertEquals("skiff", backedUp.wordCards.single().term)

        store.replace(store.state().copy(wordCards = emptyList()))
        assertTrue(store.state().wordCards.isEmpty())
        assertTrue(backup.isFile, "the pre-overwrite copy must survive the replace")
    }
}
