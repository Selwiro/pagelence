package com.pagelence.presentation.text

/** A word hit inside one sentence: the exact spelling plus its offsets. */
data class WordHit(val text: String, val start: Int, val end: Int)

/**
 * 点词落点换算：把「字符偏移」扩成完整的英文单词。
 *
 * 规则与普通版一致：字母、撇号（直/弯）与连字符算词内字符；点在标点或空白上
 * 时回退一格再取词；结果里必须有字母，否则当作没点中。
 */
object WordBoundary {
    fun wordAt(text: String, index: Int): WordHit? {
        if (text.isEmpty()) return null
        fun isWordChar(c: Char) = c.isLetter() || c == '\'' || c == '’' || c == '-'
        var start = index.coerceIn(0, text.lastIndex)
        var end = start
        if (!isWordChar(text[start])) {
            if (start > 0 && isWordChar(text[start - 1])) {
                start -= 1
                end = start
            } else {
                return null
            }
        }
        while (start > 0 && isWordChar(text[start - 1])) start -= 1
        while (end < text.lastIndex && isWordChar(text[end + 1])) end += 1
        val word = text.substring(start, end + 1)
        return if (word.any { it.isLetter() }) WordHit(word, start, end) else null
    }
}
