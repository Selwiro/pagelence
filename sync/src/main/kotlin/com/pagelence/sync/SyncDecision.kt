package com.pagelence.sync

/** What one sync round should do, given the three observations of the document. */
enum class SyncAction {
    /** Both sides already agree. */
    UP_TO_DATE,

    /** Remote is missing and local has data: create it conditionally. */
    CREATE_REMOTE,

    /** Only local changed: conditional overwrite. */
    UPLOAD,

    /** Only remote changed: replace local. */
    DOWNLOAD,

    /** Both changed: merge field by field, then conditional overwrite. */
    UPLOAD_MERGED,
}

/**
 * The decision table from the WebDAV playbook, extended with two cases that
 * only show up in long-lived installs:
 * - a blank device must download instead of uploading an empty document over
 *   real progress;
 * - if the remote file disappeared after a successful sync, local data is
 *   re-created with `If-None-Match: *` instead of quietly staying gone.
 */
object SyncDecision {
    fun decide(
        localChanged: Boolean,
        remoteExists: Boolean,
        remoteChanged: Boolean,
        restoreMissing: Boolean = false,
        localBlank: Boolean = false,
    ): SyncAction {
        if (!remoteExists) {
            if (localBlank) return SyncAction.UP_TO_DATE
            if (!localChanged && !restoreMissing) return SyncAction.UP_TO_DATE
            return SyncAction.CREATE_REMOTE
        }
        if (localBlank) return SyncAction.DOWNLOAD
        return when {
            localChanged && remoteChanged -> SyncAction.UPLOAD_MERGED
            localChanged -> SyncAction.UPLOAD
            remoteChanged -> SyncAction.DOWNLOAD
            else -> SyncAction.UP_TO_DATE
        }
    }
}
