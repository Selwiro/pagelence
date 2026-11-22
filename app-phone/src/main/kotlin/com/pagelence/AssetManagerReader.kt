package com.pagelence

import android.content.res.AssetManager
import com.pagelence.core.content.AssetReader
import java.io.InputStream

/**
 * The `AssetManager` half of [com.pagelence.core.content.AssetContentSource].
 *
 * Keeping this adapter in the app shell lets `:core` read a package straight
 * from the APK without depending on Android; `AssetContentSource` itself is
 * unit-tested on a plain JVM with a fake reader.
 */
class AssetManagerReader(private val assets: AssetManager) : AssetReader {
    override fun open(path: String): InputStream = assets.open(path)

    override fun list(path: String): List<String> = assets.list(path)?.toList().orEmpty()
}
