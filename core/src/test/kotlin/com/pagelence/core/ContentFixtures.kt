package com.pagelence.core

import java.nio.file.Path
import kotlin.io.path.readBytes
import kotlin.io.path.readText

/** Access to the real shipped content package and to test fixtures. */
object ContentFixtures {
    val contentDir: Path = Path.of(
        System.getProperty("pagelence.contentDir") ?: "../content",
    ).toAbsolutePath().normalize()

    /** The only book shipped: public domain worldwide, unlike the archived Web pilot. */
    val dolittle = "doctor-dolittle-1920"
    val alice = "alice-1866"

    fun resource(name: String): String {
        val stream = ContentFixtures::class.java.classLoader.getResourceAsStream(name)
            ?: error("missing test resource $name")
        return stream.bufferedReader().use { it.readText() }
    }

    fun read(path: String): String = contentDir.resolve(path).readText()

    fun readBytes(path: String): ByteArray = contentDir.resolve(path).readBytes()
}
