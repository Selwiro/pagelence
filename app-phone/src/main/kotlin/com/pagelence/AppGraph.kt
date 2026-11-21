package com.pagelence

import android.content.Context
import android.net.Uri
import com.pagelence.core.assessment.FileAssessmentRepository
import com.pagelence.core.content.ContentRepository
import com.pagelence.core.content.FileBuiltInStoreCatalog
import com.pagelence.core.content.FileContentRepository
import com.pagelence.core.source.DownloadedBooks
import com.pagelence.core.source.SourceLimits
import com.pagelence.core.source.SourceManager
import com.pagelence.core.source.SourceStore
import com.pagelence.data.ReadingStateStore
import com.pagelence.data.StatePreferencesRepository
import com.pagelence.data.StateProgressRepository
import com.pagelence.data.StateReadingProfileRepository
import com.pagelence.data.StateWordCardRepository
import com.pagelence.sync.OkHttpWebDavClient
import com.pagelence.sync.SyncBaselineStore
import com.pagelence.sync.SyncEngine
import com.pagelence.sync.SyncStateStore
import java.io.ByteArrayOutputStream
import java.io.File

/**
 * 手工依赖装配：两个壳共享 `:core`，Hilt 的收益不足（见 android-design.md 第五节）。
 * 这里只接线，不含业务规则。
 */
class AppGraph(context: Context) {
    private val appContext = context.applicationContext

    val store: ReadingStateStore by lazy {
        ReadingStateStore(File(appContext.filesDir, "state.json"))
    }

    val cards: StateWordCardRepository by lazy { StateWordCardRepository(store) }

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
            appVersionCode = BuildConfig.VERSION_CODE,
            shippedBookIds = { shippedBookIds },
        )
    }

    private val contentDir: File by lazy { ContentSeed.ensure(appContext) }

    /** Book ids that ship inside the APK, read from the seeded content manifest. */
    private val shippedBookIds: Set<String> by lazy {
        runCatching {
            FileContentRepository.loadManifest(contentDir.toPath()).books.map { it.id }.toSet()
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

    /** Read an imported progress file, capped at the same limit WebDAV accepts. */
    fun readTextDocument(uri: Uri, maxBytes: Long = 20L * 1024 * 1024): String? = runCatching {
        appContext.contentResolver.openInputStream(uri)?.use { stream ->
            val output = ByteArrayOutputStream()
            val chunk = ByteArray(16 * 1024)
            var total = 0L
            while (true) {
                val read = stream.read(chunk)
                if (read <= 0) break
                total += read
                if (total > maxBytes) return null
                output.write(chunk, 0, read)
            }
            output.toString(Charsets.UTF_8.name())
        }
    }.getOrNull()

    val content: ContentRepository by lazy {
        FileContentRepository(contentDir.toPath(), cards, downloads.dir)
    }

    /** Re-read the content manifests after a download or removal. */
    fun refreshContent() {
        (content as? FileContentRepository)?.refresh()
    }

    val assessment: FileAssessmentRepository by lazy {
        FileAssessmentRepository(contentDir.toPath())
    }

    /** Planned built-in shelf; availability still comes from the content manifest. */
    val builtInCatalog: FileBuiltInStoreCatalog by lazy {
        FileBuiltInStoreCatalog(contentDir.toPath())
    }

    val speech: SpeechController by lazy { SpeechController(appContext) }
}
