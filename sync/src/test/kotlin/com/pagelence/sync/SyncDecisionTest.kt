package com.pagelence.sync

import kotlin.test.Test
import kotlin.test.assertEquals

class SyncDecisionTest {
    @Test
    fun bothUnchangedIsUpToDate() {
        assertEquals(
            SyncAction.UP_TO_DATE,
            SyncDecision.decide(localChanged = false, remoteExists = true, remoteChanged = false),
        )
    }

    @Test
    fun onlyLocalChangedUploads() {
        assertEquals(
            SyncAction.UPLOAD,
            SyncDecision.decide(localChanged = true, remoteExists = true, remoteChanged = false),
        )
    }

    @Test
    fun onlyRemoteChangedDownloads() {
        assertEquals(
            SyncAction.DOWNLOAD,
            SyncDecision.decide(localChanged = false, remoteExists = true, remoteChanged = true),
        )
    }

    @Test
    fun bothChangedMergesInsteadOfStopping() {
        assertEquals(
            SyncAction.UPLOAD_MERGED,
            SyncDecision.decide(localChanged = true, remoteExists = true, remoteChanged = true),
        )
    }

    @Test
    fun missingRemoteWithLocalDataCreates() {
        assertEquals(
            SyncAction.CREATE_REMOTE,
            SyncDecision.decide(localChanged = true, remoteExists = false, remoteChanged = false),
        )
    }

    @Test
    fun missingRemoteAndUnchangedLocalDoesNothing() {
        assertEquals(
            SyncAction.UP_TO_DATE,
            SyncDecision.decide(localChanged = false, remoteExists = false, remoteChanged = false),
        )
    }

    @Test
    fun deletedRemoteIsRestoredAfterAnEarlierSuccess() {
        assertEquals(
            SyncAction.CREATE_REMOTE,
            SyncDecision.decide(
                localChanged = false,
                remoteExists = false,
                remoteChanged = true,
                restoreMissing = true,
            ),
        )
    }

    @Test
    fun blankDeviceDownloadsInsteadOfOverwriting() {
        assertEquals(
            SyncAction.DOWNLOAD,
            SyncDecision.decide(
                localChanged = true,
                remoteExists = true,
                remoteChanged = true,
                localBlank = true,
            ),
        )
        assertEquals(
            SyncAction.UP_TO_DATE,
            SyncDecision.decide(
                localChanged = false,
                remoteExists = false,
                remoteChanged = false,
                localBlank = true,
            ),
        )
    }

    @Test
    fun backoffGrowsToTwoMinuteCap() {
        assertEquals(30, SyncEngine.backoffSeconds(1))
        assertEquals(60, SyncEngine.backoffSeconds(2))
        assertEquals(120, SyncEngine.backoffSeconds(3))
        assertEquals(120, SyncEngine.backoffSeconds(9))
    }
}
