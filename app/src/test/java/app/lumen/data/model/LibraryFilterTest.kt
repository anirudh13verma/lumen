package app.lumen.data.model

import org.junit.Assert.assertEquals
import org.junit.Test

class LibraryFilterTest {

    private fun item(title: String, kind: ItemKind) = LibraryItem(
        id = title, kind = kind, title = title, year = null, posterPath = null,
        fileCount = 1, totalSizeBytes = 1, addedAt = 0, matched = false,
    )

    private val items = listOf(
        item("Spider-Man: No Way Home", ItemKind.Movie),
        item("Severance", ItemKind.Show),
        item("Dune", ItemKind.Movie),
    )

    @Test
    fun chipFiltersByKind() {
        assertEquals(listOf("Severance"), filterItems(items, LibraryFilter.Shows, "").map { it.title })
        assertEquals(2, filterItems(items, LibraryFilter.Movies, "").size)
        assertEquals(3, filterItems(items, LibraryFilter.All, "  ").size)
    }

    @Test
    fun searchIgnoresCaseAndPunctuation() {
        assertEquals(listOf("Spider-Man: No Way Home"), filterItems(items, LibraryFilter.All, "spider man").map { it.title })
        assertEquals(listOf("Dune"), filterItems(items, LibraryFilter.All, "DUN").map { it.title })
    }

    @Test
    fun searchAndChipCombine() {
        assertEquals(emptyList<LibraryItem>(), filterItems(items, LibraryFilter.Shows, "dune"))
    }

    @Test
    fun seasonsAreGroupedAndSorted() {
        fun f(s: Int?, e: Int?, n: String) = ItemFile("u/$n", n, 1, s, e, null)
        val groups = groupBySeason(listOf(f(2, 1, "c"), f(1, 2, "b"), f(1, 1, "a"), f(null, null, "x")))
        assertEquals(listOf(0, 1, 2), groups.map { it.season })
        assertEquals(listOf("a", "b"), groups[1].episodes.map { it.name })
    }
}
