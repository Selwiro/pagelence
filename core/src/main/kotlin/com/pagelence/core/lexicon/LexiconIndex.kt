package com.pagelence.core.lexicon

import com.pagelence.core.content.PagelenceJson
import kotlinx.serialization.Serializable
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * The three global word tables behind one read-only index.
 *
 * The app ships `third-party/lexicon.bin` (built by `tools/build_lexicon.py`)
 * instead of parsing ~11 MB of JSON into object graphs on the first word tap:
 * that parse used to add about 95 MB of heap to a reading session and kept it
 * for the whole process. [LexiconIndex] reads the file once, binary-searches
 * the key bytes in place and decodes only the tapped entry (a short JSON
 * object), so the whole table costs roughly the file size and nothing else.
 */
interface Lexicon {
    val dictionary: DictionarySource?
    val levels: WordLevelSource?
    val frequencies: WordFrequencySource?
}

/**
 * A read-only `lexicon.bin` parsed in place.
 *
 * The byte array is retained as-is; key offsets are the only extra allocation
 * (four bytes per term). All lookups are allocation-light and safe to call
 * from any thread: the buffer is only read through absolute gets.
 */
class LexiconIndex private constructor(private val data: ByteArray) : Lexicon,
    DictionarySource, WordLevelSource, WordFrequencySource {

    private val buffer: ByteBuffer = ByteBuffer.wrap(data).order(ByteOrder.LITTLE_ENDIAN)

    private val dictionaryTable: Table
    private val levelsTable: Table
    private val frequencyTable: Table
    private val bands: FrequencyBands

    init {
        require(data.size >= HEADER_SIZE) { "lexicon.bin is truncated" }
        val magic = String(data, 0, MAGIC.length, Charsets.US_ASCII)
        require(magic == MAGIC) { "lexicon.bin has a bad magic: $magic" }
        val version = buffer.getInt(4)
        require(version == VERSION) { "lexicon.bin version $version, expected $VERSION" }
        val sectionCount = buffer.getInt(8)
        require(sectionCount >= 1) { "lexicon.bin has no sections" }

        var header = 12
        var body = 12 + sectionCount * SECTION_HEADER_SIZE
        var dict: Table? = null
        var lev: Table? = null
        var freq: Table? = null
        repeat(sectionCount) {
            val kind = buffer.getInt(header)
            val count = buffer.getInt(header + 4)
            val keysLength = buffer.getInt(header + 8)
            val valuesLength = buffer.getInt(header + 12)
            val blobLength = buffer.getInt(header + 16)
            header += SECTION_HEADER_SIZE
            require(count >= 0 && keysLength >= 0 && valuesLength >= 0 && blobLength >= 0) {
                "lexicon.bin has a negative section size"
            }
            require(body + keysLength + valuesLength + blobLength <= data.size) {
                "lexicon.bin section runs past the end of the file"
            }
            val table = Table(
                data = data,
                buffer = buffer,
                count = count,
                keysStart = body,
                valuesStart = body + keysLength,
                blobStart = body + keysLength + valuesLength,
            )
            when (kind) {
                KIND_DICTIONARY -> dict = table
                KIND_LEVELS -> lev = table
                KIND_FREQUENCIES -> freq = table
                else -> throw IllegalArgumentException("lexicon.bin has unknown section kind $kind")
            }
            body += keysLength + valuesLength + blobLength
        }
        dictionaryTable = requireNotNull(dict) { "lexicon.bin has no dictionary section" }
        levelsTable = requireNotNull(lev) { "lexicon.bin has no levels section" }
        frequencyTable = requireNotNull(freq) { "lexicon.bin has no frequencies section" }
        bands = frequencyTable.blobDouble(0).let { high ->
            FrequencyBands(
                high = high,
                common = frequencyTable.blobDouble(8),
                normal = frequencyTable.blobDouble(16),
            )
        }
    }

    override val dictionary: DictionarySource get() = this
    override val levels: WordLevelSource get() = this
    override val frequencies: WordFrequencySource get() = this

    override fun entry(term: String): DictionaryEntry? {
        val index = dictionaryTable.find(term)
        if (index < 0) return null
        return runCatching {
            PagelenceJson.decodeFromString<DictionaryEntry>(dictionaryTable.blobString(index))
        }.getOrNull()
    }

    override fun lookup(term: String): WordLevels? {
        val index = levelsTable.find(term)
        if (index < 0) return null
        val blob = runCatching {
            PagelenceJson.decodeFromString<LevelBlob>(levelsTable.blobString(index))
        }.getOrNull() ?: return null
        return WordLevels(blob.levels, blob.sources)
    }

    override fun zipf(term: String): Double? {
        val index = frequencyTable.find(term)
        if (index < 0) return null
        return frequencyTable.valueDouble(index)
    }

    override fun bandOf(zipf: Double): FrequencyBand = bands.bandOf(zipf)

    /** Number of dictionary terms, levels and frequency entries. */
    val dictionarySize: Int get() = dictionaryTable.count
    val levelSize: Int get() = levelsTable.count
    val frequencySize: Int get() = frequencyTable.count

    @Serializable
    private data class LevelBlob(
        val l: Map<String, String> = emptyMap(),
        val s: Map<String, String> = emptyMap(),
    ) {
        val levels: Map<String, String> get() = l
        val sources: Map<String, String> get() = s
    }

    /**
     * One section: `count` length-prefixed keys, `count` eight-byte values and
     * an optional blob. Keys are sorted by encoded bytes, so the search never
     * allocates a key object.
     */
    private class Table(
        private val data: ByteArray,
        private val buffer: ByteBuffer,
        val count: Int,
        keysStart: Int,
        private val valuesStart: Int,
        private val blobStart: Int,
    ) {
        /** Absolute offset of each key's length prefix. */
        private val keyOffsets = IntArray(count).also { offsets ->
            var position = keysStart
            for (index in 0 until count) {
                offsets[index] = position
                position += 4 + buffer.getInt(position)
            }
        }

        fun find(term: String): Int {
            val query = term.encodeToByteArray()
            var low = 0
            var high = count - 1
            while (low <= high) {
                val middle = (low + high) ushr 1
                val order = compareKey(middle, query)
                when {
                    order < 0 -> low = middle + 1
                    order > 0 -> high = middle - 1
                    else -> return middle
                }
            }
            return -1
        }

        private fun compareKey(index: Int, query: ByteArray): Int {
            val start = keyOffsets[index]
            val length = buffer.getInt(start)
            val offset = start + 4
            val shared = minOf(length, query.size)
            for (position in 0 until shared) {
                val stored = data[offset + position].toInt() and 0xFF
                val wanted = query[position].toInt() and 0xFF
                if (stored != wanted) return stored - wanted
            }
            return length - query.size
        }

        fun blobString(index: Int): String {
            val offset = buffer.getInt(valuesStart + index * VALUE_SIZE)
            val length = buffer.getInt(valuesStart + index * VALUE_SIZE + 4)
            return String(data, blobStart + offset, length, Charsets.UTF_8)
        }

        fun blobDouble(offset: Int): Double = buffer.getDouble(blobStart + offset)

        fun valueDouble(index: Int): Double =
            buffer.getDouble(valuesStart + index * VALUE_SIZE)
    }

    companion object {
        private const val MAGIC = "PLX1"
        private const val VERSION = 1
        private const val HEADER_SIZE = 12
        private const val SECTION_HEADER_SIZE = 20
        private const val VALUE_SIZE = 8
        private const val KIND_DICTIONARY = 1
        private const val KIND_LEVELS = 2
        private const val KIND_FREQUENCIES = 3

        /**
         * Parse the bytes read from `third-party/lexicon.bin`.
         *
         * @throws IllegalArgumentException when the file is truncated, has an
         *   unknown version or is missing a section. Callers that can live
         *   without the tables should catch it and fall back to the JSON
         *   tables (tests, tools) or to "not in the dictionary" (the app).
         */
        fun parse(data: ByteArray): LexiconIndex = LexiconIndex(data)
    }
}
