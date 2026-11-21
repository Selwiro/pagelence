package com.pagelence.core.content

import java.nio.file.Files
import kotlin.io.path.writeText
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class StoreCatalogTest {
    private fun book(
        id: String,
        order: Int,
        status: String,
        title: String? = null,
        level: String? = null,
    ) = BuiltInStoreBook(
        id = id,
        order = order,
        status = status,
        title = title?.let { LocalizedText(zh = it) },
        level = level,
        levelSource = level?.let { "estimated" },
    )

    @Test
    fun `retired books never reach the shelf`() {
        val visible = StoreCatalogRules.visible(
            listOf(
                book("old", 0, StoreCatalogRules.STATUS_RETIRED),
                book("live", 2, StoreCatalogRules.STATUS_AVAILABLE),
            ),
        )
        assertEquals(listOf("live"), visible.map { it.id })
    }

    @Test
    fun `available books come before planned and keep their order`() {
        val visible = StoreCatalogRules.visible(
            listOf(
                book("planned-b", 5, StoreCatalogRules.STATUS_PLANNED, title = "B", level = "B2"),
                book("available-b", 3, StoreCatalogRules.STATUS_AVAILABLE),
                book("available-a", 1, StoreCatalogRules.STATUS_AVAILABLE),
                book("planned-a", 4, StoreCatalogRules.STATUS_PLANNED, title = "A", level = "B1"),
            ),
        )
        assertEquals(listOf("available-a", "available-b", "planned-a", "planned-b"), visible.map { it.id })
    }

    @Test
    fun `a missing store file yields an empty shelf`() {
        val root = Files.createTempDirectory("store")
        assertTrue(FileBuiltInStoreCatalog(root).books().isEmpty())
    }

    @Test
    fun `store v2 parses curation and planned identity`() {
        val root = Files.createTempDirectory("store")
        root.resolve("store.json").writeText(
            """
            {
              "schemaVersion": 2,
              "updatedAt": "2026-11-20",
              "books": [
                {"id":"alice-1866","order":2,"status":"available","monogram":"爱"},
                {"id":"four","order":3,"status":"planned","title":{"zh":"四签名"},
                 "author":"Arthur Conan Doyle","level":"B2","levelSource":"estimated",
                 "summary":{"zh":"钩子"}}
              ]
            }
            """.trimIndent(),
        )
        val catalog = FileBuiltInStoreCatalog(root).catalog()
        assertEquals(2, catalog.schemaVersion)
        val available = catalog.books.first()
        assertNull(available.title)
        assertNull(available.author)
        assertNull(available.level)
        val planned = catalog.books.last()
        assertEquals("B2", planned.level)
        assertEquals("estimated", planned.levelSource)
    }
}
