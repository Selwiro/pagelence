package com.pagelence

import android.content.Context
import java.io.File

/**
 * Ships the content package inside the APK (assets come from `work/android/content`)
 * and copies it into app storage on first launch or when the book index changes.
 * `:core` stays platform-free by reading a plain directory.
 */
object ContentSeed {
    /**
     * Files whose bytes decide whether the seeded copy is up to date. Book
     * packages and the shared dictionaries are listed from the asset tree, so
     * editing a book or regenerating a dictionary also refreshes the copy.
     */
    private fun stamps(context: Context): List<String> = buildList {
        add("books-index.json")
        add("store.json")
        add("taxonomy.json")
        add("assessment/bank.json")
        add("grammar/grammar-library.json")
        add("third-party/word-levels.json")
        context.assets.list("books").orEmpty().forEach { book ->
            add("books/$book/book.json")
            // Optional; a missing asset is skipped by the caller's open() probe.
            add("books/$book/cover.jpg")
        }
        context.assets.list("third-party/dictionaries").orEmpty().forEach { name ->
            if (name.endsWith(".json")) add("third-party/dictionaries/$name")
        }
    }

    fun ensure(context: Context): File {
        val target = File(context.filesDir, "content")
        val fresh = stamps(context).all { name ->
            val asset = runCatching { context.assets.open(name).use { it.readBytes() } }.getOrNull() ?: return@all true
            val stored = File(target, name)
            stored.isFile && stored.readBytes().contentEquals(asset)
        }
        if (target.isDirectory && fresh) return target
        target.deleteRecursively()
        target.mkdirs()
        copyTree(context, "", target)
        return target
    }

    private fun copyTree(context: Context, path: String, target: File) {
        val assets = context.assets
        val children = assets.list(path).orEmpty()
        for (child in children) {
            val childPath = if (path.isEmpty()) child else "$path/$child"
            val nested = assets.list(childPath).orEmpty()
            if (nested.isNotEmpty()) {
                val directory = File(target, child)
                directory.mkdirs()
                copyTree(context, childPath, directory)
            } else {
                val file = File(target, child)
                file.parentFile?.mkdirs()
                assets.open(childPath).use { input ->
                    file.outputStream().use { output -> input.copyTo(output) }
                }
            }
        }
    }
}
