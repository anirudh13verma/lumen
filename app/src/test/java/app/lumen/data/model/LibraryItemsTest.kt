package app.lumen.data.model

import app.lumen.data.db.MediaRow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class LibraryItemsTest {

    private fun row(
        name: String,
        path: String = "",
        size: Long = 100,
        addedAt: Long = 1,
        mediaKey: String? = null,
        kind: String? = null,
        title: String? = null,
        year: Int? = null,
        poster: String? = null,
    ) = MediaRow(
        documentUri = "content://test/$path/$name",
        name = name,
        relativePath = path,
        sizeBytes = size,
        lastModified = 0,
        addedAt = addedAt,
        mediaKey = mediaKey,
        kind = kind,
        metaTitle = title,
        metaYear = year,
        posterPath = poster,
    )

    @Test
    fun matchedEpisodesShareOneShowCard() {
        val items = buildLibraryItems(
            listOf(
                row("Severance.S01E01.mkv", size = 10, addedAt = 5, mediaKey = "tv:1", kind = "tv", title = "Severance", year = 2022, poster = "/p.jpg"),
                row("Severance.S01E02.mkv", size = 20, addedAt = 9, mediaKey = "tv:1", kind = "tv", title = "Severance", year = 2022, poster = "/p.jpg"),
            ),
        )
        assertEquals(1, items.size)
        val show = items.single()
        assertEquals(ItemKind.Show, show.kind)
        assertEquals("Severance", show.title)
        assertEquals(2022, show.year)
        assertEquals("/p.jpg", show.posterPath)
        assertEquals(2, show.fileCount)
        assertEquals(30L, show.totalSizeBytes)
        assertEquals(9L, show.addedAt)
        assertTrue(show.matched)
    }

    @Test
    fun anUnmatchedEpisodeJoinsItsAlreadyMatchedShow() {
        val items = buildLibraryItems(
            listOf(
                row("Severance.S01E01.mkv", mediaKey = "tv:1", kind = "tv", title = "Severance", poster = "/p.jpg"),
                row("Severance.S01E03.mkv"), // newly added, not looked up yet
            ),
        )
        assertEquals(1, items.size)
        assertEquals(2, items.single().fileCount)
        assertTrue(items.single().matched)
    }

    @Test
    fun unmatchedEpisodesAreGroupedByShowTitle() {
        val items = buildLibraryItems(
            listOf(
                row("Obscure.Show.S01E01.mkv"),
                row("Obscure.Show.S01E02.mkv"),
                row("Obscure.Show.S02E01.mkv"),
            ),
        )
        assertEquals(1, items.size)
        assertEquals(ItemKind.Show, items.single().kind)
        assertEquals("Obscure Show", items.single().title)
        assertEquals(3, items.single().fileCount)
        assertFalse(items.single().matched)
        assertNull(items.single().posterPath)
    }

    @Test
    fun episodesFoundByFolderOnlyAreGroupedWithTheirShow() {
        val items = buildLibraryItems(
            listOf(
                row("S01E01.mkv", path = "Shows/Odd Show/Season 1"),
                row("S01E02.mkv", path = "Shows/Odd Show/Season 1"),
            ),
        )
        assertEquals(1, items.size)
        assertEquals("Odd Show", items.single().title)
    }

    @Test
    fun anUnmatchedMovieIsACardOfItsOwn() {
        val items = buildLibraryItems(
            listOf(
                row("Some.Film.2020.1080p.mkv"),
                row("Another.Film.2021.mkv"),
            ),
        )
        assertEquals(2, items.size)
        assertEquals(setOf("Some Film", "Another Film"), items.map { it.title }.toSet())
        assertTrue(items.all { it.kind == ItemKind.Movie })
        assertEquals(2020, items.first { it.title == "Some Film" }.year)
    }

    @Test
    fun twoVersionsOfAMatchedMovieShareACard() {
        val items = buildLibraryItems(
            listOf(
                row("Dune.2021.1080p.mkv", mediaKey = "movie:438631", kind = "movie", title = "Dune", year = 2021),
                row("Dune.2021.2160p.mkv", mediaKey = "movie:438631", kind = "movie", title = "Dune", year = 2021),
            ),
        )
        assertEquals(1, items.size)
        assertEquals(ItemKind.Movie, items.single().kind)
        assertEquals(2, items.single().fileCount)
    }

    @Test
    fun matchedTitleOverridesTheFileName() {
        val items = buildLibraryItems(
            listOf(row("dune.part.two.2024.mkv", mediaKey = "movie:693134", kind = "movie", title = "Dune: Part Two", year = 2024)),
        )
        assertEquals("Dune: Part Two", items.single().title)
    }

    @Test
    fun emptyInputGivesNoItems() {
        assertTrue(buildLibraryItems(emptyList()).isEmpty())
    }
}
