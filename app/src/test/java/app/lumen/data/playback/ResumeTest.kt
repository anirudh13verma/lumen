package app.lumen.data.playback

import app.lumen.data.model.ItemFile
import app.lumen.data.model.ItemKind
import app.lumen.data.model.LibraryItem
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class ResumeTest {

    private fun file(n: Int) = ItemFile("u$n", "E$n.mkv", 1, 1, n, null)
    private val files = (1..4).map { file(it) }

    private fun p(n: Int, pos: Long, dur: Long = 1_000_000, done: Boolean = false, at: Long = 1) =
        Progress("u$n", pos, dur, done, at)

    @Test
    fun resumableNeedsRealProgressAndNotNearTheEnd() {
        assertFalse(p(1, 5_000).isResumable())
        assertTrue(p(1, 400_000).isResumable())
        assertFalse(p(1, 950_000).isResumable())
        assertFalse(p(1, 400_000, done = true).isResumable())
        assertFalse(p(1, 400_000, dur = 0).isResumable())
    }

    @Test
    fun completionThreshold() {
        assertTrue(ResumeRules.isCompleted(920_000, 1_000_000))
        assertFalse(ResumeRules.isCompleted(500_000, 1_000_000))
        assertFalse(ResumeRules.isCompleted(500_000, 0))
    }

    @Test
    fun nothingWatchedStartsAtTheFirstFile() {
        val target = pickPlayTarget(files, emptyMap())!!
        assertEquals("u1", target.file.uri)
        assertFalse(target.isResume)
    }

    @Test
    fun halfWatchedEpisodeResumes() {
        val target = pickPlayTarget(files, mapOf("u2" to p(2, 300_000, at = 5)))!!
        assertEquals("u2", target.file.uri)
        assertEquals(300_000, target.startMs)
    }

    @Test
    fun mostRecentlyWatchedWins() {
        val progress = mapOf("u1" to p(1, 300_000, at = 1), "u3" to p(3, 200_000, at = 9))
        assertEquals("u3", pickPlayTarget(files, progress)!!.file.uri)
    }

    @Test
    fun finishedEpisodeAdvancesToTheNext() {
        val progress = mapOf("u1" to p(1, 990_000, done = true, at = 1), "u2" to p(2, 990_000, done = true, at = 2))
        assertEquals("u3", pickPlayTarget(files, progress)!!.file.uri)
    }

    @Test
    fun finishingTheLastFileRestartsFromTheTop() {
        val progress = mapOf("u4" to p(4, 990_000, done = true, at = 3))
        assertEquals("u1", pickPlayTarget(files, progress)!!.file.uri)
    }

    @Test
    fun emptyFileListHasNoTarget() {
        assertNull(pickPlayTarget(emptyList(), emptyMap()))
    }

    @Test
    fun startPositionOnlyResumesWhenResumable() {
        assertEquals(300_000, startPositionFor(file(1), mapOf("u1" to p(1, 300_000))))
        assertEquals(0, startPositionFor(file(1), mapOf("u1" to p(1, 3_000))))
        assertEquals(0, startPositionFor(file(1), emptyMap()))
    }

    private fun item(id: String, vararg f: ItemFile) = LibraryItem(
        id = id, kind = ItemKind.Show, title = id, year = null, posterPath = null,
        fileCount = f.size, totalSizeBytes = 1, addedAt = 0, matched = false, files = f.toList(),
    )

    @Test
    fun continueWatchingHasOneCardPerTitleNewestFirst() {
        val show = item("show", file(1), file(2))
        val movie = item("movie", ItemFile("m", "m.mkv", 1, null, null, null))
        val progress = listOf(
            p(1, 300_000, at = 1),
            p(2, 300_000, at = 5),
            Progress("m", 300_000, 1_000_000, false, 3),
            p(3, 300_000, at = 9), // file belongs to no known title
        ).associateBy { it.uri }

        val entries = buildContinueWatching(listOf(show, movie), progress)
        assertEquals(listOf("show", "movie"), entries.map { it.item.id })
        assertEquals("u2", entries[0].file.uri)
    }
}
