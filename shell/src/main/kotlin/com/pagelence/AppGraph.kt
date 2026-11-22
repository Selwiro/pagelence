package com.pagelence

import android.content.Context
import android.net.Uri
import com.pagelence.core.assessment.FileAssessmentRepository
import com.pagelence.core.content.AssetContentSource
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.ContentSource
import com.pagelence.core.content.FileBuiltInStoreCatalog
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.content.FileContentSource
import com.pagelence.core.dict.DictManifest
import com.pagelence.core.dict.DictManifestCodec
import com.pagelence.core.dict.FullDictInstaller
import com.pagelence.core.lexicon.LexiconIndex
import com.pagelence.core.source.DownloadedBooks
import com.pagelence.core.source.SourceLimits
import com.pagelence.core.source.SourceManager
import com.pagelence.core.source.SourceStore
import com.pagelence.data.ReadingStateStore
import com.pagelence.data.StateAnnotationRepository
import com.pagelence.data.StateLookupHistoryRepository
import com.pagelence.data.StatePreferencesRepository
import com.pagelence.data.StateProgressRepository
import com.pagelence.data.StateReadingProfileRepository
import com.pagelence.data.StateWordCardRepository
import com.pagelence.sync.OkHttpWebDavClient
import com.pagelence.sync.StateTransfer
import com.pagelence.sync.SyncBaselineStore
import com.pagelence.sync.SyncEngine
import com.pagelence.sync.SyncStateStore
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 手工依赖装配：两个壳共享 `:core`，Hilt 的收益不足（见 android-design.md 第五节）。
 * 这里只接线，不含业务规则。
 */
class AppGraph(context: Context, val config: ShellConfig = ShellConfig("0.0.0", 0)) {
    private val appContext = context.applicationContext

    val store: ReadingStateStore by lazy {
        ReadingStateStore(File(appContext.filesDir, "state.json"))
    }

    val cards: StateWordCardRepository by lazy { StateWordCardRepository(store) }

    /** Sentence-level bookmarks/highlights/notes, tombstones included. */
    val annotations: StateAnnotationRepository by lazy { StateAnnotationRepository(store) }

    /** Recent word lookups, one row per word per book (T161). */
    val lookups: StateLookupHistoryRepository by lazy { StateLookupHistoryRepository(store) }

    /** WebDAV settings and the Keystore-encrypted application password. */
    val syncCredentials: SyncCredentialStore by lazy { SyncCredentialStore(appContext) }

    /** ETag / baseline / failure state; one file each so a reset is local. */
    val syncEngine: SyncEngine by lazy {
        SyncEngine(
            client = OkHttpWebDavClient(),
            local = store,
            stateStore = SyncStateStore(File(appContext.filesDir, "sync-state.json").toPath()),
            baselineStore = SyncBaselineStore(File(appContext.filesDir, "sync-baseline.json").toPath()),
            backupDir = File(appContext.filesDir, "sync-backups").toPath(),
        )
    }

    val progress: StateProgressRepository by lazy { StateProgressRepository(store) }

    /** Android-free reading-session rules shared with the future e-ink shell. */
    val readerSession: com.pagelence.presentation.reader.ReaderSession by lazy {
        com.pagelence.presentation.reader.ReaderSession(progress = progress, profile = profile)
    }

    val preferences: StatePreferencesRepository by lazy { StatePreferencesRepository(store) }

    val profile: StateReadingProfileRepository by lazy { StateReadingProfileRepository(store) }

    val sourceStore: SourceStore by lazy {
        SourceStore(File(appContext.filesDir, "source-settings.json").toPath())
    }

    val downloads: DownloadedBooks by lazy {
        DownloadedBooks(File(appContext.filesDir, "downloaded").toPath())
    }

    val sourceManager: SourceManager by lazy {
        SourceManager(
            fetcher = AndroidHttpFetcher { sourceStore.settings().allowLocalHttp },
            settingsStore = sourceStore,
            downloads = downloads,
            cacheDir = File(appContext.filesDir, "source-cache").toPath(),
            appVersionCode = config.appVersionCode,
            shippedBookIds = { shippedBookIds },
        )
    }

    /**
     * The package that ships inside the APK, read through `AssetManager`.
     * Nothing is copied to `filesDir`: the APK assets are the single copy, so
     * an update never re-copies 13 MB and the install does not pay for it twice.
     */
    private val shippedContent: ContentSource by lazy {
        AssetContentSource(AssetManagerReader(appContext.assets))
    }

