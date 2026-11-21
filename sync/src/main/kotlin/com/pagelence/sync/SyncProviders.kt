package com.pagelence.sync

/**
 * Address shapes users actually paste. The UI shows these as hints; only
 * Jianguoyun has a complete default, the rest need the user's host or share.
 *
 * Note that these are not protocol differences: all four speak WebDAV. The
 * hint exists because "where do I get the file URL" is the most common setup
 * question.
 */
enum class SyncProvider {
    JIANGUOYUN,
    NEXTCLOUD,
    SYNOLOGY,
    INFINICLOUD,
    CUSTOM,
}

object SyncProviders {
    /** Complete URL with no placeholders, or null when the user must fill it. */
    fun defaultUrl(provider: SyncProvider): String? = when (provider) {
        SyncProvider.JIANGUOYUN -> "https://dav.jianguoyun.com/dav/Pagelence/state.json"
        else -> null
    }

    /** Example shown under the URL field; `<...>` marks what must be replaced. */
    fun templateUrl(provider: SyncProvider): String? = when (provider) {
        SyncProvider.JIANGUOYUN -> "https://dav.jianguoyun.com/dav/Pagelence/state.json"
        SyncProvider.NEXTCLOUD -> "https://<host>/remote.php/dav/files/<username>/Pagelence/state.json"
        SyncProvider.SYNOLOGY -> "https://<host>:5006/<share>/Pagelence/state.json"
        SyncProvider.INFINICLOUD -> "https://<subdomain>.infini-cloud.net/dav/Pagelence/state.json"
        SyncProvider.CUSTOM -> "https://<host>/dav/Pagelence/state.json"
    }
}
