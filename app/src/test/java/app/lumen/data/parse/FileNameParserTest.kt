package app.lumen.data.parse

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class FileNameParserTest {

    // --- Movies ------------------------------------------------------------------------------

    @Test
    fun movieWithTypicalReleaseNoise() {
        val parsed = parseFileName("Dune.Part.Two.2024.1080p.WEB-DL.DDP5.1.Atmos.x265-GRP.mkv")
        assertEquals("Dune Part Two", parsed.title)
        assertEquals(2024, parsed.year)
        assertFalse(parsed.isEpisode)
    }

    @Test
    fun movieWithYearInParentheses() {
        val parsed = parseFileName("Wonder Woman 1984 (2020).mkv")
        assertEquals("Wonder Woman 1984", parsed.title)
        assertEquals(2020, parsed.year)
    }

    @Test
    fun lastYearWinsWhenTheTitleContainsAYear() {
        val parsed = parseFileName("Blade.Runner.2049.2017.1080p.BluRay.mkv")
        assertEquals("Blade Runner 2049", parsed.title)
        assertEquals(2017, parsed.year)
    }

    @Test
    fun aTitleThatIsOnlyAYearStaysTheTitle() {
        val parsed = parseFileName("2012.2009.720p.mkv")
        assertEquals("2012", parsed.title)
        assertEquals(2009, parsed.year)
    }

    @Test
    fun implausibleYearsAreNotYears() {
        val parsed = parseFileName("Blade.Runner.2049.1080p.mkv")
        assertEquals("Blade Runner 2049", parsed.title)
        assertNull(parsed.year)
        assertEquals("Movie 1899", parseFileName("Movie 1899.mkv").title)
    }

    @Test
    fun leadingParenthesisInATitleSurvives() {
        val parsed = parseFileName("(500) Days of Summer (2009).mkv")
        assertEquals("(500) Days of Summer", parsed.title)
        assertEquals(2009, parsed.year)
    }

    @Test
    fun hyphenatedTitlesKeepTheirHyphen() {
        val parsed = parseFileName("Spider-Man.No.Way.Home.2021.2160p.mkv")
        assertEquals("Spider-Man No Way Home", parsed.title)
        assertEquals(2021, parsed.year)
    }

    @Test
    fun ordinaryWordsAreNotMistakenForReleaseNoise() {
        val parsed = parseFileName("The.Web.of.Lies.2019.mkv")
        assertEquals("The Web of Lies", parsed.title)
        assertEquals(2019, parsed.year)
    }

    @Test
    fun noiseWithoutYear() {
        val parsed = parseFileName("Movie.Title.1080p.mkv")
        assertEquals("Movie Title", parsed.title)
        assertNull(parsed.year)
    }

    @Test
    fun nameWithoutKnownExtensionIsStillParsed() {
        val parsed = parseFileName("Movie.Name.2024")
        assertEquals("Movie Name", parsed.title)
        assertEquals(2024, parsed.year)
    }

    // --- Episodes ----------------------------------------------------------------------------

    @Test
    fun standardEpisodeMarker() {
        val parsed = parseFileName("Severance.S02E03.1080p.WEB-DL.mkv")
        assertEquals("Severance", parsed.title)
        assertEquals(2, parsed.season)
        assertEquals(3, parsed.episode)
        assertNull(parsed.episodeEnd)
        assertTrue(parsed.isEpisode)
    }

    @Test
    fun episodeTitleAfterTheMarkerIsIgnored() {
        val parsed = parseFileName("The.Last.of.Us.S01E02.Infected.1080p.mkv")
        assertEquals("The Last of Us", parsed.title)
        assertEquals(1, parsed.season)
        assertEquals(2, parsed.episode)
    }

    @Test
    fun multiEpisodeFiles() {
        assertEquals(3, parseFileName("Show Name S01E02E03.mkv").episodeEnd)
        assertEquals(6, parseFileName("Show.S01E05-06.mkv").episodeEnd)
        assertEquals(6, parseFileName("Show.S01E05-E06.mkv").episodeEnd)
    }

    @Test
    fun oneXTwoStyleMarker() {
        val parsed = parseFileName("Show.1x04.mkv")
        assertEquals("Show", parsed.title)
        assertEquals(1, parsed.season)
        assertEquals(4, parsed.episode)
    }

    @Test
    fun resolutionIsNotAnEpisodeMarker() {
        assertFalse(parseFileName("Some Film 1920x1080.mkv").isEpisode)
    }

    @Test
    fun longFormMarker() {
        val parsed = parseFileName("Show Season 3 Episode 7.mkv")
        assertEquals("Show", parsed.title)
        assertEquals(3, parsed.season)
        assertEquals(7, parsed.episode)
    }

    @Test
    fun yearBeforeTheEpisodeMarkerIsKept() {
        val parsed = parseFileName("Doctor.Who.2005.S01E01.mkv")
        assertEquals("Doctor Who", parsed.title)
        assertEquals(2005, parsed.year)
        assertEquals(1, parsed.episode)
    }

    @Test
    fun dottedAbbreviationsBecomeSpaces() {
        assertEquals("Mr Robot", parseFileName("Mr. Robot S01E01.mkv").title)
    }

    @Test
    fun fansubNamingWithGroupTag() {
        val parsed = parseFileName("[SubsPlease] Sousou no Frieren - 12 (1080p) [ABCD1234].mkv")
        assertEquals("Sousou no Frieren", parsed.title)
        assertEquals(1, parsed.season)
        assertEquals(12, parsed.episode)
    }

    @Test
    fun dashNumberWithoutGroupTagIsNotAnEpisode() {
        // Too ambiguous ("Apollo - 13"), so it stays a plain title.
        assertFalse(parseFileName("Apollo - 13.mkv").isEpisode)
    }

    @Test
    fun dashNumberIsAnEpisodeWhenTheFolderNamesTheSameShow() {
        val parsed = parseFileName("Attack on Titan - 87.mkv", "Anime/Attack on Titan")
        assertEquals("Attack on Titan", parsed.title)
        assertEquals(1, parsed.season)
        assertEquals(87, parsed.episode)
    }

    @Test
    fun dashNumberStaysATitleWhenTheFolderIsSomethingElse() {
        assertFalse(parseFileName("Apollo - 13.mkv", "Movies/Space").isEpisode)
    }

    // --- Folder context ----------------------------------------------------------------------

    @Test
    fun titleComesFromTheFolderWhenTheFileHasNone() {
        val parsed = parseFileName("S02E03.mkv", "Shows/Severance (2022)/Season 2")
        assertEquals("Severance", parsed.title)
        assertEquals(2022, parsed.year)
        assertEquals(2, parsed.season)
        assertEquals(3, parsed.episode)
    }

    @Test
    fun bareEpisodeNumberTakesItsSeasonFromTheFolder() {
        val parsed = parseFileName("E03.mkv", "Shows/Severance/Season 2")
        assertEquals("Severance", parsed.title)
        assertEquals(2, parsed.season)
        assertEquals(3, parsed.episode)
    }

    @Test
    fun genericFileNameUsesTheMovieFolder() {
        val parsed = parseFileName("movie.mkv", "Movies/Dune (2021)")
        assertEquals("Dune", parsed.title)
        assertEquals(2021, parsed.year)
        assertFalse(parsed.isEpisode)
    }

    @Test
    fun fileTitleBeatsFolderTitle() {
        val parsed = parseFileName("Heat.1995.mkv", "Movies/Crime")
        assertEquals("Heat", parsed.title)
        assertEquals(1995, parsed.year)
    }

    // --- normalizeTitle ----------------------------------------------------------------------

    @Test
    fun normalizeTitleIgnoresCasePunctuationAndSpacing() {
        assertEquals("spider man no way home", normalizeTitle("Spider-Man: No Way Home"))
        assertEquals("dune part two", normalizeTitle("  DUNE:  Part Two "))
        assertEquals("기생충", normalizeTitle("기생충!"))
    }

    @Test
    fun apostrophesDoNotSplitWords() {
        assertEquals(
            normalizeTitle("Marvel's Agents of S.H.I.E.L.D."),
            normalizeTitle(parseFileName("Marvels.Agents.of.S.H.I.E.L.D.S02E01.mkv").title),
        )
        assertEquals(normalizeTitle("Don't Look Up"), normalizeTitle("Don’t Look Up"))
    }
}
