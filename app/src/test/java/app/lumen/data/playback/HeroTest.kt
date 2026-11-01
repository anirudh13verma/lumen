package app.lumen.data.playback

import app.lumen.data.meta.LogoCandidate
import app.lumen.data.meta.pickLogo
import app.lumen.data.model.ItemFile
import app.lumen.data.model.ItemKind
import app.lumen.data.model.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class HeroTest {

    private fun item(id: String, addedAt: Long, backdrop: String? = "/b.jpg", poster: String? = null) = LibraryItem(
        id = id, kind = ItemKind.Movie, title = id, year = null, posterPath = poster,
        fileCount = 1, totalSizeBytes = 1, addedAt = addedAt, matched = true, backdropPath = backdrop,
        files = listOf(ItemFile("u-$id", "$id.mkv", 1, null, null, null)),
    )

    @Test
    fun inProgressTitlesComeFirstThenNewest() {
        val items = listOf(item("old", 1), item("mid", 2), item("new", 3))
        val progress = mapOf("u-old" to Progress("u-old", 300_000, 1_000_000, false, 9))
        assertEquals(listOf("old", "new", "mid"), buildHeroEntries(items, progress).map { it.item.id })
    }

    @Test
    fun capsAtSevenAndSkipsTitlesWithoutArtwork() {
        val items = (1..10).map { item("t$it", it.toLong()) } + item("bare", 99, backdrop = null)
        val entries = buildHeroEntries(items, emptyMap())
        assertEquals(7, entries.size)
        assertEquals("t10", entries.first().item.id)
        assertEquals(false, entries.any { it.item.id == "bare" })
    }

    @Test
    fun posterOnlyTitlesAreKept() {
        assertEquals(1, buildHeroEntries(listOf(item("p", 1, backdrop = null, poster = "/p.jpg")), emptyMap()).size)
    }

    @Test
    fun heroPlayTargetResumes() {
        val progress = mapOf("u-a" to Progress("u-a", 300_000, 1_000_000, false, 1))
        assertEquals(300_000, buildHeroEntries(listOf(item("a", 1)), progress).single().target!!.startMs)
    }

    @Test
    fun logoPrefersEnglishThenNeutralThenBestRated() {
        val logos = listOf(
            LogoCandidate("/ja.png", "ja", 9.0),
            LogoCandidate("/none.png", null, 8.0),
            LogoCandidate("/en-low.png", "en", 1.0),
            LogoCandidate("/en-high.png", "en", 5.0),
            LogoCandidate("/en.svg", "en", 10.0),
        )
        assertEquals("/en-high.png", pickLogo(logos))
        assertEquals("/none.png", pickLogo(logos.filter { it.language != "en" }))
        assertEquals("/ja.png", pickLogo(listOf(LogoCandidate("/ja.png", "ja", 1.0))))
        assertNull(pickLogo(emptyList()))
        assertNull(pickLogo(listOf(LogoCandidate("/x.svg", "en", 5.0))))
    }
}
