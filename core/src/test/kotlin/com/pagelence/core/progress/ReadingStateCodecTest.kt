package com.pagelence.core.progress

import com.pagelence.core.prefs.DefinitionMode
import com.pagelence.core.prefs.GuideMode
import com.pagelence.core.prefs.Language
import com.pagelence.core.prefs.ReaderLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ReadingStateCodecTest {
    private val v1 = """
        {
          "schemaVersion": 1,
          "deviceId": "device-legacy",
          "updatedAt": "2026-11-18T10:00:00Z",
          "books": {
            "doctor-dolittle-1920": {
              "bookId": "doctor-dolittle-1920",
              "paragraphId": "chapter-01-p003",
              "readUnitIds": ["chapter-01-u01"]
            }
          },
          "wordCards": [
            {
              "id": "doctor-dolittle-1920::chapter-01-p002::chapter-01-p002-folks",
              "bookId": "doctor-dolittle-1920",
              "paragraphId": "chapter-01-p002",
              "hintId": "chapter-01-p002-folks",
              "term": "folks",
              "lemma": "folk",
              "zh": "人们",
              "createdAt": "2026-11-18T09:00:00Z",
              "updatedAt": "2026-11-18T09:00:00Z"
            }
          ],
          "preferences": {
            "language": "en",
            "definitionMode": "mixed",
            "guideMode": "en",
            "fontSize": "large",
            "handledOffers": ["pilot"]
          }
        }
    """.trimIndent()

    @Test
    fun `recent assessment papers survive a round trip`() {
        val state = ReadingState(
            deviceId = "device-test",
            updatedAt = "2026-11-20T10:00:00Z",
            recentAssessmentPapers = listOf(listOf("q-a2-v01"), listOf("q-b1-g02")),
        )
        val decoded = assertNotNull(ReadingStateCodec.decode(ReadingStateCodec.encode(state)))
        assertEquals(
            listOf(listOf("q-a2-v01"), listOf("q-b1-g02")),
            decoded.state.recentAssessmentPapers,
        )
    }

    @Test
    fun `a v2 document without finishedAt decodes it as null`() {
        val json = """
            {
              "schemaVersion": 2,
              "deviceId": "device-legacy",
              "updatedAt": "2026-11-20T10:00:00Z",
              "books": {
                "b": { "bookId": "b", "readUnitIds": ["u1"] }
              }
            }
        """.trimIndent()
        val decoded = assertNotNull(ReadingStateCodec.decode(json))
        assertNull(decoded.state.progress("b").finishedAt)
        assertEquals(STATE_SCHEMA_VERSION, decoded.state.schemaVersion)
    }

    @Test
    fun `finishedAt round-trips and stays optional`() {
        val state = ReadingState(
            deviceId = "device-test",
            updatedAt = "2026-11-21T10:00:00Z",
            books = mapOf(
                "b" to BookProgress(
                    bookId = "b",
                    readUnitIds = listOf("u1"),
                    finishedAt = "2026-11-21T09:00:00Z",
                ),
            ),
        )
        val decoded = assertNotNull(ReadingStateCodec.decode(ReadingStateCodec.encode(state)))
        assertEquals("2026-11-21T09:00:00Z", decoded.state.progress("b").finishedAt)
        val encoded = ReadingStateCodec.encode(decoded.state)
        assertTrue(encoded.contains("\"finishedAt\""))
    }

    @Test
    fun `v1 migrates without losing books cards preferences or device id`() {
        val decoded = assertNotNull(ReadingStateCodec.decode(v1))
        assertTrue(decoded.upgraded)
        assertFalse(decoded.fromFuture)
        assertEquals(1, decoded.sourceVersion)
        assertEquals(STATE_SCHEMA_VERSION, decoded.state.schemaVersion)
        assertEquals("device-legacy", decoded.state.deviceId)
        assertEquals("2026-11-18T10:00:00Z", decoded.state.updatedAt)
        assertEquals("chapter-01-p003", decoded.state.progress("doctor-dolittle-1920").paragraphId)
        assertEquals(listOf("chapter-01-u01"), decoded.state.progress("doctor-dolittle-1920").readUnitIds)
        assertEquals("folks", decoded.state.wordCards.single().term)
        assertEquals(Language.EN, decoded.state.preferences.language)
        assertEquals(DefinitionMode.MIXED, decoded.state.preferences.definitionMode)
        assertEquals(GuideMode.EN, decoded.state.preferences.guideMode)
        assertEquals(listOf("pilot"), decoded.state.preferences.handledOffers)
        assertTrue(decoded.state.preferences.levelOnboardingDone, "existing users are not prompted again")
        assertNull(decoded.state.selfLevel)
        assertTrue(decoded.state.dailyActivity.isEmpty())
    }

    @Test
    fun `v1 with missing optional fields falls back to defaults`() {
        val decoded = assertNotNull(
            ReadingStateCodec.decode(
                """{"schemaVersion":1,"deviceId":"d","updatedAt":"2026-11-18T10:00:00Z"}""",
            ),
        )
        assertEquals(STATE_SCHEMA_VERSION, decoded.state.schemaVersion)
        assertTrue(decoded.state.books.isEmpty())
        assertTrue(decoded.state.wordCards.isEmpty())
        assertEquals(LevelSource.SELF, decoded.state.levelSource)
        assertEquals(DefinitionMode.ZH, decoded.state.preferences.definitionMode)
        assertTrue(decoded.state.preferences.levelOnboardingDone)
    }

    @Test
    fun `v2 round-trips level and daily activity`() {
        val state = ReadingState(
            deviceId = "device-2",
            updatedAt = "2026-11-19T01:00:00Z",
            books = mapOf(
                "doctor-dolittle-1920" to BookProgress(
                    bookId = "doctor-dolittle-1920",
                    paragraphId = "chapter-01-p003",
                    readUnitIds = listOf("chapter-01-u01"),
                    lastReadAt = "2026-11-19T01:00:00Z",
                ),
            ),
            selfLevel = CefrLevel.B1,
            levelSource = LevelSource.QUICK,
            levelScore = 72,
            levelUpdatedAt = "2026-11-19T01:00:00Z",
            dailyActivity = mapOf(
                "2026-11-18" to DailyActivity(1),
                "2026-11-19" to DailyActivity(2),
            ),
        )
        val json = ReadingStateCodec.encode(state)
        assertTrue(json.contains("\"schemaVersion\":2"))
        assertTrue(json.contains("\"dailyActivity\""))
        assertTrue(json.contains("\"levelSource\":\"quick\""))
        val decoded = assertNotNull(ReadingStateCodec.decode(json))
        assertEquals(state, decoded.state)
        assertEquals(2, decoded.sourceVersion)
        assertFalse(decoded.upgraded)
    }

    @Test
    fun `reader position round-trips through the state document`() {
        val state = ReadingState(
            deviceId = "device-reader",
            updatedAt = "2026-11-20T01:00:00Z",
            books = mapOf(
                "alice-1866" to BookProgress(
                    bookId = "alice-1866",
                    paragraphId = "chapter-01-p003",
                    reader = ReaderPosition(
                        unitId = "chapter-01-u01",
                        phase = ReaderPhase.QUIZ,
                        textPage = 1,
                        textProgress = 0.75f,
                        quizAnswers = mapOf(0 to 1, 1 to 2),
                        quizSubmitted = true,
                        grammarReached = false,
                    ),
                ),
            ),
        )
        val decoded = assertNotNull(ReadingStateCodec.decode(ReadingStateCodec.encode(state)))
        assertEquals(state.books.getValue("alice-1866"), decoded.state.books.getValue("alice-1866"))
    }

    @Test
    fun `legacy layout without an explicit choice migrates to paged`() {
        val json = v1.replace(
            "\"preferences\": {",
            "\"preferences\": { \"readerLayout\": \"scroll\",",
        )
        val decoded = assertNotNull(ReadingStateCodec.decode(json))
        assertEquals(ReaderLayout.PAGED, decoded.state.preferences.readerLayout)
        assertFalse(decoded.state.preferences.readerLayoutChosen)
    }

    @Test
    fun `a newer version keeps known fields and is not marked for rewriting`() {
        val json = v1.replace("\"schemaVersion\": 1", "\"schemaVersion\": 3")
        val decoded = assertNotNull(ReadingStateCodec.decode(json))
        assertTrue(decoded.fromFuture)
        assertFalse(decoded.upgraded)
        assertEquals(3, decoded.state.schemaVersion)
        assertEquals("chapter-01-p003", decoded.state.progress("doctor-dolittle-1920").paragraphId)
        assertEquals("folks", decoded.state.wordCards.single().term)
    }

    @Test
    fun `unknown fields are ignored and an invalid score is clamped`() {
        val json = v1
            .replace("\"schemaVersion\": 1", "\"schemaVersion\": 2, \"futureField\": {\"x\": 1}")
            .replace("\"preferences\": {", "\"levelScore\": 120, \"preferences\": {")
        val decoded = assertNotNull(ReadingStateCodec.decode(json))
        assertEquals(100, decoded.state.levelScore)
        assertEquals("device-legacy", decoded.state.deviceId)
    }

    @Test
    fun `malformed documents decode to null`() {
        assertNull(ReadingStateCodec.decode("{not json"))
        assertNull(ReadingStateCodec.decode("[]"))
        assertNull(ReadingStateCodec.decode("""{"schemaVersion":2}"""))
    }
}
