package com.pagelence.core.assessment

import com.pagelence.core.content.ContentSource
import com.pagelence.core.content.FileContentSource
import com.pagelence.core.content.PagelenceJson
import java.nio.file.Path

/** Read access to the shipped question bank. */
interface AssessmentRepository {
    fun bank(): AssessmentBank
}

/** Loads `assessment/bank.json`; an absent file yields an empty bank. */
class FileAssessmentRepository(private val source: ContentSource) : AssessmentRepository {
    /** Convenience for tests and file-based tools. */
    constructor(contentDir: Path) : this(FileContentSource(contentDir))

    private val loaded: AssessmentBank by lazy {
        if (!source.exists("assessment/bank.json")) AssessmentBank()
        else PagelenceJson.decodeFromString<AssessmentBank>(source.readText("assessment/bank.json"))
    }

    override fun bank(): AssessmentBank = loaded
}

/** In-memory bank for tests and previews. */
class InMemoryAssessmentRepository(private val value: AssessmentBank) : AssessmentRepository {
    override fun bank(): AssessmentBank = value
}
