package com.pagelence.sync

import com.pagelence.core.progress.ReadingState
import com.pagelence.core.progress.ReadingStateCodec

/** In-memory WebDAV server with the conditional-write rules of a real one. */
class FakeRemote(
    var text: String? = null,
    var etag: String? = null,
    var noEtag: Boolean = false,
) {
    private var version = 0
    var beforePut: (() -> Unit)? = null
    var beforeGet: (() -> Unit)? = null
    var puts = 0

    fun store(body: String) {
        text = body
        version += 1
        etag = if (noEtag) null else "\"v$version\""
    }

    fun bumpFromOutside(body: String) = store(body)
}

class FakeWebDavClient(
    private val remote: FakeRemote,
    override val maxBytes: Long = 20L * 1024 * 1024,
) : WebDavClient {
    override fun head(credentials: WebDavCredentials): RemoteFile =
        if (remote.text == null) {
            RemoteFile.Missing
        } else {
            RemoteFile(true, remote.etag, remote.text!!.length.toLong())
        }

    override fun get(credentials: WebDavCredentials): RemoteContent? {
        remote.beforeGet?.invoke()
        val text = remote.text ?: return null
        return RemoteContent(text, remote.etag, text.length.toLong())
    }

    override fun readPrefix(credentials: WebDavCredentials, maxBytes: Int): String? =
        remote.text?.take(maxBytes)

    override fun put(
        credentials: WebDavCredentials,
        body: String,
        condition: PutCondition,
        contentType: String,
    ): RemoteFile {
        remote.beforePut?.invoke()
        when (condition) {
            is PutCondition.IfMatch -> {
                if (remote.text == null || !Etag.equal(remote.etag, condition.etag)) throw precondition()
            }

            PutCondition.MustNotExist -> if (remote.text != null) throw precondition()
            PutCondition.Force -> Unit
        }
        remote.puts += 1
        remote.store(body)
        return RemoteFile(true, remote.etag, body.toByteArray(Charsets.UTF_8).size.toLong())
    }

    override fun ensureDirectories(credentials: WebDavCredentials) = Unit

    private fun precondition(): SyncException = SyncException(
        "precondition",
        SyncErrorCategory.PRECONDITION,
        "PRECONDITION_FAILED",
        412,
    )
}

class MemoryLocalState(initial: ReadingState) : LocalState {
    var state: ReadingState = initial
        private set
    private var revision = 0L

    override fun current(): ReadingState = state

    override fun snapshot(): LocalSnapshot = LocalSnapshot(ReadingStateCodec.encode(state), revision)

    override fun replaceIfUnchanged(snapshot: LocalSnapshot, state: ReadingState): Boolean {
        if (snapshot.revision != revision) return false
        this.state = state
        revision += 1
        return true
    }

    override fun replace(state: ReadingState) {
        this.state = state
        revision += 1
    }

    fun edit(transform: (ReadingState) -> ReadingState) {
        state = transform(state)
        revision += 1
    }
}

class TestClock(var instant: java.time.Instant) {
    fun advanceSeconds(seconds: Long) {
        instant = instant.plusSeconds(seconds)
    }
}
