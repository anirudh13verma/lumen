package app.lumen.data.meta

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class TitleMatchingTest {

    private fun match(
        id: Int,
        title: String,
        year: Int?,
        originalTitle: String? = null,
        kind: MediaKind = MediaKind.Movie,
    ) = TmdbMatch(kind, id, title, originalTitle, year, null, null, null, null)

    @Test
    fun exactTitleWithMatchingYearWins() {
        val results = listOf(
            match(1, "Dune: Part Two", 2024),
            match(2, "Dune", 2021),
            match(3, "Dune", 1984),
        )
        assertEquals(3, pickBest(results, "Dune", 1984)?.id)
        assertEquals(2, pickBest(results, "Dune", 2021)?.id)
    }

    @Test
    fun exactTitleWithoutYearTakesTmdbsOrdering() {
        val results = listOf(match(2, "Dune", 2021), match(3, "Dune", 1984))
        assertEquals(2, pickBest(results, "Dune", null)?.id)
    }

    @Test
    fun yearIsAllowedToBeOffByOne() {
        // File dates and TMDB release dates often disagree around New Year and festival releases.
        assertEquals(5, pickBest(listOf(match(5, "Some Film", 2020)), "Some Film", 2019)?.id)
    }

    @Test
    fun punctuationAndCaseDoNotMatter() {
        val results = listOf(match(7, "Spider-Man: No Way Home", 2021))
        assertEquals(7, pickBest(results, "spider man no way home", 2021)?.id)
    }

    @Test
    fun originalTitleCounts() {
        val results = listOf(match(9, "Parasite", 2019, originalTitle = "기생충"))
        assertEquals(9, pickBest(results, "기생충", 2019)?.id)
    }

    @Test
    fun aSlightlyLongerTopResultIsAcceptedWhenNothingContradictsIt() {
        val results = listOf(match(4, "The Avengers", 2012))
        assertEquals(4, pickBest(results, "Avengers", null)?.id)
    }

    @Test
    fun aSequelIsNotAcceptedForTheOriginalWhenThereIsNoYear() {
        val results = listOf(match(1, "Dune: Part Two", 2024))
        assertNull(pickBest(results, "Dune", null))
    }

    @Test
    fun relatedTitleWithAWrongYearIsRejected() {
        val results = listOf(match(1, "The Avengers", 1998))
        assertNull(pickBest(results, "Avengers", 2012))
    }

    @Test
    fun unrelatedTopResultIsRejected() {
        val results = listOf(match(1, "Completely Different", 2020))
        assertNull(pickBest(results, "Severance", 2022))
    }

    @Test
    fun emptyInputsGiveNoMatch() {
        assertNull(pickBest(emptyList(), "Dune", 2021))
        assertNull(pickBest(listOf(match(1, "Dune", 2021)), "   ", 2021))
    }

    @Test
    fun mediaKeyIncludesKind() {
        assertEquals("movie:603", match(603, "The Matrix", 1999).mediaKey)
        assertEquals("tv:1396", match(1396, "Breaking Bad", 2008, kind = MediaKind.Tv).mediaKey)
    }

    @Test
    fun imageUrlsAreBuiltFromTmdbPaths() {
        assertEquals("https://image.tmdb.org/t/p/w342/abc.jpg", TmdbImages.poster("/abc.jpg"))
        assertNull(TmdbImages.poster(null))
    }
}
