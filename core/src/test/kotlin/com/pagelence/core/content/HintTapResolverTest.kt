package com.pagelence.core.content

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class HintTapResolverTest {
    private val sentenceText = "All the folks, young and old, knew him well by sight."

    private fun hint(
        id: String,
        target: String,
        sentence: Int? = null,
        kind: String = "phrase",
    ) = Hint(
        id = id,
        kind = kind,
        target = target,
        sentence = sentence,
        zh = "释义",
        en = "meaning",
    )

    private fun tap(word: String, occurrence: Int = 0): Pair<Int, Int> {
        var from = 0
        repeat(occurrence + 1) { from = sentenceText.indexOf(word, from) + 1 }
        val start = from - 1
        return start to start + word.length - 1
    }

    @Test
    fun `tap on either word opens the whole phrase`() {
        val hints = listOf(hint("by-sight", "by sight", sentence = 4))
        val (byStart, byEnd) = tap("by")
        val (sightStart, sightEnd) = tap("sight")

        val byTap = HintTapResolver.resolve(sentenceText, 4, hints, byStart, byEnd)
        val sightTap = HintTapResolver.resolve(sentenceText, 4, hints, sightStart, sightEnd)

        assertEquals("by sight", byTap?.target)
        assertEquals("by sight", sightTap?.target)
        assertEquals(sentenceText.indexOf("by sight"), byTap?.start)
        assertEquals(sentenceText.indexOf("sight") + 4, byTap?.end)
    }

    @Test
    fun `tap on the space between words still opens the phrase`() {
        val hints = listOf(hint("by-sight", "by sight", sentence = 4))
        val space = sentenceText.indexOf("by sight") + 2

        assertEquals("by sight", HintTapResolver.resolve(sentenceText, 4, hints, space, space)?.target)
    }

    @Test
    fun `multi-word proper names are promoted even when marked as word`() {
        val text = "He walked down the street in his high hat."
        val start = text.indexOf("high hat")
        val hints = listOf(hint("high-hat", "high hat", kind = "word"))

        assertEquals(
            "high hat",
            HintTapResolver.resolve(text, 3, hints, start, start + 3)?.target,
        )
    }

    @Test
    fun `targets with punctuation open as a whole`() {
        val text = "“M.D.” means that he was a proper doctor."
        val start = text.indexOf("M.D.")
        val hints = listOf(hint("md", "M.D.", sentence = 2, kind = "word"))

        val tap = HintTapResolver.resolve(text, 2, hints, start, start)

        assertEquals("M.D.", tap?.target)
        assertEquals(start, tap?.start)
        assertEquals(start + 3, tap?.end)
    }

    @Test
    fun `hint assigned to another sentence does not apply`() {
        val hints = listOf(hint("by-sight", "by sight", sentence = 5))
        val (start, end) = tap("sight")

        assertNull(HintTapResolver.resolve(sentenceText, 4, hints, start, end))
    }

    @Test
    fun `paragraph-level hint applies to every sentence`() {
        val hints = listOf(hint("by-sight", "by sight"))
        val (start, end) = tap("by")

        assertEquals("by sight", HintTapResolver.resolve(sentenceText, 99, hints, start, end)?.target)
    }

    @Test
    fun `the longest target wins over a nested word`() {
        val hints = listOf(
            hint("sight", "sight", sentence = 4),
            hint("by-sight", "by sight", sentence = 4),
        )
        val (start, end) = tap("sight")

        assertEquals("by sight", HintTapResolver.resolve(sentenceText, 4, hints, start, end)?.target)
    }

    @Test
    fun `exact-sentence hint wins over a paragraph-level hint for the same target`() {
        val hints = listOf(
            hint("generic", "by sight"),
            hint("specific", "by sight", sentence = 4),
        )
        val (start, end) = tap("sight")

        assertEquals("specific", HintTapResolver.resolve(sentenceText, 4, hints, start, end)?.hint?.id)
    }

    @Test
    fun `an unannotated word stays a plain word`() {
        val hints = listOf(hint("by-sight", "by sight", sentence = 4))
        val (start, end) = tap("folks")

        assertNull(HintTapResolver.resolve(sentenceText, 4, hints, start, end))
    }

    @Test
    fun `whole-sentence hints are not tap targets`() {
        val hints = listOf(hint("line", "All the folks, young and old, knew him well by sight.", kind = "sentence"))
        val (start, end) = tap("sight")

        assertNull(HintTapResolver.resolve(sentenceText, 4, hints, start, end))
    }

    @Test
    fun `word boundaries keep a short target from matching inside a longer word`() {
        val text = "The sights were lovely."
        val hints = listOf(hint("sight", "sight", kind = "word"))
        val start = text.indexOf("sights")

        assertNull(HintTapResolver.resolve(text, 1, hints, start, start + 4))
    }

    @Test
    fun `the tapped occurrence is the one returned`() {
        val text = "He knew him by sight, and she knew him by sight too."
        val hints = listOf(hint("by-sight", "by sight", sentence = 2))
        val second = text.indexOf("by sight", startIndex = text.indexOf("by sight") + 1)

        val tap = HintTapResolver.resolve(text, 2, hints, second, second + 1)

        assertEquals(second, tap?.start)
        assertEquals(second + 7, tap?.end)
    }

    @Test
    fun `matching ignores case`() {
        val hints = listOf(hint("by-sight", "By Sight", sentence = 4))
        val (start, end) = tap("sight")

        assertEquals("By Sight", HintTapResolver.resolve(sentenceText, 4, hints, start, end)?.target)
    }

    @Test
    fun `invalid ranges and blank text return null`() {
        val hints = listOf(hint("by-sight", "by sight", sentence = 4))

        assertNull(HintTapResolver.resolve("", 4, hints, 0, 0))
        assertNull(HintTapResolver.resolve(sentenceText, 4, hints, -1, 0))
        assertNull(HintTapResolver.resolve(sentenceText, 4, hints, 10, 9))
        assertNull(HintTapResolver.resolve(sentenceText, 4, hints, 0, sentenceText.length))
    }
}
