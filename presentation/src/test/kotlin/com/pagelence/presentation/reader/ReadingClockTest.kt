package com.pagelence.presentation.reader

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ReadingClockTest {
    @Test
    fun `ticks return whole seconds and carry the remainder`() {
        val clock = ReadingClock()
        clock.start(0)

        assertEquals(14, clock.tick(14_900))
        assertEquals(1, clock.tick(15_100), "900ms carried plus 200ms")
        assertEquals(0, clock.tick(15_400), "400ms is still pending")
        assertEquals(1, clock.tick(16_000), "the carried 400ms plus 600ms makes a second")
    }

    @Test
    fun `a stopped clock counts nothing`() {
        val clock = ReadingClock()

        assertEquals(0, clock.tick(5_000))
        assertFalse(clock.running)
    }

    @Test
    fun `stop returns the last partial segment and resets`() {
        val clock = ReadingClock()
        clock.start(1_000)
        clock.tick(3_000)

        assertEquals(2, clock.stop(5_000))
        assertFalse(clock.running)
        assertEquals(0, clock.tick(9_000))
    }

    @Test
    fun `a long gap counts at most the cap`() {
        val clock = ReadingClock(maxSegmentMs = 120_000)
        clock.start(0)

        assertEquals(120, clock.tick(10 * 60_000))
    }

    @Test
    fun `a backwards clock is not reading time`() {
        val clock = ReadingClock()
        clock.start(50_000)

        assertEquals(0, clock.tick(40_000))
        assertEquals(1, clock.tick(41_000))
    }

    @Test
    fun `starting twice does not move the segment start`() {
        val clock = ReadingClock()
        clock.start(0)
        clock.start(30_000)

        assertTrue(clock.running)
        assertEquals(30, clock.tick(30_000), "time still counts from the first start")
    }
}
