package app.lumen.data.scan

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoFilesTest {

    @Test
    fun videoMimeTypeIsEnough() {
        assertTrue(isVideoFile("clip", "video/mp4"))
    }

    @Test
    fun mkvAsGenericBinaryIsRecognisedByExtension() {
        assertTrue(isVideoFile("Dune.Part.Two.2024.mkv", "application/octet-stream"))
        assertTrue(isVideoFile("SHOW.S01E01.MKV", null))
    }

    @Test
    fun nonVideoFilesAreRejected() {
        assertFalse(isVideoFile("cover.jpg", "image/jpeg"))
        assertFalse(isVideoFile("subtitles.srt", "application/x-subrip"))
        assertFalse(isVideoFile("notes", null))
        // .ts is far more often TypeScript than MPEG-TS
        assertFalse(isVideoFile("app.ts", "application/octet-stream"))
    }

    @Test
    fun cleanTitleStripsExtensionAndSeparators() {
        assertEquals("Dune Part Two 2024 1080p WEB-DL", cleanTitle("Dune.Part.Two.2024.1080p.WEB-DL.mkv"))
        assertEquals("Some Movie", cleanTitle("Some_Movie.mp4"))
        assertEquals("Spaced Out", cleanTitle("Spaced   Out.avi"))
    }

    @Test
    fun cleanTitleKeepsNameWithoutKnownExtension() {
        assertEquals("Movie Name 2024", cleanTitle("Movie.Name.2024"))
    }

    @Test
    fun cleanTitleNeverReturnsEmpty() {
        assertEquals(".mkv", cleanTitle(".mkv"))
    }

    @Test
    fun isSameOrInsideMatchesFoldersAndDescendantsOnly() {
        assertTrue(isSameOrInside("primary:Movies", "primary:Movies"))
        assertTrue(isSameOrInside("primary:Movies/Action", "primary:Movies"))
        assertFalse(isSameOrInside("primary:Movies2", "primary:Movies"))
        assertFalse(isSameOrInside("primary:Movies", "primary:Movies/Action"))
        // A whole-volume tree such as "primary:" contains everything on that volume.
        assertTrue(isSameOrInside("primary:Movies", "primary:"))
    }
}