    /**
     * The three global word tables as one binary index. Read on the first word
     * tap (~11 MB) instead of parsing ~11 MB of JSON into object graphs, which
     * used to add ~95 MB of heap. A missing or stale file only disables
     * dictionary lookups; the reader itself keeps working.
     */
    private val lexiconIndex: LexiconIndex? by lazy {
        runCatching {
            LexiconIndex.parse(shippedContent.readBytes("third-party/lexicon.bin"))
        }.getOrNull()
    }

    /** Book ids that ship inside the APK, read from the shipped manifest. */
    private val shippedBookIds: Set<String> by lazy {
        runCatching {
            FileContentRepository.loadManifest(shippedContent).books.map { it.id }.toSet()
        }.getOrDefault(emptySet())
    }

    /**
     * Read a user-picked `.pagelence` document, stopping one byte past the
     * package limit so an oversized file is rejected without buffering it all.
     */
    fun readPackageFile(uri: Uri): ByteArray? = runCatching {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                total += read
                if (total > SourceLimits.MAX_PACKAGE_BYTES) break
                output.write(chunk, 0, read)
            }
            output.toByteArray()
        }
    }.getOrNull()

    /** Write an exported progress file to the document the user picked. */
    fun writeTextDocument(uri: Uri, text: String): Boolean = runCatching {
        appContext.contentResolver.openOutputStream(uri)?.use { stream ->
            stream.write(text.toByteArray(Charsets.UTF_8))
            stream.flush()
        } != null
    }.getOrDefault(false)

    /** Read an imported progress file, capped at the local-import limit. */
    fun readTextDocument(
        uri: Uri,
        maxBytes: Long = StateTransfer.MAX_IMPORT_BYTES,
    ): TextDocumentRead {
        return try {
            val stream = appContext.contentResolver.openInputStream(uri)
                ?: return TextDocumentRead.Failed
            stream.use {
                val output = ByteArrayOutputStream()
                val chunk = ByteArray(16 * 1024)
                var total = 0L
                while (true) {
                    val read = it.read(chunk)
                    if (read <= 0) break
                    total += read
                    if (total > maxBytes) return TextDocumentRead.TooLarge
                    output.write(chunk, 0, read)
                }
                TextDocumentRead.Ok(output.toString(Charsets.UTF_8.name()))
            }
        } catch (_: Exception) {
            TextDocumentRead.Failed
        }
    }

    // --- optional full ECDICT table (T123) -------------------------------

    private val dictionaryDir: File by lazy { File(appContext.filesDir, "dictionaries") }

    /** The pinned `dict-manifest.json` shipped in `content/`, if present. */
    val fullDictManifest: DictManifest? by lazy {
        runCatching {
            if (!shippedContent.exists("dict-manifest.json")) return@runCatching null
            DictManifestCodec.decode(shippedContent.readText("dict-manifest.json"))
        }.getOrNull()
    }

    /** Read-only SQLite view of the downloaded table; misses until installed. */
    val fullDictionary: SqliteDictionarySource by lazy {
        SqliteDictionarySource { File(dictionaryDir, "full-dict.sqlite") }
    }

    val fullDictInstaller: FullDictInstaller by lazy {
        FullDictInstaller(
            dir = dictionaryDir.toPath(),
            transport = AndroidDictTransport { sourceStore.settings().allowLocalHttp },
        )
    }

    /** True on Wi-Fi/ethernet; unknown counts as metered so the reader is asked. */
    fun dictionaryNetworkUnmetered(): Boolean = DictionaryNetwork.isUnmetered(appContext)

    val content: ContentRepository by lazy {
        FileContentRepository(
            shipped = shippedContent,
            cards = cards,
            extra = FileContentSource(downloads.dir),
            fullDictionary = fullDictionary,
            lexicon = { lexiconIndex },
        )
    }

    /** Re-read the content manifests after a download or removal. */
    fun refreshContent() {
        (content as? FileContentRepository)?.refresh()
    }

    val assessment: FileAssessmentRepository by lazy {
        FileAssessmentRepository(shippedContent)
    }

    /** Planned built-in shelf; availability still comes from the content manifest. */
    val builtInCatalog: FileBuiltInStoreCatalog by lazy {
        FileBuiltInStoreCatalog(shippedContent)
    }

    val speech: SpeechController by lazy { SpeechController(appContext) }
}

/** Outcome of reading a user-picked document; oversize is a distinct case. */
sealed interface TextDocumentRead {
    data class Ok(val text: String) : TextDocumentRead
    data object TooLarge : TextDocumentRead
    data object Failed : TextDocumentRead
}
