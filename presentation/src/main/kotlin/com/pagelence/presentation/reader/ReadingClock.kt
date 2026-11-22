package com.pagelence.presentation.reader

/**
 * Counts the seconds a reader actually spends in the reader (T162).
 *
 * Fed with a **monotonic** clock (`SystemClock.elapsedRealtime`), never wall
 * time: changing the phone's date must not move reading time. The caller starts
 * the clock when the reader is on screen and the app is in the foreground, ticks
 * it every few seconds, and stops it on the way out.
 *
 * Gaps are capped at [MAX_SEGMENT_MS]: if the process is frozen (doze, a long
 * permission dialog, a killed activity) the gap is not reading time, so a
 * segment that overshoots the cap counts the cap instead of the whole gap. A
 * partial second is carried between ticks, so a 15-second ticker does not drift.
 */
class ReadingClock(private val maxSegmentMs: Long = MAX_SEGMENT_MS) {
    private var lastMs: Long? = null
    private var carriedMs: Long = 0L

    val running: Boolean get() = lastMs != null

    /** Start counting from [nowMs]; a no-op while already running. */
    fun start(nowMs: Long) {
        if (lastMs == null) {
            lastMs = nowMs
            carriedMs = 0L
        }
    }

    /** Whole seconds elapsed since the previous call; the remainder is carried. */
    fun tick(nowMs: Long): Int {
        val last = lastMs ?: return 0
        val delta = nowMs - last
        // A clock that moved backwards (or not at all) is not reading time.
        if (delta <= 0) {
            lastMs = nowMs
            return 0
        }
        lastMs = nowMs
        carriedMs += if (delta > maxSegmentMs) maxSegmentMs else delta
        val seconds = (carriedMs / 1000L).toInt()
        carriedMs -= seconds * 1000L
        return seconds
    }

    /** Stop and return the last, partial segment. */
    fun stop(nowMs: Long): Int {
        val seconds = tick(nowMs)
        lastMs = null
        carriedMs = 0L
        return seconds
    }

    companion object {
        /** Longest gap still counted; anything longer is treated as a pause. */
        const val MAX_SEGMENT_MS = 120_000L
    }
}
