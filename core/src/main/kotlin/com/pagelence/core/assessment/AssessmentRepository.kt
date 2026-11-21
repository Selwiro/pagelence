package com.pagelence.core.assessment

import com.pagelence.core.content.PagelenceJson
import java.nio.file.Path
import kotlin.io.path.exists
import kotlin.io.path.readText

/** Read access to the shipped question bank. */
interface AssessmentRepository {
    fun bank(): AssessmentBank
}

/** Loads `content/assessment/bank.json`; an absent file yields an empty bank. */
class FileAssessmentRepository(private val contentDir: Path) : AssessmentRepository {
    private val loaded: AssessmentBank by lazy {
        val path = contentDir.resolve("assessment/bank.json")
        if (!path.exists()) AssessmentBank()
        else PagelenceJson.decodeFromString<AssessmentBank>(path.readText(Charsets.UTF_8))
    }

    override fun bank(): AssessmentBank = loaded
}

/** In-memory bank for tests and previews. */
class InMemoryAssessmentRepository(private val value: AssessmentBank) : AssessmentRepository {
    override fun bank(): AssessmentBank = value
}
